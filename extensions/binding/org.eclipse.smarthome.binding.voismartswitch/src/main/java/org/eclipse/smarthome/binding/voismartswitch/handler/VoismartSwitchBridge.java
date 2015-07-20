package org.eclipse.smarthome.binding.voismartswitch.handler;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoismartSwitchBridge {

    private final static Logger logger = LoggerFactory.getLogger(VoismartSwitchBridgeHandler.class);

    private InetAddress ip;
    // private String username, password;
    private boolean logged = false;

    private HttpClient client;
    private URIBuilder uri;

    private FullConfig currentPortsConfig;

    VoismartSwitchBridge(InetAddress ip, String username, String password) {
        logger.info("Creating VoismartSwitch bridge {} ", ip.getHostName());
        this.ip = ip;
        /*
         * this.username = username; this.password = password;
         */

        client = new DefaultHttpClient();// HttpClientBuilder.create().build();
        uri = new URIBuilder();
        uri.setScheme("http").setHost(ip.getHostAddress());
        currentPortsConfig = new FullConfig();
        logger.info("Done");
    }

    String getIPAddress() {
        return ip.getHostAddress();
    }

    FullConfig getFullConfig() throws Exception {
        currentPortsConfig = new FullConfig();
        if (isReachable())
            if (retriveCurrentPortPOEState()) {
                logger.debug("Get full port config read");
                return currentPortsConfig;
            }
        // Non raggiungibile
        logger.debug("Not reachable");
        throw new Exception();
    }

    boolean isReachable() throws IOException {
        return ip.isReachable(5000);
    }

    public void startSearch() {
        // Ocioi il login???
        retriveCurrentPortPOEState();
    }

    synchronized void setPortPOEState(FullPort port, OnOffType state) {
        logger.debug("1 - Request port {} status change from {} to {}", port.getId(), port.getPOEStatus(), state);
        retriveCurrentPortPOEState();
        // logger.debug("2 - Reuqest port {} status {}, state {}", port.getId(), port.getPOEStatus(), state);
        currentPortsConfig.getPortById(port.getId()).setPortPOEStatus(state);
        // logger.debug("3 - Reuqest port {} status {}, state {}", port.getId(), port.getPOEStatus(), state);
        // Devo controllare se nel passaggio lo stato della porta che voglio
        // cambiare non viene modificato

        setPortPOEState();
        logger.debug("4 - Done port {} status changed to {}", port.getId(), port.getPOEStatus());
    }

    public synchronized boolean retriveCurrentPortPOEState() throws IllegalStateException {
        // logger.debug("Get poe status.");
        boolean logged = true;
        boolean res = false;
        int retry = 0;
        // ArrayList<OnOffType> curr_status = new ArrayList<OnOffType>();

        while (!res && retry < 3) {
            // if (logged) {
            // logger.debug("Get current status");
            try {
                uri.removeQuery();
                uri.setPath("/POE.htm");
                HttpResponse httpResponse = makeHttpGetRequest();
                String response = IOUtils.toString(httpResponse.getEntity().getContent());
                // logger.debug("Got status response");
                // logger.debug("My response" + response);
                logged = !response.contains("Time Out");
                if (!logged) {
                    logger.debug("Session timeout");
                    throw new IllegalStateException();
                } else {
                    Integer l = 8;
                    int h = 0;
                    int port_num = 1;
                    int poe_num = 1;
                    String stat = "NAME=\"POE" + poe_num + "\" VALUE=\"" + l.toString() + "\"";
                    // logger.debug("Searching for string {}", stat);
                    int pos = response.lastIndexOf(stat);
                    // logger.debug("Found at", pos);
                    while (pos != -1) {
                        // logger.debug("Status found at " + pos);
                        if (pos != -1) {
                            String cutted = response.substring(pos + stat.length(), pos + stat.length() + 24);
                            // logger.debug("Status " + port_num + " is " + cutted.contains("checked"));
                            // logger.debug("Parsed " + cutted.contains("checked"));
                            FullPort port = currentPortsConfig.getPortById(Integer.toString(port_num));
                            OnOffType currentState = cutted.contains("checked") ? OnOffType.ON : OnOffType.OFF;
                            if (port == null) {
                                port = new FullPort(Integer.toString(port_num), currentState);
                                currentPortsConfig.addPort(port);
                            } else {
                                port.setPortPOEStatus(currentState);
                            }

                            // Aggiorno i contatori
                            if (l > 1)
                                l -= l / 2;
                            else
                                l = 8;
                            h++;
                            port_num++;
                            poe_num = Math.floorDiv(port_num, 4) + 1;
                            stat = "NAME=\"POE" + poe_num + "\" VALUE=\"" + l.toString() + "\"";
                            // logger.debug("Searching for string {}", stat);
                            pos = response.lastIndexOf(stat);
                            // logger.debug("Found at", pos);
                        }
                    }

                    res = true;
                    // logger.debug("Done");
                }
            } catch (IllegalStateException ex) {
                throw ex;
            } catch (Exception ex) {
                res = false;
                logged = false;
            }
            retry++;
        }

        return res;
    }

    private boolean setPortPOEState() {
        // logger.debug("Updating poe status for " + currentPortsConfig.getPorts().size() + " ports");
        boolean logged = true;
        boolean res = false;
        int retry = 0;

        while (!res && retry < 3) {

            if (logged) {
                // logger.debug("Sono loggato vado avanti");
                synchronized (client) {
                    try {
                        uri.removeQuery();
                        uri.setPath("/cgi/POE.cgi");
                        // logger.debug("URI1:" + uri.toString());
                        Integer l;
                        int h = 1;
                        for (int poe_num = 1; poe_num <= (currentPortsConfig.getPorts().size() / 4); poe_num++) {
                            l = 8;
                            for (int i = 0; i < 4; i++) {
                                // logger.debug("I: " + i + " L: " + l + " H(port): " + h);
                                if (currentPortsConfig.getPortById(Integer.toString(h)).getPOEStatus()
                                        .equals(OnOffType.ON)) {
                                    uri.addParameter("POE" + poe_num, l.toString());
                                    // logger.debug("URI1 POE:" + uri.toString());
                                }
                                l -= l / 2;
                                h++;
                            }
                        }

                        // logger.debug("URI2:" + uri.toString());
                        uri.addParameter("update", "Update");
                        // logger.debug("URI3:" + uri.toString());
                        // Devo controllare se è andato a buon fine
                        HttpResponse httpResponse = makeHttpGetRequest();
                        String response = IOUtils.toString(httpResponse.getEntity().getContent());
                        // logger.debug("My response\n" + response);
                        res = response.contains("POE Configuration");
                        if (!res) {
                            // logger.debug("Failed");
                            logged = !response.contains("Time Out");
                            if (!logged) {
                                // logger.debug("Session timeout");
                            }
                        } else {
                            // logger.debug("Done");
                        }
                    } catch (Exception ex) {
                        // logger.debug("Get exception");
                        res = false;
                        logged = false;
                    }
                }
            } else {
                // logger.debug("Not logged");
                // Da vedere come gestirlo
                // logged = login();
            }

            retry++;
        }

        return res;
    }

    private HttpResponse makeHttpGetRequest() throws Exception {
        HttpGet httpGet = new HttpGet(uri.toString());
        // logger.debug("Sending get request to " + uri.toString());
        HttpResponse httpResponse = client.execute(httpGet);
        // logger.debug("Done");
        if (httpResponse.getStatusLine().getStatusCode() == 200) {
            return httpResponse;
        } else {
            throw new Exception();
        }
    }

    private HttpResponse makeHttpPostRequest(Header[] headers, HttpEntity entity) throws Exception {
        HttpResponse httpResponse = null;
        try {
            HttpPost httpPost = new HttpPost(uri.toString());
            if (headers != null) {
                httpPost.setHeaders(headers);
            }
            if (entity != null) {
                httpPost.setEntity(entity);
            }
            // httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            // logger.debug("Sending post 3 request to " + uri.toString());
            if (client != null) {
                // logger.debug("Client " + client.toString());
            } else {
                // logger.debug("Client null");
            }
            httpResponse = client.execute(httpPost);
            // logger.debug("Done");
        } catch (Exception e) {
            // logger.debug("Cecczione");
        }

        if (httpResponse != null) {
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                return httpResponse;
            } else {
                throw new Exception();
            }
        }
        return null;
    }

    private String findField(String text, String challange_start, String challange_end) {
        String challange = null;
        String tmp_text;

        int pos = text.lastIndexOf(challange_start) + challange_start.length();
        tmp_text = text.substring(pos);
        if (pos != -1) {
            int pos_end = tmp_text.indexOf(challange_end);
            challange = tmp_text.substring(0, pos_end);
            // logger.debug("Got challange " + challange);
        }

        return challange;
    }

    private String findField(String text, String challange_start, int challange_length) {
        String challange = null;
        String tmp_text;

        int pos = text.lastIndexOf(challange_start) + challange_start.length();
        tmp_text = text.substring(pos);
        if (pos != -1) {
            challange = tmp_text.substring(0, challange_length);
            // logger.debug("Got challange " + challange);
        }

        return challange;
    }

    boolean login(String username, String password) {
        boolean loginResponse = false;

        // logger.debug("Try login");
        try {
            uri.removeQuery();
            uri.setPath("/login2.htm");
            HttpResponse httpResponse = makeHttpGetRequest();
            // logger.debug("Get response");
            uri.removeQuery();
            String response = IOUtils.toString(httpResponse.getEntity().getContent());
            // logger.debug("My response\n" + response);

            String challange_field = "name=\"Challenge\" value=\"";
            String challange = findField(response, challange_field, 4);

            if (challange != null) {
                uri.setPath("/cgi/login.cgi").addParameter("Username", username).addParameter("Password", password)
                        .addParameter("Challange", challange);
                httpResponse = makeHttpGetRequest();
                response = IOUtils.toString(httpResponse.getEntity().getContent());
                // logger.debug("Got response");
                loginResponse = !response.contains("PicachuCam");
                if (loginResponse) {
                    // logger.debug("Logged in successfully");
                } else
                    throw new IllegalStateException();
            } else {
                // logger.debug("Login exception");
                throw new IllegalStateException();
            }
        } catch (IOException e) {
            // logger.debug("Login exception");
            throw new IllegalStateException();
        } catch (Exception ex) {
            throw new IllegalStateException();
        }

        // logger.debug("Done " + loginResponse);

        return loginResponse;
    }

}
