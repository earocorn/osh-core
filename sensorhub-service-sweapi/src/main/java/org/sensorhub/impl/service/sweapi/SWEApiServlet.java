/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sweapi;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.sensorhub.api.datastore.feature.IFeatureStore;
import org.sensorhub.api.datastore.system.ISystemDescStore;
import org.sensorhub.api.security.ISecurityManager;
import org.sensorhub.impl.service.sweapi.resource.IResourceHandler;
import org.sensorhub.impl.service.sweapi.resource.RequestContext;
import org.sensorhub.impl.service.sweapi.stream.WebSocketOut;
import org.slf4j.Logger;
import com.google.gson.stream.JsonWriter;
import static javax.servlet.http.HttpServletResponse.*;


@SuppressWarnings("serial")
public class SWEApiServlet extends RestApiServlet
{
    static final String LOG_REQUEST_MSG = "{} {}{} (from ip={}, user={})";
    static final String INTERNAL_ERROR_MSG = "Internal server error";
    static final String INTERNAL_ERROR_LOG_MSG = INTERNAL_ERROR_MSG + " while processing request " + LOG_REQUEST_MSG;
    static final String ACCESS_DENIED_ERROR_MSG = "Permission denied";
    static final String JSON_CONTENT_TYPE = "application/json";

    protected final SWEApiSecurity securityHandler;
    protected ISystemDescStore systems;
    protected IFeatureStore features;
    protected IResourceHandler rootHandler;
    protected ScheduledExecutorService threadPool;
    protected WebSocketServletFactory wsFactory;
    protected Logger log;
    protected String rootUrl;
    

