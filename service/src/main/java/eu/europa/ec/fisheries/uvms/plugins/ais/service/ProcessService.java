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

import eu.europa.ec.fisheries.schema.exchange.module.v1.ExchangeModuleMethod;
import eu.europa.ec.fisheries.schema.exchange.module.v1.ReceiveAssetInformationRequest;
import eu.europa.ec.fisheries.schema.exchange.movement.asset.v1.AssetId;
import eu.europa.ec.fisheries.schema.exchange.movement.asset.v1.AssetIdList;
import eu.europa.ec.fisheries.schema.exchange.movement.asset.v1.AssetIdType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementBaseType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementPoint;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementSourceType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.SetReportMovementType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PluginType;
import eu.europa.ec.fisheries.uvms.asset.client.model.AssetDTO;
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
import javax.jms.Queue;
import javax.jms.*;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.util.*;
import java.util.concurrent.Future;

/**
 *
 **/
@LocalBean
@Stateless
public class ProcessService {

    private Jsonb jsonb = JsonbBuilder.create();


    class BinaryToMovementReturn {
        private int messageType;
        private MovementBaseType movementBaseType;
        private AssetDTO assetDto;

        public BinaryToMovementReturn(int messageType, MovementBaseType movementBaseType) {
            this.messageType = messageType;
            this.movementBaseType = movementBaseType;
            this.assetDto = null;
        }

        public BinaryToMovementReturn(int messageType, AssetDTO assetDto) {
            this.messageType = messageType;
            this.movementBaseType = null;
            this.assetDto = assetDto;
        }

        public MovementBaseType getMovementBaseType() {
            return movementBaseType;
        }

        public AssetDTO getAssetDTO() {
            return assetDto;
        }

        public int getMessageType() {
            return messageType;
        }
    }

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
        List<AssetDTO> assetInfo = new ArrayList<>();

