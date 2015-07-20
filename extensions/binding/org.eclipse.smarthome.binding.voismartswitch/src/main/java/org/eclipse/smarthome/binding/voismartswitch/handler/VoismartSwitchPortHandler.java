package org.eclipse.smarthome.binding.voismartswitch.handler;

import static org.eclipse.smarthome.binding.voismartswitch.VoismartSwitchBindingConstants.*;

import java.util.Collections;
import java.util.Set;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoismartSwitchPortHandler extends BaseThingHandler implements PortStatusListener {

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_VOISMARTSWITCH_PORT);

    private String portId;

    // private OnOffType lastPortStatus;

    private final static Logger logger = LoggerFactory.getLogger(VoismartSwitchPortHandler.class);

    private VoismartSwitchBridgeHandler bridgeHandler;

    public VoismartSwitchPortHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing voismart switch port handler.");
        final String configPortId = (String) getConfig().get(PORT_ID);
        if (configPortId != null) {
            portId = configPortId;
            logger.debug("Port ID is {}", configPortId);
            // note: this call implicitly registers our handler as a listener on
            // the bridge
            if (getVoismartSwitchBridgeHandler() != null) {
                logger.debug("Get bridge done");
                ;
                ThingStatusInfo statusInfo = getBridge().getStatusInfo();
                updateStatus(statusInfo.getStatus(), statusInfo.getStatusDetail(), statusInfo.getDescription());
                logger.debug("Set thing status from bridge status {}", getBridge().getStatus());
                /*
                 * FullPort fullPort = getPort(); if (fullPort != null) {
                 * updateProperty(Thing.PROPERTY_FIRMWARE_VERSION,
                 * fullLight.getSoftwareVersion()); }
                 */
            } else {
                logger.debug("Null bridge can't se thing status...scheduling");
                // try again later???
            }
        }
    }

    @Override
    public void dispose() {
        logger.debug("Handler disposes. Unregistering listener.");
        if (portId != null) {
            VoismartSwitchBridgeHandler bridgeHandler = getVoismartSwitchBridgeHandler();
            if (bridgeHandler != null) {
                getVoismartSwitchBridgeHandler().unregisterPortStatusListener(this);
            }
            portId = null;
        }
    }

    private FullPort getPort() {
        VoismartSwitchBridgeHandler bridgeHandler = getVoismartSwitchBridgeHandler();
        if (bridgeHandler != null) {
            // logger.debug("Get port id {} and search on bridge {}", portId, bridgeHandler.getPortById(portId));
            return bridgeHandler.getPortById(portId);
        }
        return null;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        VoismartSwitchBridgeHandler voismartBridge = getVoismartSwitchBridgeHandler();
        if (voismartBridge == null) {
            logger.warn("VoismartSwitch bridge handler not found. Cannot handle command without bridge.");
            return;
        }

        FullPort port = getPort();
        if (port == null) {
            logger.warn("Voismart switch port not known on bridge. Cannot handle command.");
            return;
        }

        OnOffType portState = null;

        // TODO Auto-generated method stub
        switch (channelUID.getId()) {
            case STATE:
                if (command instanceof OnOffType) {
                    portState = command == OnOffType.ON ? OnOffType.ON : OnOffType.OFF;
                    if (portState != null) {
                        voismartBridge.updatePortState(port, portState);
                    }
                }
                break;
            default:
                logger.warn("Command send to an unknown channel id: " + channelUID);
                break;
        }

    }

    private synchronized VoismartSwitchBridgeHandler getVoismartSwitchBridgeHandler() {
        // logger.debug("Get bridge called");
        if (this.bridgeHandler == null) {
            Bridge bridge = getBridge();
            if (bridge == null) {
                return null;
            }
            ThingHandler handler = bridge.getHandler();
            if (handler instanceof VoismartSwitchBridgeHandler) {
                this.bridgeHandler = (VoismartSwitchBridgeHandler) handler;
                logger.debug("Listener registered");
                this.bridgeHandler.registerPortStatusListener(this);
            } else {
                return null;
            }
        }
        return this.bridgeHandler;
    }

    @Override
    public void onPortStateChanged(VoismartSwitchBridge bridge, FullPort fullPort) {
        logger.debug("Port Handler onPortStateChanged {} with {}", fullPort.getId(), fullPort.getPOEStatus());
        if (fullPort.getId().equals(portId)) {
            // lastPortStatus = null;
            updateState(new ChannelUID(getThing().getUID(), STATE), fullPort.getPOEStatus());
        }
    }

    @Override
    public void onPortRemoved(VoismartSwitchBridge bridge, FullPort fullPort) {
        if (fullPort.getId().equals(portId)) {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    @Override
    public void onPortAdded(VoismartSwitchBridge bridge, FullPort fullPort) {
        logger.debug("On port added signalled");
        if (fullPort.getId().equals(portId)) {
            logger.debug("Port {} is ONLINE", fullPort.getId());
            updateStatus(ThingStatus.ONLINE);
            onPortStateChanged(bridge, fullPort);
        }
    }

}
