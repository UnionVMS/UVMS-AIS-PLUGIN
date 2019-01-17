/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.europa.ec.fisheries.uvms.plugins.ais.service;

import eu.europa.ec.fisheries.schema.exchange.movement.asset.v1.AssetId;
import eu.europa.ec.fisheries.schema.exchange.movement.asset.v1.AssetIdList;
import eu.europa.ec.fisheries.schema.exchange.movement.asset.v1.AssetIdType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.*;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PluginType;
import eu.europa.ec.fisheries.uvms.exchange.model.exception.ExchangeModelMarshallException;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangeModuleRequestMapper;
import eu.europa.ec.fisheries.uvms.plugins.ais.StartupBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.*;
import javax.jms.Queue;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.Future;

/**
 **/
@LocalBean
@Stateless
public class ProcessService {

    final static Logger LOG = LoggerFactory.getLogger(ProcessService.class);

    @Resource(mappedName = "java:/ConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Resource(mappedName = "java:/jms/queue/UVMSExchangeEvent")
    private Queue exchangeQueue;


    @Resource(mappedName = "java:/jms/queue/UVMSPluginFailedReport")
    private Queue errorQueue;



    @Inject
    StartupBean startUp;

    @Inject
    private Conversion conversion;


    @Asynchronous
    public Future<Long> processMessages(List<String> sentences) {

        Map<String, MovementBaseType> downSamplingControl = new HashMap<>();

        // collect
        for (String sentence : sentences) {
            String binary = symbolToBinary(sentence);
            if (binary != null) {
                try {
                    MovementBaseType movement = binaryToMovement(binary, sentence);
                    if (movement != null) {
                        downSamplingControl.put(movement.getMmsi(), movement);
                    }
                    else{
                        sendToErrorQueueParsingError(sentence);
                    }
                } catch (Exception e) {
                    LOG.warn(e.toString(), e);
                }
            }
        }
        return sendToExchange(downSamplingControl);
    }


    public Future<Long> sendToExchange(Map<String, MovementBaseType> movements){

        long start = System.currentTimeMillis();

        try (Connection connection = connectionFactory.createConnection();
             Session session = connection.createSession(false, 1);
             MessageProducer producer = session.createProducer(exchangeQueue)
        ) {
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            // emit

            for (MovementBaseType movement : movements.values()) {

                String text = null;
                try {
                    SetReportMovementType movementReport = getMovementReport(movement);
                    text = ExchangeModuleRequestMapper.createSetMovementReportRequest(movementReport, "AIS", null, new Date(), null, PluginType.OTHER, "AIS", null);
                    TextMessage message = session.createTextMessage();
                    message.setText(text);
                    producer.send(message);
                } catch (ExchangeModelMarshallException e) {
                    LOG.error("Couldn't map movement to setreportmovementtype");
                    sendToErrorQueueParsingError(movement.toString());
                } catch (JMSException e) {
                    // save it and try again in a scheduled thread
                    startUp.addCachedMovement(movement);
                } catch (Exception e) {
                    LOG.info("//NOP: {}", e.getLocalizedMessage());
                }
            }
        } catch (JMSException e) {
            LOG.error("couldn't send movement");
        }
        return new AsyncResult<Long>(new Long(System.currentTimeMillis() - start));
    }



    private String symbolToBinary(String symbolString) {
        try {
            StringBuilder sb = new StringBuilder();

            switch (symbolString.charAt(0)) {
                case '0': // message id 0
                case '1': // message id 1
                case '2': // message id 2
                case 'B': // message id 18
                    // case 'H': // message id 24
                    for (int i = 0; i < symbolString.length(); i++) {
                        sb.append(conversion.getBinaryForSymbol(symbolString.charAt(i)));
                    }
                    return sb.toString();
            }
        } catch (Exception e) {
            LOG.info("//NOP: {}", e.getLocalizedMessage());
        }
        return null;
    }

    private MovementBaseType binaryToMovement(String binary, String sentence) {
        if (binary == null || binary.length() < 144) {
            return null;
        }

        try {
            int messageType = Integer.parseInt(binary.substring(0, 6), 2);
            // Check that the type of msg is a valid postion msg print for info. Should already be handled.
            LOG.debug("Sentence: {}", binary);
            switch (messageType) {
                case 0:
                case 1:
                case 2:
                    return parseReportType_1_2_3(messageType, binary, sentence);
                case 18:
                    // MSG ID 18 Class B Equipment Position report
                    return parseReportType_18(messageType, binary, sentence);
                //case 24:
                // MSG ID 24
                //    return parseReportType_24(messageType, binary);
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            sendToErrorQueueParsingError(sentence);
            LOG.error(e.getMessage(), e);

        }

        return null;
    }

    private MovementBaseType parseReportType_24(Integer messageType, String binary) throws NumberFormatException {

        if (binary == null || binary.trim().length() < 1) {
            return null;
        }
        MovementBaseType movement = new MovementBaseType();
        movement.setAisMessageType(messageType);

        String mmsi = String.valueOf(Integer.parseInt(binary.substring(8, 38), 2));
        movement.setMmsi(mmsi);
        movement.setAssetId(getAssetId(mmsi));

        // if partNumber == 0   the rest of the message is interpreted as a Part A
        // if partNumber == 1   the rest of the message is interpreted as a Part B
        // values of 2 and 3 is not allowed
        Integer partNumber = parseToNumeric("Part Number", binary, 38, 40);
        movement.setPartNumber(partNumber);
        if (partNumber.equals(0)) {
            movement.setAssetName(binary.substring(40, 160));
        } else if (partNumber.equals(1)) {
            movement.setShipType(binary.substring(40, 48));
            movement.setVendorId(binary.substring(48, 66));
            movement.setUnitModelCode(Integer.parseInt(binary.substring(66, 70), 2));
            movement.setSerialNumber(binary.substring(70, 90));
            movement.setCallSign(binary.substring(90, 132));
            movement.setDimensionToBow(Integer.parseInt(binary.substring(132, 141), 2));
            movement.setDimensionToStern(Integer.parseInt(binary.substring(141, 150), 2));
            movement.setDimensionToPort(Integer.parseInt(binary.substring(150, 156), 2));
            movement.setDimensionToStarBoard(Integer.parseInt(binary.substring(156, 162), 2));
            movement.setMotherShipMMSI(String.valueOf(Integer.parseInt(binary.substring(132, 162), 2)));
        }
        return movement;
    }


    private MovementBaseType parseReportType_1_2_3(Integer messageType, String binary, String sentence) {
        MovementBaseType movement = new MovementBaseType();
        movement.setAisMessageType(messageType);
        String mmsi = String.valueOf(Integer.parseInt(binary.substring(8, 38), 2));
        movement.setMmsi(mmsi);
        movement.setAssetId(getAssetId(mmsi));

        // NavigationStatus
        String navigationStatus = binary.substring(38, 42);
        movement.setNavigationStatus(navigationStatus);

        // rateOfTurn
        String rateOfTurn = binary.substring(42, 50);
        movement.setRateOfTurn(rateOfTurn);

        movement.setReportedSpeed(parseSpeedOverGround(binary, 50, 60));

        // positionaccuracy
        Boolean positionAccuracy = parseToBoolean(binary, 60, 61);
        movement.setPositionAccuracy(positionAccuracy);

        MovementPoint point = getMovementPoint(parseCoordinate(binary, 61, 89), parseCoordinate(binary, 89, 116), sentence, 123);
        if (point == null) {
            LOG.error("Error in position longitude or latitude in type 1 or 2 or 3");
            return null;
        }
        movement.setPosition(point);
        movement.setReportedCourse(parseCourseOverGround(binary, 116, 128));

        // trueHeading
        String trueHeadingStr = binary.substring(128, 137);
        Integer trueHeading = parseToNumeric("TrueHeading", trueHeadingStr);
        movement.setTrueHeading(trueHeading);

        movement.setPositionTime(getTimestamp(Integer.parseInt(binary.substring(137, 143), 2)));

        // maneuverIndicator
        String maneuverIndicator = binary.substring(143, 145);
        movement.setManeuverIndicator(maneuverIndicator);

        // Raim flag
        Boolean raimFlag = parseToBoolean(binary, 148, 149);
        movement.setRaimFlag(raimFlag);

        //
        String radioStatusStr = binary.substring(149, 168);
        Integer radioStatus = parseToNumeric("RadioStatus", radioStatusStr);
        movement.setRadioStatus(radioStatus);

        movement.setSource(MovementSourceType.AIS);
        movement.setComChannelType(MovementComChannelType.NAF);

        return movement;
    }


    private MovementBaseType parseReportType_18(Integer messageType, String binary, String sentence) throws NumberFormatException {

        if (binary == null || binary.trim().length() < 1) {
            return null;
        }
        MovementBaseType movement = new MovementBaseType();
        movement.setAisMessageType(messageType);

        // mmsi
        String mmsi = String.valueOf(Integer.parseInt(binary.substring(8, 38), 2));
        movement.setMmsi(mmsi);
        movement.setAssetId(getAssetId(mmsi));

        // speedOverGround
        double speedOverGround = parseSpeedOverGround(binary, 46, 56);
        movement.setReportedSpeed(speedOverGround);

        // positionaccuracy
        Boolean positionAccuracy = parseToBoolean(binary, 56, 57);
        movement.setPositionAccuracy(positionAccuracy);

        // position  longitude latitude
        MovementPoint point = getMovementPoint(parseCoordinate(binary, 57, 85), parseCoordinate(binary, 85, 112), sentence, 18);
        if (point == null) {
            LOG.error("Error in position longitude or latitude in type 18");
            return null;
        }
        movement.setPosition(point);

        // course
        movement.setReportedCourse(parseCourseOverGround(binary, 112, 124));

        // trueHeading
        String trueHeadingStr = binary.substring(124, 133);
        Integer trueHeading = parseToNumeric("TrueHeading", trueHeadingStr);
        movement.setTrueHeading(trueHeading);

        // timestamp
        movement.setPositionTime(getTimestamp(Integer.parseInt(binary.substring(133, 139), 2)));

        // CS Unit
        Boolean csUnit = parseToBoolean(binary, 141, 142);
        movement.setCsUnit(csUnit);

        // Display flag
        Boolean displayFlag = parseToBoolean(binary, 142, 143);
        movement.setDisplayFlag(displayFlag);

        // DSC Flag
        Boolean dscFlag = parseToBoolean(binary, 143, 144);
        movement.setDscFlag(dscFlag);

        // band flag
        Boolean band = parseToBoolean(binary, 144, 145);
        movement.setBandFlag(band);

        // message22 flag
        Boolean message22 = parseToBoolean(binary, 145, 146);
        movement.setMessage22(message22);

        // Assigned
        Boolean assigned = parseToBoolean(binary, 146, 147);
        movement.setAssigned(assigned);

        // Raim flag
        Boolean raimFlag = parseToBoolean(binary, 147, 148);
        movement.setRaimFlag(raimFlag);

        //
        String radioStatusStr = binary.substring(148, 168);
        Integer radioStatus = parseToNumeric("RadioStatus", radioStatusStr);
        movement.setRadioStatus(radioStatus);

        movement.setSource(MovementSourceType.AIS);
        movement.setComChannelType(MovementComChannelType.NAF);

        return movement;
    }

    private Boolean parseToBoolean(String sentence, int startPosInclusive, int endPosExclusive) throws NumberFormatException {
        String str = sentence.substring(startPosInclusive, endPosExclusive);
        if (str == null) return null;
        return str.equals("1");
    }

    private Double parseCoordinate(String data, int stringStart, int stringEnd) throws NumberFormatException {
        Integer i = Integer.parseInt(data.substring(stringStart, stringEnd), 2);
        return (i.doubleValue() / 10000 / 60);
    }

    private double parseCourseOverGround(String s, int stringStart, int stringEnd) throws NumberFormatException {
        Integer i = Integer.parseInt(s.substring(stringStart, stringEnd), 2);
        return i.doubleValue() / 10;
    }

    private double parseSpeedOverGround(String s, int stringStart, int stringEnd) throws NumberFormatException {
        Integer speedOverGround = Integer.parseInt(s.substring(stringStart, stringEnd), 2);
        return speedOverGround.doubleValue() / 10;
    }

    private AssetId getAssetId(String mmsi) {
        AssetId assetId = new AssetId();
        AssetIdList assetIdList = new AssetIdList();
        assetIdList.setIdType(AssetIdType.MMSI);
        assetIdList.setValue(mmsi);
        assetId.getAssetIdList().add(assetIdList);
        return assetId;
    }

    SetReportMovementType getMovementReport(MovementBaseType movement) {
        SetReportMovementType report = new SetReportMovementType();
        report.setTimestamp(getTimestamp());
        report.setPluginName(startUp.getRegisterClassName());
        report.setPluginType(PluginType.OTHER);
        report.setMovement(movement);
        return report;
    }

    private MovementPoint getMovementPoint(Double longitude, Double latitude, String sentence, int messageType) {

        if (longitude.equals(91) || longitude.equals(181) || latitude.equals(91)|| latitude.equals(181)) {
            return null;
        }

        MovementPoint point = new MovementPoint();
        point.setLongitude(longitude);
        point.setLatitude(latitude);
        return point;
    }

    Date getTimestamp() {
        return getTimestamp(null);
    }

    private Date getTimestamp(Integer utcSeconds) {
        GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        if (utcSeconds != null) {
            cal.set(GregorianCalendar.SECOND, utcSeconds);
        }

        return cal.getTime();
    }

    private Integer parseToNumeric(String fieldName, String str) throws NumberFormatException {
        try {
            return Integer.parseInt(str, 2);
        } catch (NumberFormatException e) {
            LOG.error(fieldName + " is not numeric", e);
            throw e;
        }
    }

    private Integer parseToNumeric(String fieldName, String sentence, int startPosInclusive, int endPosExclusive) throws NumberFormatException, IndexOutOfBoundsException {

        try {
            String str = sentence.substring(startPosInclusive, endPosExclusive);
            return Integer.parseInt(str, 2);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            LOG.error(fieldName + " parsing error", e);
            throw e;
        }
    }



    private void sendToErrorQueueParsingError(String movement) {
        try (
                Connection connection = connectionFactory.createConnection();
                Session session = connection.createSession(false, 1);
                MessageProducer producer = session.createProducer(errorQueue)
        ) {
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            // emit

            try {
                TextMessage message = session.createTextMessage();
                message.setStringProperty("source", "AIS");
                message.setText(movement);
                producer.send(message);
            } catch (Exception e) {
                LOG.info("//NOP: {}", e.getLocalizedMessage());
            }
        } catch (JMSException e) {
            LOG.error("couldn't send movement");
        }
    }

    private void sendToErrorQueue(String movement) {
        try (
                Connection connection = connectionFactory.createConnection();
                Session session = connection.createSession(false, 1);
                MessageProducer producer = session.createProducer(errorQueue)
        ) {
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            // emit

            try {
                TextMessage message = session.createTextMessage();
                message.setStringProperty("source", "AIS");
                message.setText(movement);
                producer.send(message);
            } catch (Exception e) {
                LOG.info("//NOP: {}", e.getLocalizedMessage());
            }
        } catch (JMSException e) {
            LOG.error("couldn't send movement");
        }
    }



}