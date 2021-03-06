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
import java.util.concurrent.TimeUnit;

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
    private final ChannelUID doorStateChannelUID;

    private final static Logger logger = LoggerFactory.getLogger(MattuinoHandler.class);
    private HttpClient client = null;
    private URIBuilder uri;
    private boolean scheduled;

    public MattuinoHandler(Thing thing) {
        super(thing);
        doorStateChannelUID = new ChannelUID(getThing().getUID(), STATE);
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

        Runnable turnoff = new Runnable() {
            @Override
            public void run() {
                logger.debug("TurnOff fired " + getThing().getUID());
                updateState(doorStateChannelUID, OnOffType.OFF);
                logger.debug("Done");
                scheduled = false;
            }
        };

        scheduler.schedule(turnoff, 5, TimeUnit.SECONDS);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        logger.debug("Mattuino:" + this.getThing().getUID() + " handling command " + command + " for channel "
                + channelUID.getAsString());

        switch (channelUID.getId()) {
            case STATE:
                if (command instanceof OnOffType && command.equals(OnOffType.ON) && client != null && !scheduled) {
                    Runnable doAction = new Runnable() {

                        @Override
                        public void run() {
                            synchronized (client) {
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
                                            updateState(new ChannelUID(getThing().getUID(), STATE), OnOffType.OFF);
                                        } else {
                                            logger.warn("Controller response error");
                                        }
                                    }
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                    updateStatus(ThingStatus.OFFLINE);
                                }
                                scheduled = false;
                            }
                        }
                    };

                    scheduled = true;
                    scheduler.execute(doAction);
                    // throw new Exception();
                }
                break;
        }

    }
}
