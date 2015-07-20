/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.person;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link PersonBinding} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Smilzo - Initial contribution
 */
public class PersonBindingConstants {

    public static final String BINDING_ID = "person";

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_PERSON = new ThingTypeUID(BINDING_ID, "person");

    // List of all Channel ids
    // public final static String CHANNEL_POSITION = "position";
    public final static String CHANNEL_ACTIVITY = "activity";
    public final static String CHANNEL_BADGE = "badge-id";

}
