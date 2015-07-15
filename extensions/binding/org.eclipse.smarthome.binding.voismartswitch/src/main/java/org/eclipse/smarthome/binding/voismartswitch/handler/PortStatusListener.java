package org.eclipse.smarthome.binding.voismartswitch.handler;

public interface PortStatusListener {

    /**
     * This method is called whenever the state of the given port has changed. The new state can be obtained by
     * {@link FullPort#getState()}.
     * 
     * @param bridge The bridge the changed port is connected to.
     * @param port The port which received the state update.
     */
    public void onPortStateChanged(VoismartSwitchBridge bridge, FullPort port);

    /**
     * This method us called whenever a port is removed.
     * 
     * @param bridge The bridge the removed port was connected to.
     * @param light The port which is removed.
     */
    public void onPortRemoved(VoismartSwitchBridge bridge, FullPort port);

    /**
     * This method us called whenever a port is added.
     * 
     * @param bridge The bridge the added port was connected to.
     * @param port The port which is added.
     */
    public void onPortAdded(VoismartSwitchBridge bridge, FullPort port);

}
