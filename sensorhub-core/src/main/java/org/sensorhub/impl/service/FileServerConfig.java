package org.sensorhub.impl.service;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.security.SecurityConfig;
import org.sensorhub.api.service.ServiceConfig;

public class FileServerConfig extends ServiceConfig {

    @DisplayInfo(desc="Root URL where static web content will be served.")
    public String staticDocsRootUrl = "/";

    @DisplayInfo(desc="Directory where static web content is located.")
    public String staticDocsRootDir = "web";

    @DisplayInfo(desc="Security related options")
    public SecurityConfig securityConfig = new SecurityConfig();

}
