package org.eclipse.smarthome.binding.aveabadge.internal;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AveaWebMsg {
    // Log

    private static final Logger logger = LoggerFactory.getLogger(AveaWebMsg.class);

    private final static int MIN_HEARTBEAT_VALUE = 0;
    private final static int MAX_HEARTBEAT_VALUE = 9999;
    private final static int DEFAULT_HEARTBEAT_VALUE = MIN_HEARTBEAT_VALUE;

    public static enum CMD {
        PU,
        PG,
        CO,
        HB,
        SW
    };

    public static enum BEEP {

        SHORT,
        LONG
    };

    // Request
    private CMD cmd;
    private InetAddress ip;
    private String sid;
    private String type;
    private String mode;
    private String rev;
    private String ver;
    private String date;
    private String time;
    private String devide_id;
    private boolean dhcp;
    private int ulen;
    private String uid;
    private Long code;

    // Response
    private int beep;
    private int heartbeat = DEFAULT_HEARTBEAT_VALUE;

    public AveaWebMsg(Map<String, String[]> params) throws Exception {
        this.beep = -1;
        String[] tmp;

        tmp = params.get("cmd");
        if (tmp != null) {
            String cmdstr = tmp[0];

            if (cmdstr == null) {
                logger.warn("No command received");
                throw new Exception("No command");
            }
            cmd = CMD.valueOf(cmdstr);
            logger.debug("Command " + cmd.toString());
        }

        tmp = params.get("id");
        if (tmp != null) {
            String ipstr = tmp[0];
            if (ipstr == null) {
                logger.error("No id received");
                throw new Exception("No id");
            }

            // id: IP del badge
            ip = InetAddress.getByName(ipstr);
            logger.debug("Badge IP:{}", ip.getHostAddress());
        }

        tmp = params.get("sid");
        if (tmp != null) {
            // sid: va configurato
            sid = tmp[0];
            if (sid == null) {
                logger.warn("No sid received");
            } else
                logger.debug("Sid received {}", sid);
        }

        tmp = params.get("type");
        if (tmp != null) {
            // type: non ci interessa nella conf base
            type = tmp[0];
            if (type == null) {
                logger.warn("No type received");
            }
        }

        tmp = params.get("mode");
        if (tmp != null) {
            // mode: internal use
            mode = tmp[0];
        }

        tmp = params.get("ver");
        if (tmp != null) {
            // mode: internal use
            ver = tmp[0];
        }

        tmp = params.get("ulen");
        if (tmp != null) {
            // code
            String ulenstr = tmp[0];
            if (ulenstr == null) {
                logger.warn("Void ulen received");
            } else {
                logger.debug("Ulen received: {}", ulenstr);
                ulen = Integer.parseInt(ulenstr);
            }
        }

        tmp = params.get("uid");
        if (tmp != null) {
            // code
            String uid = tmp[0];
            if (uid == null) {
                logger.warn("Void uid received");
            } else {
                logger.debug("Uid received: {}", uid);
            }
        }

        tmp = params.get("code");
        if (tmp != null) {
            // code
            String codestr = tmp[0];
            if (codestr == null) {
                logger.warn("No code received");
                if (cmd == CMD.CO)
                    throw new Exception("No card code");
            } else {
                logger.debug("Code received: {}", codestr);
                code = Long.parseLong(codestr);
            }
        }

        tmp = params.get("deviceid");
        if (tmp != null) {
            // code
            String device_id = tmp[0];
            if (device_id == null)
                logger.warn("No device id received");
            else
                logger.debug("Deviceid received: {}", device_id);
        }
    }

    public String getSid() {
        return sid;
    }

    public InetAddress getIP() {
        return ip;
    }

    public CMD getCmd() {
        return cmd;
    }

    public Long getCode() {
        return code;
    }

    public void setBeep(BEEP b) {
        if (b == BEEP.SHORT) {
            beep = 1;
        } else {
            beep = 0;
        }
    }

    public void setHeartBeat(int heartbeat) {
        if (heartbeat >= MIN_HEARTBEAT_VALUE && heartbeat <= MAX_HEARTBEAT_VALUE)
            this.heartbeat = heartbeat;
    }

    public StringBuilder getResponse() {
        StringBuilder res = new StringBuilder();

        if (cmd == CMD.PU || cmd == CMD.HB) {
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date now = new Date();
            String strDate = sdfDate.format(now);
            res.append("CK=" + strDate);
        }

        if (cmd == CMD.PU && heartbeat >= 0) {
            res.append("HB=" + String.format("%04d", heartbeat));
        }

        if (beep > -1) {
            res.append("BEEP=" + beep);
        }

        return res;
    }

    /*
     * switch (cmd) {
     * //Power Up
     * case "PU":
     * logger.log(Level.INFO, "Power up command");
     *
     * //Save info
     *
     * //mode: internal use
     * String mode = "";
     * if (params.containsKey("mode")) {
     * mode = params.get("mode");
     * }
     *
     * //ver: fw version
     * String ver;
     * if (params.containsKey("ver")) {
     * ver = params.get("ver");
     * }
     *
     * //rev: non usato
     * //sw: non usato
     * //Risposta
     * out.print("CK=" + getCurrentTime());
     * out.print("HB=0030");
     * if (mode.equals("ID2")) {
     * out.print("DHCP=1");
     * }
     * //out.println("BEEP=0");
     *
     * break;
     * case "PG":
     * logger.log(Level.INFO, "Being pinged");
     * //out.print("HB=0010");
     * break;
     * case "CO":
     * logger.log(Level.INFO, "Card only");
     *
     * Long code;
     * if (params.containsKey("code")) {
     * code = Long.parseLong(params.get("code"));
     * logger.log(Level.INFO, "Card number " + code);
     *
     * out.print("HB=0001");
     * out.print("BEEP=1");
     *
     * try {
     * logger.log(Level.INFO, "Avea listener called");
     * BadgeCommService.badged();
     * } catch (Exception ex) {
     * logger.log(Level.SEVERE, "Failed to open door");
     * logger.log(Level.FINER, ex.getStackTrace().toString());
     * resp.getWriter().append("error");
     * }
     * }
     * break;
     * case "HB":
     * logger.log(Level.INFO, "Heart beat");
     * out.print("CK=" + getCurrentTime());
     * out.print("HB=0300");
     * break;
     * case "SW":
     * logger.log(Level.INFO, "Switched");
     * break;
     * }
     * }
     *
     * }
     *
     * }
     */

}
