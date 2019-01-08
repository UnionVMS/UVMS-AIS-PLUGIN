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

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void sendResponseMessage(String text, TextMessage requestMessage) throws JMSException {

        Connection connection = null;
        Session session = null;
        javax.jms.MessageProducer producer = null;
        try {
            connection = getConnection();
            session = JMSUtils.connectToQueue(connection);
            TextMessage message = session.createTextMessage();
            message.setJMSDestination(requestMessage.getJMSReplyTo());
            message.setJMSCorrelationID(requestMessage.getJMSMessageID());
            message.setText(text);

            producer = getProducer(session, requestMessage.getJMSReplyTo());
            producer.send(message);

        } catch (JMSException e) {
            LOG.error("[ Error when sending jms message. {}] {}", text, e.getMessage());
            throw new JMSException(e.getMessage());
        } finally {
            if (producer != null) {
                try {
                    producer.close();
                } catch (JMSException je) {
                    // well  . . .
                }
            }
            if (session != null) {
                try {
                    session.close();
                } catch (JMSException je) {
                    // well  . . .
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException je) {
                    // well  . . .
                }
            }
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String sendModuleMessage(String text, ModuleQueue queue) throws JMSException {

        Connection connection = null;
        Session session = null;
        javax.jms.MessageProducer producer = null;

        try {
            connection = getConnection();
            session = JMSUtils.connectToQueue(connection);

            TextMessage message = session.createTextMessage();
            message.setText(text);

            switch (queue) {
                case EXCHANGE:
                    producer = getProducer(session, exchangeQueue);
                    producer.send(message);
                    break;
                default:
                    LOG.error("[ Sending Queue is not implemented ]");
                    break;
            }

            return message.getJMSMessageID();
        } catch (JMSException e) {
            LOG.error("[ Error when sending data source message. {}] {}", text, e.getMessage());
            throw new JMSException(e.getMessage());
        } finally {
            if (producer != null) {
                try {
                    producer.close();
                } catch (JMSException je) {
                    // well  . . .
                }
            }
            if (session != null) {
                try {
                    session.close();
                } catch (JMSException je) {
                    // well  . . .
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException je) {
                    // well  . . .
                }
            }
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public String sendEventBusMessage(String text, String serviceName) throws JMSException {

        Connection connection = null;
        Session session = null;
        javax.jms.MessageProducer producer = null;

        try {
            connection = getConnection();
            session = JMSUtils.connectToQueue(connection);

            TextMessage message = session.createTextMessage();
            message.setText(text);
            message.setStringProperty(ExchangeModelConstants.SERVICE_NAME, serviceName);


            producer = getProducer(session, eventBus);
            producer.send(message);

            return message.getJMSMessageID();
        } catch (JMSException e) {
            LOG.error("[ Error when sending message. {}] {}", text, e.getMessage());
            throw new JMSException(e.getMessage());
        } finally {
            if (producer != null) {
                try {
                    producer.close();
                } catch (JMSException je) {
                    // well  . . .
                }
            }
            if (session != null) {
                try {
                    session.close();
                } catch (JMSException je) {
                    // well  . . .
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException je) {
                    // well  . . .
                }
            }
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