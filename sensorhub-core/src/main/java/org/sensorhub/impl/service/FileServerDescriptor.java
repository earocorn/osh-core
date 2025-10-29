package org.sensorhub.impl.service;

import org.sensorhub.api.module.IModuleBase;
import org.sensorhub.api.module.ModuleConfigBase;
import org.sensorhub.impl.module.JarModuleProvider;

public class FileServerDescriptor extends JarModuleProvider {

    @Override
    public String getModuleName() {
        return "File Server";
    }

    @Override
    public String getModuleDescription() {
        return "File server module providing permissions and authentication for access control";
    }

    @Override
    public String getProviderName() {
        return "Botts Innovative Research, Inc.";
    }

    @Override
    public String getModuleVersion() {
        return "1.0.0";
    }

    @Override
    public Class<? extends IModuleBase<?>> getModuleClass() {
        return FileServer.class;
    }

    @Override
    public Class<? extends ModuleConfigBase> getModuleConfigClass() {
        return FileServerConfig.class;
    }

}
