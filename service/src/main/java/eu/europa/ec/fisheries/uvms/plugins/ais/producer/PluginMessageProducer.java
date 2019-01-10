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

import eu.europa.ec.fisheries.uvms.commons.message.impl.JMSUtils;
import eu.europa.ec.fisheries.uvms.exchange.model.constant.ExchangeModelConstants;
import eu.europa.ec.fisheries.uvms.plugins.ais.constants.ModuleQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.*;

@Stateless
@LocalBean
public class PluginMessageProducer {

    private Queue exchangeQueue;
    private Topic eventBus;

    private ConnectionFactory connectionFactory;


    final static Logger LOG = LoggerFactory.getLogger(PluginMessageProducer.class);


    @PostConstruct
    public void resourceLookup() {
        exchangeQueue = JMSUtils.lookupQueue(ExchangeModelConstants.EXCHANGE_MESSAGE_IN_QUEUE);
        eventBus = JMSUtils.lookupTopic(ExchangeModelConstants.PLUGIN_EVENTBUS);
    }

    public void sendResponseMessage(String text, TextMessage requestMessage) throws JMSException {

        try (Connection connection = getConnection();
             Session session = JMSUtils.connectToQueue(connection);
             MessageProducer producer = session.createProducer(requestMessage.getJMSReplyTo())
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

    public void sendModuleMessage(Connection connection, String text, ModuleQueue queue) throws JMSException {

        try (Session session = JMSUtils.connectToQueue(connection);
             MessageProducer producer =  getProducer(session, exchangeQueue)
        ) {
            switch(queue) {

                case EXCHANGE:
                    TextMessage message = session.createTextMessage();
                    message.setText(text);
                    producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                    producer.send(message);
                    break;
                default:
                    LOG.error("[ Sending Queue is not implemented ]");
                    break;
            }
        } catch (JMSException e) {
            LOG.error(e.toString(),e);
            throw e;
        }
    }




    public String sendEventBusMessage(String text, String serviceName) throws JMSException {

        try (Connection connection = getConnection();
             Session session = JMSUtils.connectToQueue(connection);
             MessageProducer producer =  getProducer(session, eventBus)
        ) {
            TextMessage message = session.createTextMessage();
            message.setText(text);
            message.setStringProperty(ExchangeModelConstants.SERVICE_NAME, serviceName);
            producer.send(message);
            return message.getJMSMessageID();
        } catch (JMSException e) {
            LOG.error(e.toString(),e);
            throw e;
        }
    }

    private ConnectionFactory getConnectionFactory() {
        if (this.connectionFactory == null) {
            this.connectionFactory = JMSUtils.lookupConnectionFactory();
        }

        return this.connectionFactory;
    }

    private Connection getConnection() throws JMSException {
        return this.getConnectionFactory().createConnection();
    }

    private MessageProducer getProducer(Session session, Destination destination) throws JMSException {
        MessageProducer producer = session.createProducer(destination);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        return producer;
    }


}