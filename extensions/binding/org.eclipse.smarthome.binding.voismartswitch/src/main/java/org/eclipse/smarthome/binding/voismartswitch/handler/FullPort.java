package org.eclipse.smarthome.binding.voismartswitch.handler;

import org.eclipse.smarthome.core.library.types.OnOffType;

public class FullPort {
	
	//See constants binding file
	public final static String uid = "voismartswitchport";
	
	private String id;
	private OnOffType POE;
	
	FullPort(String portId, OnOffType status) {
		id = portId;
		POE = status;
	}
	
	public String getId() {
		return id;
	}
	
	public OnOffType getPOEStatus() {
		return POE;
	}
	
	public void setPortPOEStatus(OnOffType status) {
		POE = status;
	}

}
