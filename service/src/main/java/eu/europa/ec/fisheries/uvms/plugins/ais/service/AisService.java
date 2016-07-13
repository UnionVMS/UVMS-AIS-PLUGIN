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
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.*;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.ResourceException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import eu.europa.ec.fisheries.schema.exchange.movement.asset.v1.AssetId;
import eu.europa.ec.fisheries.schema.exchange.movement.asset.v1.AssetIdList;
import eu.europa.ec.fisheries.schema.exchange.movement.asset.v1.AssetIdType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.*;
import eu.europa.ec.fisheries.uvms.ais.AISConnection;
import eu.europa.ec.fisheries.uvms.ais.AISConnectionFactoryImpl;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PluginType;
import eu.europa.ec.fisheries.uvms.plugins.ais.StartupBean;
import eu.europa.ec.fisheries.uvms.plugins.ais.constants.AisField;

@Singleton
@Startup
@DependsOn({"StartupBean"})
public class AisService {

	private static final Logger LOG = LoggerFactory.getLogger(AisService.class);

    private static final int RETRY_DELAY_TIME_SEC = 10;

    @Inject
    StartupBean startUp;

    AISConnection connection;

    @EJB
    ProcessService processService;

    List<Future<Long>> processes = new ArrayList<>();

    @PostConstruct
    public void init() {
        try {
            Context ctx = new InitialContext();
            AISConnectionFactoryImpl factory = (AISConnectionFactoryImpl) ctx.lookup("java:/eis/AISConnectionFactory");
            if (factory != null) {
                LOG.debug("Factory lookup done! {}, {}", factory.toString(), factory.getClass());
                connection = factory.getConnection();

                if (startUp.isEnabled() && connection != null && !connection.isOpen()) {
                    String host = startUp.getSetting("HOST");
                    int port = Integer.parseInt(startUp.getSetting("PORT"));
                    String username = startUp.getSetting("USERNAME");
                    String password = startUp.getSetting("PASSWORD");

                    connection.open(host, port, username, password);
                }
            }
        } catch (NamingException | ResourceException e) {
            LOG.error("Exception: {}", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (connection != null) {
            connection.close();
        }
        Iterator<Future<Long>> processIterator = processes.iterator();
        while (processIterator.hasNext()) {
            Future<Long> process = processIterator.next();
            if (process.isDone() || process.isCancelled()) {
                processIterator.remove();
            } else {
                process.cancel(true);
            }
        }
    }

    @Schedule(minute="*/1", hour="*", persistent=false)
    public void connectAndRetrive(){
        if (!startUp.isEnabled()) {
            return;
        }
        if (connection != null && !connection.isOpen()) {
            String host = startUp.getSetting("HOST");
            int port = Integer.parseInt(startUp.getSetting("PORT"));
            String username = startUp.getSetting("USERNAME");
            String password = startUp.getSetting("PASSWORD");

            connection.open(host, port, username, password);
        }

        if (connection != null && connection.isOpen()) {
            Iterator<Future<Long>> processIterator = processes.iterator();
            while (processIterator.hasNext()) {
                Future<Long> process = processIterator.next();
                if (process.isDone() || process.isCancelled()) {
                    processIterator.remove();
                }
            }
            List<String> sentences = connection.getSentences();
            Future<Long> process = processService.processMessages(sentences);
            processes.add(process);
            LOG.info("Got {} sentences from AIS RA. Currently running {} parallel threads", sentences.size(), processes.size());
        }
    }

}