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

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.ResourceException;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementBaseType;
import eu.europa.ec.fisheries.uvms.ais.AISConnection;
import eu.europa.ec.fisheries.uvms.ais.AISConnectionFactoryImpl;
import eu.europa.ec.fisheries.uvms.asset.client.model.AssetDTO;
import eu.europa.ec.fisheries.uvms.plugins.ais.StartupBean;

@Singleton
@Startup
@DependsOn({"StartupBean"})
public class AisService {

    private static final Logger LOG = LoggerFactory.getLogger(AisService.class);

    @Inject
    StartupBean startUp;

    AISConnection connection;

    @EJB
    ProcessService processService;
    
    @Inject
    private ExchangeService exchangeService;

    List<Future<Long>> processes = new ArrayList<>();

    private List<MovementBaseType> failedSendList = new ArrayList<>();
    private Map<String, MovementBaseType> downSampledMovements = new HashMap<>();
    private Map<String, AssetDTO> downSampledAssetInfo = new HashMap<>();
    private Set<String> knownFishingVessels = new HashSet<>();

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
    public void destroy() throws InterruptedException, ExecutionException, TimeoutException {
        if (connection != null) {
            connection.close();
        }
        Iterator<Future<Long>> processIterator = processes.iterator();
        while (processIterator.hasNext()) {
            Future<Long> process = processIterator.next();
            if (process.isDone() || process.isCancelled()) {
                processIterator.remove();
            } else {
                process.get(15, TimeUnit.SECONDS);
                process.cancel(true);
            }
        }
    }

    @Schedule(second = "*/15", minute = "*", hour = "*", persistent = false)
    public void connectAndRetrive() {
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

    @Schedule(minute = "6", hour = "*", persistent = false )
    public void sendAssetUpdates() {
        if (!startUp.isEnabled()) {
            return;
        }
        exchangeService.sendAssetUpdates(downSampledAssetInfo.values());
        downSampledAssetInfo.clear();
    }

//    @Schedule(second = "*/30", minute = "*", hour = "*", persistent = false)
    @Schedule(minute = "*/1", hour = "*", persistent = false )
    public void sendDownSampledMovements() {
        if (downSampledMovements.isEmpty()) {
            return;
        }
        List<MovementBaseType> movements;
        synchronized (downSampledMovements) {
            movements = new ArrayList<>(downSampledMovements.values());
            downSampledMovements.clear();
        }
        List<MovementBaseType> failedMessages = exchangeService.sendToExchange(movements, startUp.getRegisterClassName());
        failedMessages.stream().forEach(this::addCachedMovement);
    }

    @Schedule(minute = "*/15", hour = "*", persistent = false)
    public void resend(Timer timer) {
        try {
            if (startUp.isRegistered()) {
                List<MovementBaseType> list = getAndClearFailedMovementList();
                List<MovementBaseType> failedMessages = exchangeService.sendToExchange(list, startUp.getRegisterClassName());
                failedMessages.stream().forEach(this::addCachedMovement);
            }
        } catch (Exception e) {
            LOG.error(e.toString(), e);
        }
    }

    public void addCachedMovement(MovementBaseType movementBaseType) {
        synchronized (failedSendList) {
            failedSendList.add(movementBaseType);
        }
    }

    public List<MovementBaseType> getAndClearFailedMovementList() {
        List<MovementBaseType> tmp = new ArrayList<>();
        synchronized (failedSendList) {
            tmp.addAll(failedSendList);
            failedSendList.clear();
        }
        return tmp;
    }

    public Map<String, AssetDTO> getStoredAssetInfo(){
        return downSampledAssetInfo;
    }

    public void addToDownSampledMovements(MovementBaseType movement) {
        synchronized (downSampledMovements) {
            downSampledMovements.put(movement.getMmsi(), movement);
        }
    }

    public Set<String> getKnownFishingVessels(){
        return knownFishingVessels;
    }

    @Gauge(unit = MetricUnits.NONE, name = "ais_knownfishingvessels_size", absolute = true)
    public int getKnownFishingVesselsSize() {
        return knownFishingVessels.size();
    }
}