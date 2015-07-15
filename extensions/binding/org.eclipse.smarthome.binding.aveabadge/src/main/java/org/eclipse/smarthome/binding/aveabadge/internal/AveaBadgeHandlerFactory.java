/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.aveabadge.internal;

import static org.eclipse.smarthome.binding.aveabadge.AveaBadgeBindingConstants.THING_TYPE_BADGE;

import java.util.Collections;
import java.util.Set;

import org.eclipse.smarthome.binding.aveabadge.handler.AveaBadgeHandler;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AveaBadgeHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Smilzo - Initial contribution
 */
public class AveaBadgeHandlerFactory extends BaseThingHandlerFactory {

    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(THING_TYPE_BADGE);

    private final static Logger logger = LoggerFactory.getLogger(AveaBadgeHandler.class);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        logger.debug("AveaBadge createing handler " + thing.getUID());

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_BADGE)) {
            return new AveaBadgeHandler(thing);
        }

        return null;
    }

    public static AveaWebMsg defaultResponse(AveaWebMsg msg) {
        logger.warn("Received request from {} but no thing configurated", msg.getIP().getHostAddress());
        if (msg.getCmd().equals(AveaWebMsg.CMD.CO))
            msg.setBeep(AveaWebMsg.BEEP.LONG);

        return msg;
    }
}
