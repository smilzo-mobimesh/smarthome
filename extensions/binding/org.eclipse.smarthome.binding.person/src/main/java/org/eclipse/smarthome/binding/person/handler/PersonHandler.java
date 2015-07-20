/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.person.handler;

import static org.eclipse.smarthome.binding.person.PersonBindingConstants.*;

import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PersonHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Smilzo - Initial contribution
 */
public class PersonHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(PersonHandler.class);

    public PersonHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("I'm born");
        Runnable init = new Runnable() {

            @Override
            public void run() {
                Configuration config = getThing().getConfiguration();
                try {
                    Object badge_id = config.get("personal-badge-id");
                    if (badge_id != null) {
                        updateState(new ChannelUID(getThing().getUID(), CHANNEL_BADGE),
                                new DecimalType(Long.parseLong((String) badge_id)));
                        logger.debug("I have a badge {}", badge_id);
                    }
                    String activity = (String) config.get("activity");
                    logger.debug("Got activity {}", activity);
                    updateState(new ChannelUID(getThing().getUID(), CHANNEL_ACTIVITY), StringType.valueOf(activity));
                } catch (Exception e) {
                }
            }
        };

        scheduler.schedule(init, 5, TimeUnit.SECONDS);

        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        /*
         * if (channelUID.getId().equals(CHANNEL_POSITION)) {
         * // TODO: handle command
         * }
         */ }
}
