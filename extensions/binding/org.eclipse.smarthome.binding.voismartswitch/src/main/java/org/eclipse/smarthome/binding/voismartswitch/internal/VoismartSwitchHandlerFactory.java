/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.voismartswitch.internal;

import static org.eclipse.smarthome.binding.voismartswitch.VoismartSwitchBindingConstants.*;

import java.util.Collections;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;

import org.eclipse.smarthome.binding.voismartswitch.discovery.VoismartSwitchPortDiscoveryService;
import org.eclipse.smarthome.binding.voismartswitch.handler.VoismartSwitchBridgeHandler;
import org.eclipse.smarthome.binding.voismartswitch.handler.VoismartSwitchPortHandler;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;

import org.osgi.framework.ServiceRegistration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * The {@link VoismartSwitchHandlerFactory} is responsible for creating things and thing 
 * handlers.
 * 
 * @author Smilzo - Initial contribution
 */
public class VoismartSwitchHandlerFactory extends BaseThingHandlerFactory {
    
	private Logger logger = LoggerFactory.getLogger(VoismartSwitchHandlerFactory.class);

	private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Sets.union(VoismartSwitchBridgeHandler.SUPPORTED_THING_TYPES,
    		VoismartSwitchPortHandler.SUPPORTED_THING_TYPES);

    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
    	logger.debug("Create handler for {}", thing.getThingTypeUID().toString());
        if (VoismartSwitchBridgeHandler.SUPPORTED_THING_TYPES.contains(thing.getThingTypeUID())) {
        	VoismartSwitchBridgeHandler handler = new VoismartSwitchBridgeHandler((Bridge) thing);
            logger.debug("Try to create discovery service");
        	registerPortDiscoveryService(handler);
            return handler;
        } else if (VoismartSwitchPortHandler.SUPPORTED_THING_TYPES.contains(thing.getThingTypeUID())) {
            logger.debug("Register new thing");
            return new VoismartSwitchPortHandler(thing);
        } else {
            logger.debug("Error unknown thing");
            return null;
        }
    }
    
    @Override
    public Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration, ThingUID thingUID,
            ThingUID bridgeUID) {
    	logger.debug("Create thing {}", thingTypeUID.toString());
        if (VoismartSwitchBridgeHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            ThingUID voismartSwitchBridgeUID = getBridgeThingUID(thingTypeUID, thingUID, configuration);
            logger.debug("Creo un bridge");
            return super.createThing(thingTypeUID, configuration, voismartSwitchBridgeUID, null);
        }
        if (VoismartSwitchPortHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            ThingUID voismartSwitchPortUID = getPortUID(thingTypeUID, thingUID, configuration, bridgeUID);
            logger.debug("Port UID {}, bridge UID {}", voismartSwitchPortUID, bridgeUID);
            return super.createThing(thingTypeUID, configuration, voismartSwitchPortUID, bridgeUID);
        }
        throw new IllegalArgumentException("The thing type " + thingTypeUID + " is not supported by the hue binding.");
    }

    private ThingUID getBridgeThingUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration) {
        if (thingUID == null) {
            String name = (String) configuration.get(SWITCH_NAME);
            thingUID = new ThingUID(thingTypeUID, name);
        }
        return thingUID;
    }

    private ThingUID getPortUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration,
            ThingUID bridgeUID) {
        String portId = (String) configuration.get(PORT_ID);

        if (thingUID == null) {
        	logger.debug("Bridge UID {}", bridgeUID.getId());
            thingUID = new ThingUID(thingTypeUID, portId, bridgeUID.getId());
        }
        return thingUID;
    }

    private synchronized void registerPortDiscoveryService(VoismartSwitchBridgeHandler bridgeHandler) {
        logger.debug("registerPortDiscoveryService");
    	VoismartSwitchPortDiscoveryService discoveryService = new VoismartSwitchPortDiscoveryService(bridgeHandler);
        discoveryService.activate();
        this.discoveryServiceRegs.put(bridgeHandler.getThing().getUID(), bundleContext.registerService(
                DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
    }
    
    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof VoismartSwitchBridgeHandler) {
            logger.debug("Remove handler");
        	ServiceRegistration<?> serviceReg = this.discoveryServiceRegs.get(thingHandler.getThing().getUID());
            if (serviceReg != null) {
                // remove discovery service, if bridge handler is removed
            	VoismartSwitchPortDiscoveryService service = (VoismartSwitchPortDiscoveryService) bundleContext.getService(serviceReg
                        .getReference());
                service.deactivate();
                serviceReg.unregister();
                discoveryServiceRegs.remove(thingHandler.getThing().getUID());
            }
        }
    }
}

