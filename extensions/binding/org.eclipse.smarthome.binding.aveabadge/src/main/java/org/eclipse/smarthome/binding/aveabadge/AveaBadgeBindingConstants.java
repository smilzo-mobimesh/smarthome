/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.aveabadge;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link AveaBadgeBinding} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Smilzo - Initial contribution
 */
public class AveaBadgeBindingConstants {

    public static final String BINDING_ID = "aveabadge";

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_BADGE = new ThingTypeUID(BINDING_ID, "WEB08S");

    // List of all Channel ids
    public final static String BADGE_ID = "badge-id";

    // Bridge config properties
    public static final String HOST = "ipAddress";
    public static final String HEARTBEAT = "heartBeatInterval";
    public static final String HEARTBEATMAXLOST = "heartBeatMaxLost";

}
