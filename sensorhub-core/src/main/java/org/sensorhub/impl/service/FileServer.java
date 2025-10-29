package org.sensorhub.impl.service;

import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.sensorhub.api.common.SensorHubException;
import org.vast.util.Asserts;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class FileServer extends AbstractHttpServiceModule<FileServerConfig> {

    @Override
    protected void doStart() throws SensorHubException {
        super.doStart();

        HttpServer server = (HttpServer) httpServer;

        Asserts.checkNotNull(config.staticDocsRootUrl);
        Asserts.checkNotNull(config.staticDocsRootDir);

        ResourceHandler fileResourceHandler = new ResourceHandler();
        fileResourceHandler.setEtags(true);

        ContextHandler fileResourceContext = new ContextHandler();
        fileResourceContext.setContextPath(config.staticDocsRootUrl);
        fileResourceContext.setHandler(fileResourceHandler);
        fileResourceContext.setResourceBase(config.staticDocsRootDir);

        HandlerList handlers = (HandlerList) server.getJettyServer().getHandler();

        Handler permissionsHandler; // TODO: Handle permissions for file server

        if (config.securityConfig.requireAuth && server.jettySecurityHandler != null) {
            ConstraintSecurityHandler fileSecurityHandler = new ConstraintSecurityHandler();
            fileSecurityHandler.setAuthenticator(server.jettySecurityHandler.getAuthenticator());
            fileSecurityHandler.setLoginService(server.jettySecurityHandler.getLoginService());
            fileSecurityHandler.setHandler(fileResourceContext);
            server.addServletSecurity(fileSecurityHandler, config.staticDocsRootUrl, config.securityConfig.requireAuth, Constraint.ANY_AUTH);
            handlers.addHandler(fileSecurityHandler);
        } else {
            handlers.addHandler(fileResourceContext);
        }

        getLogger().info("Static resources being served at {} from {}", config.staticDocsRootUrl, config.staticDocsRootDir);
    }

}
