/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.mattuino.internal;

import static org.eclipse.smarthome.binding.mattuino.MattuinoBindingConstants.THING_TYPE_DOOR;

import java.util.Collections;
import java.util.Set;

import org.eclipse.smarthome.binding.mattuino.MattuinoBindingConstants;
import org.eclipse.smarthome.binding.mattuino.handler.MattuinoHandler;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MattuinoHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Smilzo - Initial contribution
 */
public class MattuinoHandlerFactory extends BaseThingHandlerFactory {

    private final static Logger logger = LoggerFactory.getLogger(MattuinoHandlerFactory.class);

    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .singleton(MattuinoBindingConstants.THING_TYPE_DOOR);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        logger.debug("Try Creating Mattuino " + thing.getUID());
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_DOOR)) {
            logger.debug("Creating Mattuino " + thing.getUID());
            return new MattuinoHandler(thing);
        }

        return null;
    }
}
