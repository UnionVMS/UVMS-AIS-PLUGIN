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
package eu.europa.ec.fisheries.uvms.plugins.ais.producer;

import eu.europa.ec.fisheries.uvms.exchange.model.constant.ExchangeModelConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.jms.*;

@Stateless
@LocalBean
public class PluginMessageProducer {

    @Resource(mappedName = "java:/" + ExchangeModelConstants.EXCHANGE_MESSAGE_IN_QUEUE)
    private Queue exchangeQueue;

    @Resource(mappedName = "java:/" + ExchangeModelConstants.PLUGIN_EVENTBUS)
    private Topic eventBus;

    @Resource(mappedName = "java:/ConnectionFactory")
    private ConnectionFactory connectionFactory;


    private static final Logger LOG = LoggerFactory.getLogger(PluginMessageProducer.class);

    public void sendResponseMessage(String text, TextMessage requestMessage) throws JMSException {

        try (Connection connection = connectionFactory.createConnection();
             Session session = connection.createSession(false, 1);
             MessageProducer producer = session.createProducer(requestMessage.getJMSReplyTo());
        ) {

            TextMessage message = session.createTextMessage();
            message.setJMSDestination(requestMessage.getJMSReplyTo());
            message.setJMSCorrelationID(requestMessage.getJMSMessageID());
            message.setText(text);

            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(message);
        } catch (JMSException e) {
            LOG.error(e.toString(),e);
            throw e;
        }
    }

    public String sendEventBusMessage(String text, String serviceName, String function) throws JMSException {

        try (Connection connection = connectionFactory.createConnection();
             Session session = connection.createSession(false, 1);
             MessageProducer producer = session.createProducer(eventBus);
        ) {
            TextMessage message = session.createTextMessage();
            message.setText(text);
            message.setStringProperty(ExchangeModelConstants.SERVICE_NAME, serviceName);
            message.setStringProperty("FUNCTION", function);
            producer.send(message);
            return message.getJMSMessageID();
        } catch (JMSException e) {
            LOG.error(e.toString(),e);
            throw e;
        }
    }

}