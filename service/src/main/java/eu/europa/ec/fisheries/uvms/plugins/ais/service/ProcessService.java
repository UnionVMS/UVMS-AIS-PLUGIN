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

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.Future;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ec.fisheries.schema.exchange.movement.asset.v1.AssetId;
import eu.europa.ec.fisheries.schema.exchange.movement.asset.v1.AssetIdList;
import eu.europa.ec.fisheries.schema.exchange.movement.asset.v1.AssetIdType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementBaseType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementComChannelType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementPoint;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementSourceType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.SetReportMovementType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PluginType;
import eu.europa.ec.fisheries.uvms.plugins.ais.StartupBean;

/**
 **/
@LocalBean
@Stateless
public class ProcessService {

    final static Logger LOG = LoggerFactory.getLogger(ProcessService.class);
    boolean detailLog = true;

    @Inject
    ExchangeService exchangeService;

    @Inject
    StartupBean startUp;

    @Inject
    private Conversion conversion;

    @Asynchronous
    public Future<Long> processMessages(List<String> sentences) {
        long start = System.currentTimeMillis();
        for (String sentence : sentences) {
            String binary = symbolToBinary(sentence);
            if (binary != null) {
                try {
                    MovementBaseType movement = binaryToMovement(binary);
                    if (movement != null) {
                        saveData(movement);
                    }
                } catch (NumberFormatException e) {
                    LOG.info("Got badly formed message: {}", binary);
                } catch (Exception e) {
                    LOG.info("//NOP: {}", e.getLocalizedMessage());
                }
            }
        }
        LOG.info("Processing time: {} for {} sentences", (System.currentTimeMillis() - start), sentences.size());
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

    private MovementBaseType binaryToMovement(String sentence) {
        if (sentence == null || sentence.length() < 144) {
            return null;
        }

        try {
            int id = Integer.parseInt(sentence.substring(0, 6), 2);
            // Check that the type of msg is a valid postion msg print for info. Should already be handled.
            LOG.info("Sentence: {}", sentence);
            switch (id) {
                case 0:
                case 1:
                case 2:
                    return parseReport(sentence);
                case 18:
                    // MSG ID 18 Class B Equipment Position report
                    return parseClassBEquipmentPositionReport(sentence);
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            LOG.error("Error when reading binary data. {}", e.getMessage());
        }

        return null;
    }

    private MovementBaseType parseReport(String sentence) {
        MovementBaseType movement = new MovementBaseType();
        String mmsi = String.valueOf(Integer.parseInt(sentence.substring(8, 38), 2));
        movement.setMmsi(mmsi);
        movement.setAssetId(getAssetId(mmsi));
        movement.setReportedSpeed(parseSpeedOverGround(sentence, 50, 60));
        movement.setPosition(getMovementPoint(parseCoordinate(sentence, 61, 89), parseCoordinate(sentence, 89, 116)));
        movement.setReportedCourse(parseCourseOverGround(sentence, 116, 128));
        movement.setPositionTime(getTimestamp(Integer.parseInt(sentence.substring(137, 143), 2)));
        movement.setSource(MovementSourceType.AIS);
        movement.setComChannelType(MovementComChannelType.NAF);

        return movement;
    }


    private MovementBaseType parseClassBEquipmentPositionReport(String sentence) throws NumberFormatException {

        if(sentence == null || sentence.trim().length() < 1) {
            return null;
        }
        int sentenceLength = sentence.length();

        MovementBaseType movement = new MovementBaseType();

        // mmsi
        String mmsi = String.valueOf(Integer.parseInt(sentence.substring(8, 38), 2));
        movement.setMmsi(mmsi);
        movement.setAssetId(getAssetId(mmsi));

       // String notUsed = sentence.substring(38, 46);

        // speedOverGround
        double speedOverGround = parseSpeedOverGround(sentence, 46, 56);
        movement.setReportedSpeed(speedOverGround);

        // positionaccuracy
        Boolean positionAccuracy = parseToBoolean(sentence, 56,57);
        movement.setPositionAccuracy(positionAccuracy);

        // position  longitude latitude
        movement.setPosition(getMovementPoint(parseCoordinate(sentence, 57, 85), parseCoordinate(sentence, 85, 112)));

        // course
        movement.setReportedCourse(parseCourseOverGround(sentence, 112, 124));

        // trueHeading
        String trueHeadingStr = sentence.substring(124,133);
        Integer trueHeading = parseToNumertic("TrueHeading", trueHeadingStr);
        movement.setTrueHeading(trueHeading);

        // timestamp
        movement.setPositionTime(getTimestamp(Integer.parseInt(sentence.substring(133, 139), 2)));

        // not used
        // String notUsed2 = sentence.substring(139, 141);

        // CS Unit
        Boolean csUnit = parseToBoolean(sentence, 141,142);
        movement.setCsUnit(csUnit);

        // Display flag
        Boolean displayFlag = parseToBoolean(sentence, 142,143);
        movement.setDisplayFlag(displayFlag);

        // DSC Flag
        Boolean dscFlag = parseToBoolean(sentence, 143,144);
        movement.setDscFlag(dscFlag);

        // band flag
        Boolean band = parseToBoolean(sentence, 144,145);
        movement.setBandFlag(band);

        // message22 flag
        Boolean message22 = parseToBoolean(sentence, 145,146);
        movement.setMessage22(message22);

        // Assigned
        Boolean assigned =  parseToBoolean(sentence, 146,147);
        movement.setAssigned(assigned);

        // Raim flag
        Boolean raimFlag = parseToBoolean(sentence, 147,148);
        movement.setRaimFlag(raimFlag);

        //
        String radioStatusStr = sentence.substring(148,168);
        Integer radioStatus = parseToNumertic("RadioStatus", radioStatusStr);
        movement.setRadioStatus(radioStatus);

        movement.setSource(MovementSourceType.AIS);
        movement.setComChannelType(MovementComChannelType.NAF);

        return movement;
    }

    private Boolean parseToBoolean(String sentence, int startPosInclusive, int endPosExclusive) throws NumberFormatException
    {
        String str = sentence.substring(startPosInclusive, endPosExclusive);
        if(str == null ) return null;
        return str.equals("1");
    }






    private MovementBaseType parseClassBEquipmentPositionReportOLD(String sentence) throws NumberFormatException {
        MovementBaseType movement = new MovementBaseType();
        String mmsi = String.valueOf(Integer.parseInt(sentence.substring(8, 38), 2));
        movement.setMmsi(mmsi);
        movement.setAssetId(getAssetId(mmsi));
        movement.setReportedSpeed(parseSpeedOverGround(sentence, 46, 56));
        movement.setPosition(getMovementPoint(parseCoordinate(sentence, 57, 85), parseCoordinate(sentence, 85, 112)));
        movement.setReportedCourse(parseCourseOverGround(sentence, 112, 124));
        movement.setPositionTime(getTimestamp(Integer.parseInt(sentence.substring(133, 139), 2)));
        movement.setSource(MovementSourceType.AIS);
        movement.setComChannelType(MovementComChannelType.NAF);

        return movement;
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

    void saveData(MovementBaseType movement) {
        SetReportMovementType movementReport = getMovementReport(movement);
        exchangeService.sendMovementReportToExchange(movementReport);
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

    private MovementPoint getMovementPoint(double longitude, double latitude) {
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

    private Integer parseToNumertic(String fieldName , String str) throws NumberFormatException
    {
        try{
            return Integer.parseInt(str,2);
        }
        catch(NumberFormatException e){
            LOG.error(fieldName + " is not numeric", e);
            throw e;
        }
    }



}