    public SWEApiServlet(SWEApiService service, SWEApiSecurity securityHandler, RootHandler rootHandler, Logger logger)
    {
        this.threadPool = service.getThreadPool();
        this.securityHandler = securityHandler;
        this.rootHandler = rootHandler;
        this.log = logger;
        
        var endPointUrl = service.getHttpServer().getPublicEndpointUrl(service.getConfiguration().endPoint);
        this.rootUrl = endPointUrl.endsWith("/") ? endPointUrl.substring(0, endPointUrl.length()-1) : endPointUrl;
    }


    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);

        // create websocket factory
        try
        {
            WebSocketPolicy wsPolicy = new WebSocketPolicy(WebSocketBehavior.SERVER);
            wsFactory = new WebSocketServerFactory(getServletContext(), wsPolicy);
            wsFactory.start();
        }
        catch (Exception e)
        {
            throw new ServletException("Cannot initialize websocket factory", e);
        }
    }


    @Override
    public void destroy()
    {
        // destroy websocket factory
        try
        {
            if (wsFactory != null)
                wsFactory.stop();
        }
        catch (Exception e)
        {
            log.error("Cannot stop websocket factory", e);
        }
    }
    
    
    protected void setCurrentUser(HttpServletRequest req)
    {
        String userID = ISecurityManager.ANONYMOUS_USER;
        if (req.getRemoteUser() != null)
            userID = req.getRemoteUser();
        securityHandler.setCurrentUser(userID);
    }
    
    
    protected void clearCurrentUser()
    {
        securityHandler.clearCurrentUser();
    }
    
    
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        logRequest(req);
        super.service(req, resp);
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        // handle request asynchronously
        try
        {
            resp.setContentType(JSON_CONTENT_TYPE);
            var ctx = createContext(req, resp);
            
            final AsyncContext aCtx = req.startAsync(req, resp);
            CompletableFuture.runAsync(() -> {
                try
                {
                    setCurrentUser(req);
                    rootHandler.doGet(ctx);
                }
                catch (InvalidRequestException e)
                {
                    handleInvalidRequestException(req, resp, e);
                }
                catch (SecurityException e)
                {
                    handleAuthException(req, resp, e);
                }
                catch (Throwable e)
                {
                    logError(req, e);
                    sendError(SC_INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MSG, resp);
                }
                finally
                {
                    clearCurrentUser();
                    aCtx.complete();
                }
            }, threadPool);
        }
        catch (Exception e)
        {
            logError(req, e);
            sendError(SC_INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MSG, resp);
        }
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        // handle request asynchronously
        try
        {
            resp.setContentType(JSON_CONTENT_TYPE);
            var ctx = createContext(req, resp);
            
            final AsyncContext aCtx = req.startAsync(req, resp);
            CompletableFuture.runAsync(() -> {
                try
                {
                    setCurrentUser(req);
                    rootHandler.doPost(ctx);
                    
                    var uriList = ctx.getResourceUris();
                    if (uriList.size() == 1)
                    {
                        resp.setStatus(SC_CREATED);
                        resp.setHeader("Location", uriList.iterator().next());
                    }
                    else if (!uriList.isEmpty())
                    {
                        // prepare to write JSON response
                        JsonWriter writer = new JsonWriter(new OutputStreamWriter(resp.getOutputStream()));
                        writer.beginArray();
                        for (var uri: uriList)
                            writer.value(uri);
                        writer.endArray();
                        writer.flush();
                    }
                }
                catch (InvalidRequestException e)
                {
                    handleInvalidRequestException(req, resp, e);
                }
                catch (SecurityException e)
                {
                    handleAuthException(req, resp, e);
                }
                catch (Throwable e)
                {
                    logError(req, e);
                    sendError(SC_INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MSG, resp);
                }
                finally
                {
                    clearCurrentUser();
                    aCtx.complete();
                }
            }, threadPool);
        }
        catch (Exception e)
        {
            logError(req, e);
            sendError(SC_INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MSG, resp);
        }
    }


    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        // handle request asynchronously
        try
        {
            resp.setContentType(JSON_CONTENT_TYPE);
            var ctx = createContext(req, resp);
            
            final AsyncContext aCtx = req.startAsync(req, resp);
            CompletableFuture.runAsync(() -> {
                try
                {
                    setCurrentUser(req);
                    rootHandler.doPut(ctx);
                    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                }
                catch (InvalidRequestException e)
                {
                    handleInvalidRequestException(req, resp, e);
                }
                catch (SecurityException e)
                {
                    handleAuthException(req, resp, e);
                }
                catch (Throwable e)
                {
                    logError(req, e);
                    sendError(SC_INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MSG, resp);
                }
                finally
                {
                    clearCurrentUser();
                    aCtx.complete();
                }
            }, threadPool);
        }
        catch (Exception e)
        {
            logError(req, e);
            sendError(SC_INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MSG, resp);
        }
    }


    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        // handle request asynchronously
        try
        {
            resp.setContentType(JSON_CONTENT_TYPE);
            var ctx = createContext(req, resp);
            
            final AsyncContext aCtx = req.startAsync(req, resp);
            CompletableFuture.runAsync(() -> {
                try
                {
                    setCurrentUser(req);
                    rootHandler.doDelete(ctx);
                    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                }
                catch (InvalidRequestException e)
                {
                    handleInvalidRequestException(req, resp, e);
                }
                catch (SecurityException e)
                {
                    handleAuthException(req, resp, e);
                }
                catch (Throwable e)
                {
                    logError(req, e);
                    sendError(SC_INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MSG, resp);
                }
                finally
                {
                    clearCurrentUser();
                    aCtx.complete();
                }
            }, threadPool);
        }
        catch (Exception e)
        {
            logError(req, e);
            sendError(SC_INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MSG, resp);
        }
    }
    
    
    protected RequestContext createContext(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {
        // check if we have an upgrade request for websockets
        if (wsFactory.isUpgradeRequest(req, resp))
        {
            /*if (req.getSubProtocols().contains("ingest"))
            {
                // get binding for parsing incoming obs records
                var binding = getBinding(ctx, true);
                return null;
            }
            else
            }*/
            var ws = new WebSocketOut(getLogger());
            
            wsFactory.acceptWebSocket(new WebSocketCreator() {
                @Override
                public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp)
                {
                    return ws;
                }
            }, req, resp);
            
            return new RequestContext(this, req, resp, ws);
        }
        
        // else just handle as regular HTTP request
        else
            return new RequestContext(this, req, resp);
    }
    
    
    protected void logRequest(HttpServletRequest req)
    {
        if (log.isInfoEnabled())
            logRequestInfo(req, null);
    }
    
    
    protected void logError(HttpServletRequest req, Throwable e)
    {
        if (log.isErrorEnabled())
            logRequestInfo(req, e);
    }
    
    
    protected void logRequestInfo(HttpServletRequest req, Throwable error)
    {
        String method = req.getMethod();
        String url = req.getRequestURI();
        String ip = req.getRemoteAddr();
        String user = req.getRemoteUser() != null ? req.getRemoteUser() : "anonymous";
        
        // if proxy header present, use source ip instead of proxy ip
        String proxyHeader = req.getHeader("X-Forwarded-For");
        if (proxyHeader != null)
        {
            String[] ips = proxyHeader.split(",");
            if (ips.length >= 1)
                ip = ips[0];
        }
        
        // detect websocket upgrade
        if ("websocket".equalsIgnoreCase(req.getHeader("Upgrade")))
            method += "/Websocket";
        
        // append decoded request if any
        String query = "";
        if (req.getQueryString() != null)
            query = "?" + req.getQueryString();
        
        if (error != null)
            log.error(INTERNAL_ERROR_LOG_MSG, method, url, query, ip, user, error);
        else
            log.info(LOG_REQUEST_MSG, method, url, query, ip, user);
    }
    
    
    public void sendError(int code, HttpServletResponse resp)
    {
        sendError(code, null, resp);
    }
    
    
    public void sendError(int code, String msg, HttpServletResponse resp)
    {
        try
        {
            resp.sendError(code, msg);
            /*resp.setStatus(code);
            
            if (msg != null)
            {
                msg = "{ \"error\": \"" + msg + "\" }";
                resp.getOutputStream().write(msg.getBytes());
            }*/
        }
        catch (IOException e)
        {
            log.error("Could not send error response", e);
        }
    }
    
    
    protected void handleInvalidRequestException(HttpServletRequest req, HttpServletResponse resp, InvalidRequestException e)
    {
        log.debug("Invalid request ({}): {}", e.getErrorCode(), e.getMessage());
        
        switch (e.getErrorCode())
        {
            case UNSUPPORTED_OPERATION:
                sendError(SC_METHOD_NOT_ALLOWED, e.getMessage(), resp);
                break;
                
            case BAD_REQUEST:
            case BAD_PAYLOAD:
                sendError(SC_BAD_REQUEST, e.getMessage(), resp);
                break;
                
            case NOT_FOUND:
                sendError(SC_NOT_FOUND, e.getMessage(), resp);
                break;
                
            case FORBIDDEN:
                sendError(SC_FORBIDDEN, e.getMessage(), resp);
                break;
                
            default:
                sendError(SC_INTERNAL_SERVER_ERROR, INTERNAL_ERROR_MSG, resp);
        }
    }
    
    
    protected void handleAuthException(HttpServletRequest req, HttpServletResponse resp, SecurityException e)
    {
        try
        {
            log.debug("Not authorized: {}", e.getMessage());
            
            if (req != null && resp != null)
            {
                if (req.getRemoteUser() == null)
                    req.authenticate(resp);
                else
                    sendError(SC_FORBIDDEN, ACCESS_DENIED_ERROR_MSG, resp);
            }
        }
        catch (Exception e1)
        {
            getLogger().error("Could not send authentication request", e1);
        }
    }
    
    
    public Logger getLogger()
    {
        return this.log;
    }


    public String getApiRootURL(HttpServletRequest req)
    {
        if (rootUrl.contains("localhost") && req != null)
            return rootUrl.replace("localhost", req.getServerName());
        
        return rootUrl;
    }
    
    
    public IResourceHandler getRootHandler()
    {
        return rootHandler;
    }


    public SWEApiSecurity getSecurityHandler()
    {
        return securityHandler;
    }

}
