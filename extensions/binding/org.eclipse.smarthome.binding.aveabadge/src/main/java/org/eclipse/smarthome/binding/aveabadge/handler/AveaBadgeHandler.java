/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.aveabadge.handler;

import static org.eclipse.smarthome.binding.aveabadge.AveaBadgeBindingConstants.*;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.binding.aveabadge.internal.AveaWebMsg;
import org.eclipse.smarthome.binding.aveabadge.servlet.WEB08SServlet;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AveaBadgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Smilzo - Initial contribution
 */
public class AveaBadgeHandler extends BaseThingHandler {

    private final static int TIMEOUT = 5000;

    private final static Logger logger = LoggerFactory.getLogger(AveaBadgeHandler.class);

    private InetAddress myIp;
    private Integer heartBeatInterval = null;
    private int heartBeatMaxLost;
    private int live_check;
    private ScheduledFuture<?> onlinejob;
    private Object lock = new Object();
    private Long code;
    private boolean checked;

    public AveaBadgeHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        try {
            Configuration config = getThing().getConfiguration();
            myIp = InetAddress.getByName((String) config.get(HOST));

            Runnable reset = new Runnable() {
                @Override
                public void run() {
                    resetBadgeState();
                    registerBadge();
                }
            };

            scheduler.schedule(reset, 5, TimeUnit.SECONDS);
        } catch (UnknownHostException e) {
        }
    }

    @Override
    public void dispose() {
        WEB08SServlet.removeReader(myIp);
        if (onlinejob != null)
            onlinejob.cancel(false);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Aveabadge handling command {} on channel {}", command, channelUID);

        if (channelUID.getId().equals(BADGE_ID)) {
            synchronized (lock) {
                if (code != null && command.toString().equals(code.toString())) {
                    code = null;
                    checked = true;
                } else
                    logger.debug("Received command but code {} not match commeand {}", code, command);
                logger.debug("Notify");
                lock.notify();
            }
        }
    }

    public AveaWebMsg messagereceived(AveaWebMsg msg) throws InterruptedException {
        logger.debug("Handling message from {}", msg.getIP().getHostAddress());
        ThingStatus status = this.getThing().getStatus();
        live_check = heartBeatMaxLost;
        if (status.equals(ThingStatus.OFFLINE) || status.equals(ThingStatus.INITIALIZING))
            updateStatus(ThingStatus.ONLINE);

        switch (msg.getCmd()) {
            case PU:
                // Set HB
                if (onlinejob != null)
                    onlinejob.cancel(true);
                try {
                    Configuration config = getThing().getConfiguration();
                    heartBeatInterval = ((BigDecimal) config.get(HEARTBEAT)).intValue();
                    logger.info("HeartBeat interval set to {}", heartBeatInterval);

                    heartBeatMaxLost = ((BigDecimal) config.get(HEARTBEATMAXLOST)).intValue();
                    logger.info("HeartBeat max lost set to {}", heartBeatMaxLost);

                    logger.debug("Aveabadge {} created with ip {}", thing.getUID(), myIp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                msg.setHeartBeat(heartBeatInterval);
                break;
            case CO:
                synchronized (lock) {
                    if (msg.getCode() != null) {
                        checked = false;
                        code = new Long(msg.getCode());
                        updateState(new ChannelUID(getThing().getUID(), BADGE_ID), new DecimalType(msg.getCode()));
                        logger.debug("Waiting");
                        lock.wait(TIMEOUT);
                        logger.debug("End wait");
                        if (checked)
                            msg.setBeep(AveaWebMsg.BEEP.SHORT);
                        else
                            msg.setBeep(AveaWebMsg.BEEP.LONG);
                    } else {
                        msg.setBeep(AveaWebMsg.BEEP.LONG);
                    }

                    resetBadgeState();
                    logger.debug("End processing message");
                }
                break;
        }

        return msg;
    }

    private void registerBadge() {
        // Mi registro
        WEB08SServlet.addReader(myIp, this);
    }

    private void resetBadgeState() {
        logger.debug("resetting state");
        updateState(new ChannelUID(getThing().getUID(), BADGE_ID), new DecimalType(0));
    }

    private void startCheckHeartbeat() {

        if (onlinejob == null || onlinejob.isCancelled()) {
            if (heartBeatInterval > 0) {
                Runnable check = new Runnable() {
                    @Override
                    public void run() {
                        if (live_check > 0) {
                            live_check--;
                            if (live_check == 0) {
                                logger.debug("Too heartbeat lost, status set to offline");
                                updateStatus(ThingStatus.OFFLINE);
                                onlinejob.cancel(false);
                            }
                        }
                    }
                };

                onlinejob = scheduler.scheduleAtFixedRate(check, 0, heartBeatInterval, TimeUnit.SECONDS);
                logger.debug("Lifecheck launched!!!");

            }
        }
    }
}
