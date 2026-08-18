/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2024 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.consys.resource;

import java.io.IOException;
import java.util.Map;
import org.sensorhub.impl.service.consys.BaseHandler;
import org.sensorhub.impl.service.consys.InvalidRequestException;
import org.sensorhub.impl.service.consys.ServiceErrors;
import com.google.common.io.ByteStreams;


public class JarStaticResourceHandler extends BaseHandler
{
    public static final String[] NAMES = { "static" };
    static final Map<String, String> MIME_TYPES = Map.of(
        "css", "text/css",
        "js", "application/javascript",
        "woff", "font/woff",
        "woff2", "font/woff2"
    );
    
    
    @Override
    public String[] getNames()
    {
        return NAMES;
    }


    @Override
    public void doGet(RequestContext ctx) throws InvalidRequestException, IOException, SecurityException
    {
        var path = ctx.getRequestPath();
                
        path = path.replaceFirst("/static", "");
        var is = getClass().getResourceAsStream(path);
        if (is == null)
            throw ServiceErrors.notFound(path);
        
        var mimeType = getMimeType(path);
        if (mimeType != null)
            ctx.setResponseContentType(mimeType);
        ctx.setResponseHeader("Cache-Control", "public, max-age=2592000;");
        ByteStreams.copy(is, ctx.getOutputStream());
    }


    protected String getMimeType(String path)
    {
        var extStart = path.lastIndexOf('.');
        if (extStart < 0 || extStart == path.length()-1)
            return null;

        return MIME_TYPES.get(path.substring(extStart+1).toLowerCase());
    }


    @Override
    public void doPost(RequestContext ctx) throws InvalidRequestException, IOException, SecurityException
    {
        ServiceErrors.unsupportedOperation("");
    }


    @Override
    public void doPut(RequestContext ctx) throws InvalidRequestException, IOException, SecurityException
    {
        ServiceErrors.unsupportedOperation("");
    }


    @Override
    public void doDelete(RequestContext ctx) throws InvalidRequestException, IOException, SecurityException
    {
        ServiceErrors.unsupportedOperation("");
    }

}
