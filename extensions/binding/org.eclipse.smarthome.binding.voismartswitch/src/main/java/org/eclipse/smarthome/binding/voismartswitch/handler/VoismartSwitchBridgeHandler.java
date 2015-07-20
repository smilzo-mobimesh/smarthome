/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.voismartswitch.handler;

import static org.eclipse.smarthome.binding.voismartswitch.VoismartSwitchBindingConstants.*;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link VoismartSwitchBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Smilzo - Initial contribution
 */
public class VoismartSwitchBridgeHandler extends BaseThingHandler {

    private final static Logger logger = LoggerFactory.getLogger(VoismartSwitchBridgeHandler.class);

    private static final int POLLING_FREQUENCY = 60; // in seconds

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_VOISMARTSWITCH);

    private Map<String, FullPort> lastPortStates = new HashMap<>();

    private boolean lastBridgeConnectionState = false;

    private List<PortStatusListener> portStatusListeners = new CopyOnWriteArrayList<>();

    private Future<?> pollingJob;

    private VoismartSwitchBridge bridge = null;

    private Runnable pollingRunnable = new Runnable() {

        @Override
        public void run() {
            try {
                try {
                    // logger.debug("Updating thread for discovery");
                    FullConfig fullConfig = bridge.getFullConfig();
                    if (!lastBridgeConnectionState) {
                        // logger.debug("Connection to VoismartSwitch {} established.", bridge.getIPAddress());
                        lastBridgeConnectionState = true;
                        onConnectionResumed(bridge);
                    }
                    if (lastBridgeConnectionState) {
                        Map<String, FullPort> lastPortStateCopy = new HashMap<>(lastPortStates);
                        // logger.debug("Iterate port list");
                        for (final FullPort fullPort : fullConfig.getPorts()) {
                            final String portId = fullPort.getId();
                            // logger.debug("Looking at port {}", portId);
                            if (lastPortStateCopy.containsKey(portId)) {
                                final FullPort lastFullPort = lastPortStateCopy.remove(portId);
                                final OnOffType lastFullPortState = lastFullPort.getPOEStatus();
                                lastPortStates.put(portId, fullPort);
                                if (!isEqual(lastFullPortState, fullPort.getPOEStatus())) {
                                    /*
                                     * logger.debug("Status {} update for port {} detected.", fullPort.getPOEStatus(),
                                     * portId);
                                     */
                                    for (PortStatusListener portStatusListener : portStatusListeners) {
                                        try {
                                            // logger.debug("Signalling port status changed");
                                            portStatusListener.onPortStateChanged(bridge, fullPort);
                                        } catch (Exception e) {
                                            logger.error(
                                                    "An exception occurred while calling the BridgeHeartbeatListener",
                                                    e);
                                        }
                                    }
                                }
                            } else {
                                // logger.debug("Adding for port {}", portId);
                                lastPortStates.put(portId, fullPort);
                                // logger.debug("Port {} added.", portId);
                                for (PortStatusListener portStatusListener : portStatusListeners) {
                                    try {
                                        // logger.debug("Calling on port added");
                                        portStatusListener.onPortAdded(bridge, fullPort);
                                    } catch (Exception e) {
                                        logger.error("An exception occurred while calling the BridgeHeartbeatListener",
                                                e);
                                    }
                                }
                            }
                        }

                        // Check for removed ports (negli switch non succede
                        // mai)
                        for (Entry<String, FullPort> fullPortEntry : lastPortStateCopy.entrySet()) {
                            lastPortStates.remove(fullPortEntry.getKey());
                            // logger.debug("Port {} removed.", fullPortEntry.getKey());
                            for (PortStatusListener portStatusListener : portStatusListeners) {
                                try {
                                    portStatusListener.onPortRemoved(bridge, fullPortEntry.getValue());
                                } catch (Exception e) {
                                    logger.error("An exception occurred while calling the BridgeHeartbeatListener", e);
                                }
                            }
                        }
                    }
                } catch (IllegalStateException e) {
                    // logger.debug("Update thread illegal state exception. Check reachability.");
                    if (isReachable(bridge.getIPAddress())) {
                        // logger.debug("Is reachable, authethication error.");
                        lastBridgeConnectionState = false;
                        onNotAuthenticated(bridge);
                        // logger.debug("Authenticated?");
                        // logger.debug("Scheduled update again");
                        run();
                    } else {
                        // logger.debug("Not reachable set connection lost.");
                        if (lastBridgeConnectionState) {
                            lastBridgeConnectionState = false;
                            onConnectionLost(bridge);
                        }
                    }
                } catch (Exception e) {
                    if (bridge != null) {
                        if (lastBridgeConnectionState) {
                            // logger.debug("Connection to Hue Bridge {} lost.", bridge.getIPAddress());
                            lastBridgeConnectionState = false;
                            onConnectionLost(bridge);
                        }
                    }
                }
            } catch (Throwable t) {
                logger.error("An unexpected error occurred: {}", t.getMessage(), t);
            }
        }

        private boolean isReachable(String ipAddress) {
            try {
                // note that InetAddress.isReachable is unreliable, see
                // http://stackoverflow.com/questions/9922543/why-does-inetaddress-isreachable-return-false-when-i-can-ping-the-ip-address
                // That's why we do an HTTP access instead

                // If there is no connection, this line will fail
                return bridge.isReachable();
            } catch (IOException e) {
                return false;
            } /*
               * catch (ApiException e) { if
               * (e.getMessage().contains("SocketTimeout") ||
               * e.getMessage().contains("ConnectException")) { return false; }
               * else { // this seems to be only an authentication issue return
               * true; } }
               */
            // return true;
        }
    };

    public VoismartSwitchBridgeHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        // logger.debug("Initializing VoismartSwitch handler.");
        super.initialize();

        Configuration config = getThing().getConfiguration();

        try {
            if (bridge == null) {
                // logger.debug("Configuration {}", config.values());
                bridge = new VoismartSwitchBridge(InetAddress.getByName((String) config.get(HOST)),
                        (String) config.get(USER_NAME), (String) config.get(USER_PASSWORD));
            }
            onUpdate();
        } catch (Exception ex) {
            logger.warn("Coudl not onnect to switch device");
        }
    }

    @Override
    public void dispose() {
        // logger.debug("Handler disposed.");
        if (pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
            pollingJob = null;
        }
        if (bridge != null) {
            bridge = null;
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        /*
         * if(channelUID.getId().equals(CHANNEL_1)) {
         * // TODO: handle command
         * }
         */
        // not needed
    }

    public void updatePortState(final FullPort port, final OnOffType stateUpdate) {
        if (bridge != null) {
            try {
                Runnable doAction = new Runnable() {

                    @Override
                    public void run() {
                        bridge.setPortPOEState(port, stateUpdate);
                    }
                };

                scheduler.execute(doAction);
            } /*
               * catch (DeviceOffException e) { //updatePortState(port,
               * LightStateConverter.toColorLightState(OnOffType.ON));
               * updatePortState(port, stateUpdate); } catch (IOException |
               * ApiException e) { throw new RuntimeException(e); }
               */
            catch (IllegalStateException e) {
                logger.trace("Error while accessing light: {}", e.getMessage());
            }
        } else {
            logger.warn("No bridge connected or selected. Cannot set light state.");
        }
    }

    /**
     * This method is called whenever the connection to the given
     * {@link HueBridge} is lost.
     *
     * @param bridge
     *            the hue bridge the connection is lost to
     */
    public void onConnectionLost(VoismartSwitchBridge bridge) {
        // logger.debug("Bridge connection lost. Updating thing status to OFFLINE.");
        updateStatus(ThingStatus.OFFLINE);
    }

    /**
     * This method is called whenever the connection to the given
     * {@link HueBridge} is resumed.
     *
     * @param bridge
     *            the hue bridge the connection is resumed to
     */
    public void onConnectionResumed(VoismartSwitchBridge bridge) {
        // logger.debug("Bridge connection resumed. Updating thing status to ONLINE.");
        updateStatus(ThingStatus.ONLINE);
        // now also re-initialize all port handlers
        // logger.debug("Re-initialize all port handlers");
        for (Thing thing : ((Bridge) getThing()).getThings()) {
            ThingHandler handler = thing.getHandler();
            if (handler != null) {
                handler.initialize();
            }
        }
    }

    /**
     * This method is called whenever the connection to the given
     * {@link HueBridge} is available, but requests are not allowed due to a
     * missing or invalid authentication.
     *
     * @param bridge
     *            the hue bridge the connection is not authorized
     */
    public void onNotAuthenticated(VoismartSwitchBridge bridge) {
        // logger.debug("onNoAutheticatted...try login again");
        String userName = (String) getConfig().get(USER_NAME);
        String userPassword = (String) getConfig().get(USER_PASSWORD);
        if (userName != null && userPassword != null) {
            try {
                bridge.login(userName, userPassword);
                // logger.debug("login done");
            } catch (Exception e) {
                logger.info("Voismart bridge {} is not authenticated - please press the pairing button on the bridge.",
                        getConfig().get(HOST));
                /*
                 * try { bridge.link(userName, "gateway"); logger.info(
                 * "User '{}' has been successfully added to Hue bridge.",
                 * userName); } catch (Exception ex) {
                 * logger.debug("Failed adding user '{}' to Hue bridge.",
                 * userName); }
                 */
            }
        }
    }

    public boolean registerPortStatusListener(PortStatusListener portStatusListener) {
        // logger.debug("Register port status listener");
        if (portStatusListener == null) {
            throw new NullPointerException("It's not allowed to pass a null PortStatusListener.");
        }
        boolean result = portStatusListeners.add(portStatusListener);
        if (result) {
            onUpdate();
            // inform the listener initially about all lights and their states
            for (FullPort port : lastPortStates.values()) {
                portStatusListener.onPortAdded(bridge, port);
            }
        }
        return result;
    }

    public boolean unregisterPortStatusListener(PortStatusListener portStatusListener) {
        // logger.debug("Unregist port status listener");
        boolean result = portStatusListeners.remove(portStatusListener);
        if (result) {
            onUpdate();
        }
        return result;
    }

    public FullPort getPortById(String portId) {
        return lastPortStates.get(portId);
    }

    public Collection<FullPort> getFullPorts() {
        Collection<FullPort> ports = null;
        if (bridge != null) {
            try {
                ports = bridge.getFullConfig().getPorts();
            } catch (Exception e) {
                logger.error("Bridge cannot search for new lights.", e);
            }
        }
        return ports;
    }

    public void startSearch() {
        if (bridge != null) {
            // try {
            bridge.startSearch();
            /*
             * } catch (IOException | ApiException e) {
             * logger.error("Bridge cannot start search mode", e); }
             */
        }
    }

    private synchronized void onUpdate() {
        if (bridge != null) {
            if (pollingJob == null || pollingJob.isCancelled()) {
                pollingJob = scheduler.scheduleAtFixedRate(pollingRunnable, 1, POLLING_FREQUENCY, TimeUnit.SECONDS);
            }
        }
    }

    private boolean isEqual(OnOffType state1, OnOffType state2) {
        try {
            return state1.equals(state2);
            /*
             * return state1.getAlertMode().equals(state2.getAlertMode()) &&
             * state1.isOn() == state2.isOn() &&
             * state1.getEffect().equals(state2.getEffect()) &&
             * state1.getBrightness() == state2.getBrightness() &&
             * state1.getColorMode().equals(state2.getColorMode()) &&
             * state1.getColorTemperature() == state2.getColorTemperature() &&
             * state1.getHue() == state2.getHue() && state1.getSaturation() ==
             * state2.getSaturation();
             */
        } catch (Exception e) {
            // if a device does not support color, the Jue library throws an NPE
            // when testing for color-related properties
            return true;
        }
    }

}
