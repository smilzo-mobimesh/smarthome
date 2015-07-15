/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.mattuino.handler;

import static org.eclipse.smarthome.binding.mattuino.MattuinoBindingConstants.*;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MattuinoHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Smilzo - Initial contribution
 */
public class MattuinoHandler extends BaseThingHandler {

    private final static String SUCCESS_RESPONSE = "PORTA APERTA";
    private final static int SLEEP_TIME = 4000;

    private final static Logger logger = LoggerFactory.getLogger(MattuinoHandler.class);
    private HttpClient client = null;
    private URIBuilder uri;

    public MattuinoHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        Configuration config = getThing().getConfiguration();

        client = new DefaultHttpClient();// HttpClientBuilder.create().build();
        uri = new URIBuilder();
        uri.setScheme("http").setHost((String) config.get(HOST));
        uri.setPath("/OPEN");
        logger.info("Done uri:" + uri);
        updateStatus(ThingStatus.ONLINE);

        Thread t = new Thread(new TurnOff(this, logger));
        t.start();

    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        logger.debug("Mattuino:" + this.getThing().getUID() + " handling command " + command + " for channel "
                + channelUID.getAsString());

        switch (channelUID.getId()) {
            case STATE:
                if (command instanceof OnOffType && command.equals(OnOffType.ON) && client != null) {
                    HttpGet httpGet = new HttpGet(uri.toString());
                    logger.debug("Sending get request to " + uri.toString());
                    HttpResponse httpResponse;
                    try {
                        httpResponse = client.execute(httpGet);
                        logger.debug("Done");
                        if (httpResponse.getStatusLine().getStatusCode() == 200) {
                            // Controllo la scritta
                            String response = IOUtils.toString(httpResponse.getEntity().getContent());
                            logger.debug("Got response " + response);
                            if (response.contains(SUCCESS_RESPONSE)) {
                                logger.debug("Request success");
                                Thread t = new Thread(new TurnOff(this, logger));
                                t.start();

                                return;
                            } else {
                                logger.warn("Controller response error");
                            }
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        updateStatus(ThingStatus.OFFLINE);
                    }
                    // throw new Exception();
                }
                break;
        }

    }

    private class TurnOff implements Runnable {
        private final MattuinoHandler door;
        private final Logger logger;

        TurnOff(MattuinoHandler currentDoor, Logger logger) {
            this.door = currentDoor;
            this.logger = logger;
            logger.debug("TurnOff created");
        }

        @Override
        public void run() {
            try {
                logger.debug("TurnOff called");
                Thread.sleep(SLEEP_TIME);
                logger.debug("TurnOff fired " + getThing().getUID());
                updateState(new ChannelUID(getThing().getUID(), STATE), OnOffType.OFF);
                logger.debug("Done");
                ;
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
