package org.sensorhub.impl.service;

import org.sensorhub.api.security.IPermission;
import org.sensorhub.impl.module.ModuleSecurity;

public class FileServerSecurity extends ModuleSecurity {

    public final IPermission get;

    public FileServerSecurity(FileServer fileServer, boolean enable) {
        super(fileServer, "fileserver", enable);

        get = rootPerm;

        fileServer.getParentHub().getSecurityManager().registerModulePermissions(rootPerm);
    }

    @Override
    protected boolean isAccessControlEnabled() {
        var httpServer = ((FileServer)module).getHttpServer();
        return super.isAccessControlEnabled() && httpServer.isAuthEnabled();
    }

}
