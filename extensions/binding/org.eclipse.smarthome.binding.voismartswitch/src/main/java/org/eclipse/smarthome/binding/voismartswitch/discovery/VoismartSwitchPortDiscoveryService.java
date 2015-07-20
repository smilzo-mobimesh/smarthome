package org.eclipse.smarthome.binding.voismartswitch.discovery;

import static org.eclipse.smarthome.binding.voismartswitch.VoismartSwitchBindingConstants.BINDING_ID;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.binding.voismartswitch.handler.FullPort;
import org.eclipse.smarthome.binding.voismartswitch.handler.PortStatusListener;
import org.eclipse.smarthome.binding.voismartswitch.handler.VoismartSwitchBridge;
import org.eclipse.smarthome.binding.voismartswitch.handler.VoismartSwitchBridgeHandler;
import org.eclipse.smarthome.binding.voismartswitch.handler.VoismartSwitchPortHandler;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoismartSwitchPortDiscoveryService extends AbstractDiscoveryService implements PortStatusListener {

    private final static Logger logger = LoggerFactory.getLogger(VoismartSwitchPortDiscoveryService.class);

    private final static int SEARCH_TIME = 300;

    private VoismartSwitchBridgeHandler voismartSwitchBridgeHandler;

    public VoismartSwitchPortDiscoveryService(VoismartSwitchBridgeHandler voismartSwitchBridgeHandler) {
        super(SEARCH_TIME);
        this.voismartSwitchBridgeHandler = voismartSwitchBridgeHandler;
    }

    public void activate() {
        logger.debug("Discovery service activated");
        ;
        voismartSwitchBridgeHandler.registerPortStatusListener(this);
    }

    @Override
    public void deactivate() {
        logger.debug("Discovery service deactivated");
        ;
        removeOlderResults(new Date().getTime());
        voismartSwitchBridgeHandler.unregisterPortStatusListener(this);
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return VoismartSwitchPortHandler.SUPPORTED_THING_TYPES;
    }

    @Override
    public void startScan() {
        logger.debug("Start discovery scan");
        Collection<FullPort> ports = voismartSwitchBridgeHandler.getFullPorts();
        if (ports != null) {
            for (FullPort p : ports) {
                onPortAddedInternal(p);
            }
        }
        // search for unpaired lights
        voismartSwitchBridgeHandler.startSearch();
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        logger.debug("Stop discovery scan");
        removeOlderResults(getTimestampOfLastScan());
    }

    @Override
    public void onPortAdded(VoismartSwitchBridge bridge, FullPort port) {
        logger.debug("onPortAdded");
        onPortAddedInternal(port);
    }

    private void onPortAddedInternal(FullPort port) {
        ThingUID thingUID = getThingUID(port);
        if (thingUID != null) {
            ThingUID bridgeUID = voismartSwitchBridgeHandler.getThing().getUID();
            Map<String, Object> properties = new HashMap<>(1);
            // properties.put(PORT_ID, port.getId());

            /*
             * TODO retrieve the light´s unique id (available since Hue bridge versions > 1.3) and set the mac address
             * as discovery result representationÏ. For this purpose the jue library has to be modified.
             */

            logger.debug("Pubblico la porta {} scoperta con state {}", port.getId(), port.getPOEStatus());
            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                    .withBridge(bridgeUID).withLabel(port.getId()).build();

            thingDiscovered(discoveryResult);
        } else {
            logger.debug("Discovered unsupported port with id {} and state {}", port.getId(), port.getPOEStatus());
        }
    }

    @Override
    public void onPortRemoved(VoismartSwitchBridge bridge, FullPort port) {
        logger.debug("Cosa rimossa " + port.getId());
        ThingUID thingUID = getThingUID(port);

        if (thingUID != null) {
            thingRemoved(thingUID);
        }
    }

    @Override
    public void onPortStateChanged(VoismartSwitchBridge bridge, FullPort fullPort) {
        // nothing to do
        // Thing port = voismartSwitchBridgeHandler.updatePortState(fullPort, fullPort.getPOEStatus());

        // logger.debug("Port Handler onPortStateChanged {} with {}", fullPort.getId(), fullPort.getPOEStatus());
        // updateState(new ChannelUID(getThing().getUID(), STATE), fullPort.getPOEStatus());

    }

    private ThingUID getThingUID(FullPort port) {
        // logger.debug("Try to find thing UID");
        ThingUID bridgeUID = voismartSwitchBridgeHandler.getThing().getUID();
        // logger.debug("Bridge UID {}", bridgeUID);
        ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID, FullPort.uid);
        // logger.debug("Thing UID {}", thingTypeUID);
        // logger.debug("Supported thing UIDs {}", getSupportedThingTypes());

        if (getSupportedThingTypes().contains(thingTypeUID)) {
            String thingPortId = port.getId();
            ThingUID thingUID = new ThingUID(thingTypeUID, bridgeUID, thingPortId);
            return thingUID;
        } else {
            return null;
        }
    }

}
