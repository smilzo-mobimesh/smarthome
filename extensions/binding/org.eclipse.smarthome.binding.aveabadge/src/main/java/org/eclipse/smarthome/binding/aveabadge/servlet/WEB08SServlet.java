package org.eclipse.smarthome.binding.aveabadge.servlet;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.eclipse.smarthome.binding.aveabadge.handler.AveaBadgeHandler;
import org.eclipse.smarthome.binding.aveabadge.internal.AveaBadgeHandlerFactory;
import org.eclipse.smarthome.binding.aveabadge.internal.AveaWebMsg;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WEB08SServlet implements Servlet {

    /** the root path of this web application */
    public final static String SERVLET_NAME = "/avea.php";

    private final static Logger logger = LoggerFactory.getLogger(WEB08SServlet.class);

    protected HttpService httpService;
    protected ItemRegistry itemRegistry;

    private static Map<InetAddress, AveaBadgeHandler> readerList;

    public void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    public void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    public void unsetHttpService(HttpService httpService) {
        this.httpService = null;
    }

    /**
     * Creates a {@link HttpContext}
     *
     * @return a {@link HttpContext}
     */
    protected HttpContext createHttpContext() {
        HttpContext defaultHttpContext = httpService.createDefaultHttpContext();
        return defaultHttpContext;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        readerList = new HashMap();
    }

    protected void activate() {
        try {
            logger.debug("Starting up CMD servlet at " + SERVLET_NAME);

            Hashtable<String, String> props = new Hashtable<String, String>();
            httpService.registerServlet(SERVLET_NAME, this, props, createHttpContext());

        } catch (NamespaceException e) {
            logger.error("Error during servlet startup", e);
        } catch (ServletException e) {
            logger.error("Error during servlet startup", e);
        }
    }

    protected void deactivate() {
        httpService.unregister(SERVLET_NAME);
    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public String getServletInfo() {
        return null;
    }

    @Override
    public void destroy() {
        readerList = null;
    }

    public static void addReader(InetAddress address, AveaBadgeHandler reader) {
        readerList.put(address, reader);
    }

    public static void removeReader(InetAddress address) {
        readerList.remove(address);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        logger.debug("Avea.php requested");
        try {
            AveaWebMsg requestMessage = new AveaWebMsg(req.getParameterMap());
            AveaWebMsg responseMessage;

            // Cerco se è stato configurato il reader corrispondente sulla base dell'ip
            AveaBadgeHandler reader = readerList.get(requestMessage.getIP());
            if (reader != null)
                responseMessage = reader.messagereceived(requestMessage);
            else
                responseMessage = AveaBadgeHandlerFactory.defaultResponse(requestMessage);

            res.setContentType("application/xml;charset=UTF-8");
            res.getWriter().append("<html>");
            res.getWriter().append("<body>");
            res.getWriter().append("<AVEA>");

            res.getWriter().append(responseMessage.getResponse());

            // eventPublisher.post(ItemEventFactory.createStateEvent(itemName, state, source));
            // postCommand("mattuino:door:front", OnOffType.ON);

            res.getWriter().append("</AVEA>");
            res.getWriter().append("</body>");
            res.getWriter().append("</html>");

            res.getWriter().close();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
