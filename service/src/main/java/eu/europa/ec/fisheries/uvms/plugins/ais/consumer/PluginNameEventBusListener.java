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
package eu.europa.ec.fisheries.uvms.plugins.ais.consumer;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ec.fisheries.schema.exchange.common.v1.AcknowledgeType;
import eu.europa.ec.fisheries.schema.exchange.common.v1.AcknowledgeTypeType;
import eu.europa.ec.fisheries.schema.exchange.plugin.v1.PluginBaseRequest;
import eu.europa.ec.fisheries.schema.exchange.plugin.v1.SetCommandRequest;
import eu.europa.ec.fisheries.schema.exchange.plugin.v1.SetConfigRequest;
import eu.europa.ec.fisheries.schema.exchange.plugin.v1.SetReportRequest;
import eu.europa.ec.fisheries.uvms.exchange.model.constant.ExchangeModelConstants;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.ExchangePluginResponseMapper;
import eu.europa.ec.fisheries.uvms.exchange.model.mapper.JAXBMarshaller;
import eu.europa.ec.fisheries.uvms.plugins.ais.StartupBean;
import eu.europa.ec.fisheries.uvms.plugins.ais.producer.PluginMessageProducer;
import eu.europa.ec.fisheries.uvms.plugins.ais.service.PluginService;

@MessageDriven(mappedName = ExchangeModelConstants.PLUGIN_EVENTBUS, activationConfig = {
    @ActivationConfigProperty(propertyName = "messagingType", propertyValue = ExchangeModelConstants.CONNECTION_TYPE),
    @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = ExchangeModelConstants.DESTINATION_TYPE_TOPIC),
    @ActivationConfigProperty(propertyName = "destination", propertyValue = ExchangeModelConstants.EVENTBUS_NAME)
})
public class PluginNameEventBusListener implements MessageListener {

    private static final Logger LOG = LoggerFactory.getLogger(PluginNameEventBusListener.class);

    @EJB
    PluginService service;

    @EJB
    PluginMessageProducer messageProducer;

    @EJB
    StartupBean startup;

    @Override
    public void onMessage(Message inMessage) {

        LOG.debug("Eventbus listener for ais (MessageConstants.PLUGIN_SERVICE_CLASS_NAME): {}", startup.getRegisterClassName());

        TextMessage textMessage = (TextMessage) inMessage;

        try {

            PluginBaseRequest request = JAXBMarshaller.unmarshallTextMessage(textMessage, PluginBaseRequest.class);

            String responseMessage = null;

            switch (request.getMethod()) {
                case SET_CONFIG:
                    SetConfigRequest setConfigRequest = JAXBMarshaller.unmarshallTextMessage(textMessage, SetConfigRequest.class);
                    AcknowledgeTypeType setConfig = service.setConfig(setConfigRequest.getConfigurations());
                    AcknowledgeType setConfigAck = ExchangePluginResponseMapper.mapToAcknowledgeType(setConfig);
                    responseMessage = ExchangePluginResponseMapper.mapToSetConfigResponse(startup.getRegisterClassName(), setConfigAck);
                    break;
                case SET_COMMAND:
                    SetCommandRequest setCommandRequest = JAXBMarshaller.unmarshallTextMessage(textMessage, SetCommandRequest.class);
                    AcknowledgeTypeType setCommand = service.setCommand(setCommandRequest.getCommand());
                    AcknowledgeType setCommandAck = ExchangePluginResponseMapper.mapToAcknowledgeType(setCommandRequest.getCommand().getLogId(), setCommandRequest.getCommand().getUnsentMessageGuid(), setCommand);
                    responseMessage = ExchangePluginResponseMapper.mapToSetCommandResponse(startup.getRegisterClassName(), setCommandAck);
                    break;
                case SET_REPORT:
                    SetReportRequest setReportRequest = JAXBMarshaller.unmarshallTextMessage(textMessage, SetReportRequest.class);
                    AcknowledgeTypeType setReport = service.setReport(setReportRequest.getReport());
                    AcknowledgeType setReportAck = ExchangePluginResponseMapper.mapToAcknowledgeType(setReportRequest.getReport().getLogId(), setReportRequest.getReport().getUnsentMessageGuid(), setReport);
                    responseMessage = ExchangePluginResponseMapper.mapToSetReportResponse(startup.getRegisterClassName(), setReportAck);
                    break;
                case START:
                    AcknowledgeTypeType start = service.start();
                    AcknowledgeType startAck = ExchangePluginResponseMapper.mapToAcknowledgeType(start);
                    responseMessage = ExchangePluginResponseMapper.mapToStartResponse(startup.getRegisterClassName(), startAck);
                    break;
                case STOP:
                    AcknowledgeTypeType stop = service.stop();
                    AcknowledgeType stopAck = ExchangePluginResponseMapper.mapToAcknowledgeType(stop);
                    responseMessage = ExchangePluginResponseMapper.mapToStopResponse(startup.getRegisterClassName(), stopAck);
                    break;
                case PING:
                    responseMessage = ExchangePluginResponseMapper.mapToPingResponse(startup.isEnabled(), startup.isEnabled());
                    break;
                default:
                    LOG.error("Not supported method");
                    break;
            }

            messageProducer.sendResponseMessage(responseMessage, textMessage);

        } catch (RuntimeException e) {
            LOG.error("[ Error when receiving message in ais " + startup.getRegisterClassName() + " ]", e);
        } catch (JMSException ex) {
            LOG.error("[ Error when handling JMS message in ais " + startup.getRegisterClassName() + " ]", ex);
        }
    }
}