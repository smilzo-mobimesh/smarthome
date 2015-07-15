package org.eclipse.smarthome.binding.voismartswitch.handler;

import java.util.Collection;
import java.util.HashMap;

public class FullConfig {
	
	private HashMap<String, FullPort> portList;
	
	FullConfig() {
		portList = new HashMap<String, FullPort>();
	}
	
	public Collection<FullPort> getPorts() {
		return portList.values();
	}
	
	void addPort(FullPort port) {
		portList.put(port.getId(), port);
	}
	
	FullPort getPortById(String portId) {
		return portList.get(portId);
	}

}
