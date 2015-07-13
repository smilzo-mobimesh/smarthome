/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.voismartswitch;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link VoismartSwitchBinding} class defines common constants, which are 
 * used across the whole binding.
 * 
 * @author Smilzo - Initial contribution
 */
public class VoismartSwitchBindingConstants {

    public static final String BINDING_ID = "voismartswitch";
    
    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_VOISMARTSWITCH = new ThingTypeUID(BINDING_ID, "voismartswitchbridge");
    public final static ThingTypeUID THING_TYPE_VOISMARTSWITCH_PORT = new ThingTypeUID(BINDING_ID, "voismartswitchport");

    // List of all Channel ids
    public final static String STATE = "state";
        
    // Bridge config properties
    public static final String SWITCH_NAME = "switchName";
    public static final String HOST = "ipAddress";
    public static final String USER_NAME = "userName";
    public static final String USER_PASSWORD = "userPassword";

    // Port config properties
    public static final String PORT_ID = "portNumber";
    public static final String BRIDGE_UID = "VoismartSwitchPort.xml";

}
