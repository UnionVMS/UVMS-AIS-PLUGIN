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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.Stateless;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementBaseType;
import eu.europa.ec.fisheries.uvms.asset.client.model.AssetDTO;
import eu.europa.ec.fisheries.uvms.plugins.ais.StartupBean;
import eu.europa.ec.fisheries.uvms.plugins.ais.mapper.AisParser;

@Stateless
public class ProcessService {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessService.class);

    @Inject
    private StartupBean startUp;
    
    @Inject
    private ExchangeService exchangeService;

    public ProcessResult processMessages(List<String> sentences, Set<String> knownFishingVessels) {
        long start = System.currentTimeMillis();

        List<MovementBaseType> movements = new ArrayList<>(); 
        Map<String, MovementBaseType> downsampledMovements = new HashMap<>();
        Map<String, AssetDTO> downsampledAssets = new HashMap<>();
        // collect
        for (String sentence : sentences) {
            String binary = symbolToBinary(sentence);
            BinaryToMovementReturn retVal = binaryToMovement(binary, sentence);
            if (retVal != null && retVal.getMessageType().equals(MessageType.MOVEMENT)) {
                MovementBaseType movement = retVal.getMovementBaseType();
                if (movement != null) {
                    if (knownFishingVessels.contains(movement.getMmsi())) {
                        movements.add(movement);
                    } else {
                        downsampledMovements.put(movement.getMmsi(), movement);
                    }
                }
            } else if (retVal != null && retVal.getMessageType().equals(MessageType.ASSET)) {
                AssetDTO assetDto  = retVal.getAssetDTO();
                downsampledAssets.put(assetDto.getMmsi(), assetDto);
                addFishingVessels(assetDto, knownFishingVessels);
            }
        }
        exchangeService.sendToExchange(movements, startUp.getRegisterClassName());
        LOG.info("Processing time: {} for {} sentences", (System.currentTimeMillis() - start), sentences.size());
        return new ProcessResult(downsampledMovements, downsampledAssets);
    }

    private void addFishingVessels(AssetDTO asset, Set<String> knownFishingVessels) {
        if (asset.getVesselType() != null && asset.getVesselType().equals("Fishing")) {
            knownFishingVessels.add(asset.getMmsi());
        } else if (knownFishingVessels.contains(asset.getMmsi()) && asset.getVesselType() != null) {
            LOG.debug("Removing mmsi {} as fishing vessel, is now {}", asset.getMmsi(), asset.getVesselType());
            knownFishingVessels.remove(asset.getMmsi());
        }
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
                        sb.append(Conversion.getBinaryForSymbol(symbolString.charAt(i)));
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
                    return new BinaryToMovementReturn(AisParser.parseReportType123(binary));
                case 5:
                    // MSG ID 5
                    return new BinaryToMovementReturn(AisParser.parseReportType5(binary));
                case 18:
                    // MSG ID 18 Class B Equipment Position report
                    return new BinaryToMovementReturn(AisParser.parseReportType18(binary));
                case 24:
                    // MSG ID 24
                    return new BinaryToMovementReturn(AisParser.parseReportType24(binary));
                default:
                    return null;
            }
        } catch (Exception e) {
            exchangeService.sendToErrorQueueParsingError(sentence);
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    private enum MessageType {
        MOVEMENT, ASSET;
    }

    private class BinaryToMovementReturn {
        private MessageType messageType;
        private MovementBaseType movementBaseType;
        private AssetDTO assetDto;

        public BinaryToMovementReturn(MovementBaseType movementBaseType) {
            this.messageType = MessageType.MOVEMENT;
            this.movementBaseType = movementBaseType;
            this.assetDto = null;
        }

        public BinaryToMovementReturn(AssetDTO assetDto) {
            this.messageType = MessageType.ASSET;
            this.movementBaseType = null;
            this.assetDto = assetDto;
        }

        public MovementBaseType getMovementBaseType() {
            return movementBaseType;
        }

        public AssetDTO getAssetDTO() {
            return assetDto;
        }

        public MessageType getMessageType() {
            return messageType;
        }
    }
}