        // collect
        for (String sentence : sentences) {
            String binary = symbolToBinary(sentence);
            if (binary != null) {
                BinaryToMovementReturn retVal = binaryToMovement(binary, sentence);
                if (retVal != null) {
                    int messageType = retVal.getMessageType();
                    switch (messageType) {
                        case 1:
                        case 2:
                        case 3:
                        case 18: {
                            MovementBaseType movement = retVal.getMovementBaseType();
                            if (movement != null) {
                                downSamplingControl.put(movement.getMmsi(), movement);
                            }
                            break;
                        }
                        case 5:
                        case 24: {
                            AssetDTO assetDto  = retVal.getAssetDTO();
                            assetInfo.add(assetDto);
                            break;
                        }
                    }
                }
            }
        }
        sendAssetUpdateToExchange(assetInfo);
        return sendToExchange(downSamplingControl);
    }

    public boolean sendAssetUpdateToExchange(List<AssetDTO> assets) {

        boolean ok = true;
        String json = jsonb.toJson(assets);
        LOG.trace(json);
        try (Connection connection = connectionFactory.createConnection();
             Session session = connection.createSession(false, 1);
             MessageProducer producer = session.createProducer(exchangeQueue)
        ) {
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            // emit
            try {
                String text = ExchangeModuleRequestMapper.createReceiveAssetInformation(json, "AIS", PluginType.OTHER);
                TextMessage message = session.createTextMessage();
                message.setText(text);
                producer.send(message);
            } catch (ExchangeModelMarshallException e) {
                LOG.error("Couldn't map movement to setreportmovementtype");
                sendToErrorQueueParsingError(json);
            } catch (Exception e) {
                LOG.info("//NOP: {}", e.getLocalizedMessage());
            }
        } catch (JMSException e) {
            LOG.error("couldn't send movement");
            ok = false;
        }
        return ok;
    }


    public Future<Long> sendToExchange(Map<String, MovementBaseType> movements) {

        long start = System.currentTimeMillis();

        try (Connection connection = connectionFactory.createConnection();
             Session session = connection.createSession(false, 1);
             MessageProducer producer = session.createProducer(exchangeQueue)
        ) {
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            // emit

            for (MovementBaseType movement : movements.values()) {

                try {
                    SetReportMovementType movementReport = getMovementReport(movement);
                    String text = ExchangeModuleRequestMapper.createSetMovementReportRequest(movementReport, "AIS", null, new Date(), null, PluginType.OTHER, "AIS", null);
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
                case '1': // message id 1
                case '2': // message id 2
                case '3': // message id 3
                case '5': // message id 5
                case 'B': // message id 18
                case 'H': // message id 24
                    for (int i = 0; i < symbolString.length(); i++) {
                        sb.append(conversion.getBinaryForSymbol(symbolString.charAt(i)));
                    }
                    return sb.toString();
                default:
            }
        } catch (Exception e) {
            LOG.info("//NOP: {}", e.getLocalizedMessage());
        }
        return null;
    }

    private BinaryToMovementReturn binaryToMovement(String binary, String sentence) {
        if (binary == null || binary.length() < 144) {
            return null;
        }

        try {
            int messageType = Integer.parseInt(binary.substring(0, 6), 2);
            // Check that the type of msg is a valid postion msg print for info. Should already be handled.
            switch (messageType) {
                case 1:
                case 2:
                case 3:
                    return new BinaryToMovementReturn(messageType, parseReportType_1_2_3(messageType, binary, sentence));
                case 5:
                    // MSG ID 5
                    return new BinaryToMovementReturn(messageType, parseReportType_5(messageType, binary, sentence));
                case 18:
                    // MSG ID 18 Class B Equipment Position report
                    return new BinaryToMovementReturn(messageType, parseReportType_18(messageType, binary, sentence));
                case 24:
                    // MSG ID 24
                    return new BinaryToMovementReturn(messageType, parseReportType_24(messageType, binary));
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            sendToErrorQueueParsingError(sentence);
            LOG.error(e.getMessage(), e);

        }

        return null;
    }

    private MovementBaseType parseReportType_1_2_3(Integer messageType, String binary, String sentence) {
        MovementBaseType movement = new MovementBaseType();
        //movement.setAisMessageType(messageType);
        String mmsi = String.valueOf(Integer.parseInt(binary.substring(8, 38), 2));
        movement.setMmsi(mmsi);
        movement.setAssetId(getAssetId(mmsi));
        movement.setReportedSpeed(parseSpeedOverGround(binary, 50, 60));
        MovementPoint point = getMovementPoint(parseCoordinate(binary, 61, 89), parseCoordinate(binary, 89, 116), sentence, 123);
        if (point == null) {
            LOG.warn("Error in position longitude or latitude in type {}  {} Lat: {}  Long: {}", messageType, sentence, binary.substring(61, 89), binary.substring(89, 116));
            return null;
        }
        movement.setPosition(point);
        movement.setReportedCourse(parseCourseOverGround(binary, 116, 128));

        // trueHeading
        String trueHeadingStr = binary.substring(128, 137);
        Integer trueHeading = parseToNumeric("TrueHeading", trueHeadingStr);
        movement.setTrueHeading(trueHeading);
        movement.setPositionTime(getTimestamp(Integer.parseInt(binary.substring(137, 143), 2)));
        movement.setSource(MovementSourceType.AIS);
        return movement;
    }

    // first draft for type 5
    private AssetDTO parseReportType_5(Integer messageType, String binary, String sentence) {


        ReceiveAssetInformationRequest req = new ReceiveAssetInformationRequest();
        req.setMethod(ExchangeModuleMethod.RECEIVE_ASSET_INFORMATION);

        String mmsi = String.valueOf(Integer.parseInt(binary.substring(8, 38), 2));
        String vesselName = conversion.getAsciiStringFromBinaryString(binary.substring(112, 232));
        String ircs = conversion.getAsciiStringFromBinaryString(binary.substring(70, 112));
        Integer shipType = Integer.parseInt(binary.substring(232, 240), 2);

        String cc = mmsi.substring(0, 3);
        String ansi3 = conversion.getAnsi3ForCountryCode(cc);

        AssetDTO assetDTO = new AssetDTO();
        assetDTO.setMmsi(mmsi);

        assetDTO.setName(vesselName);
        assetDTO.setIrcs(ircs);
        assetDTO.setVesselType(conversion.getShiptypeForCode(shipType));
        assetDTO.setFlagStateCode(ansi3);
        return assetDTO;

    }


    private MovementBaseType parseReportType_18(Integer messageType, String binary, String sentence) throws NumberFormatException {

        if (binary == null || binary.trim().length() < 1) {
            return null;
        }
        MovementBaseType movement = new MovementBaseType();

        // mmsi
        String mmsi = String.valueOf(Integer.parseInt(binary.substring(8, 38), 2));
        movement.setMmsi(mmsi);
        movement.setAssetId(getAssetId(mmsi));

        // speedOverGround
        double speedOverGround = parseSpeedOverGround(binary, 46, 56);
        movement.setReportedSpeed(speedOverGround);

        // position  longitude latitude
        MovementPoint point = getMovementPoint(parseCoordinate(binary, 57, 85), parseCoordinate(binary, 85, 112), sentence, 18);
        if (point == null) {
            LOG.warn("Error in position longitude or latitude in type {}  {} Lat: {}  Long: {}", messageType, sentence, binary.substring(57, 85), binary.substring(85, 112));
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
        movement.setSource(MovementSourceType.AIS);
        return movement;
    }

    private AssetDTO parseReportType_24(Integer messageType, String binary) throws NumberFormatException {

        if (binary == null || binary.trim().length() < 1) {
            return null;
        }
        ReceiveAssetInformationRequest req = new ReceiveAssetInformationRequest();
        req.setMethod(ExchangeModuleMethod.RECEIVE_ASSET_INFORMATION);

        //movement.setAisMessageType(messageType);
        String mmsi = String.valueOf(Integer.parseInt(binary.substring(8, 38), 2));
        String vesselName = "";
        Integer shipType = null;
        String ircs = "";
        String ansi3 = "";

        // if partNumber == 0   the rest of the message is interpreted as a Part A
        // if partNumber == 1   the rest of the message is interpreted as a Part B
        // values of 2 and 3 is not allowed
        Integer partNumber = parseToNumeric("Part Number", binary, 38, 40);
        //movement.setPartNumber(partNumber);
        if (partNumber.equals(0)) {
            vesselName = conversion.getAsciiStringFromBinaryString(binary.substring(40, 160));
        } else if (partNumber.equals(1)) {
            shipType = Integer.parseInt(binary.substring(40,48), 2);
            ircs = conversion.getAsciiStringFromBinaryString(binary.substring(90, 132));
            String cc = mmsi.substring(0, 3);
            ansi3 = conversion.getAnsi3ForCountryCode(cc);
        }

        AssetDTO assetDTO = new AssetDTO();
        assetDTO.setMmsi(mmsi);
        assetDTO.setName(vesselName);
        assetDTO.setIrcs(ircs);
        if(shipType != null) {
            assetDTO.setVesselType(conversion.getShiptypeForCode(shipType));
        }
        assetDTO.setFlagStateCode(ansi3);
        return assetDTO;

    }


    Date asDate(String monthStr, String dayStr, String hourStr, String minuteStr) {

        int month = toInt(monthStr, 0);
        int day = toInt(dayStr, 0);
        int hour = toInt(hourStr, 24);
        int minute = toInt(minuteStr, 60);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR, hour);
        cal.set(Calendar.MINUTE, minute);
        return cal.getTime();
    }


    int toInt(String value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
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

        if (longitude.equals(181d) || latitude.equals(91d)) {
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
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.MILLISECOND, 0);
        if (utcSeconds != null && utcSeconds >= 0 && utcSeconds < 60) {
            cal.set(Calendar.SECOND, utcSeconds);
            if (utcSeconds >= cal.get(Calendar.SECOND)) {
                cal.add(Calendar.MINUTE, -1);
            }
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
                BytesMessage message_bytes = session.createBytesMessage();
                message_bytes.setStringProperty("source", "AIS");
                message_bytes.setStringProperty("type", "byte");
                message_bytes.writeBytes(movement.getBytes());
                producer.send(message_bytes);
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