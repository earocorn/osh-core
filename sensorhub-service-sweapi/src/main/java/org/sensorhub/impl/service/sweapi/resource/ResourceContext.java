/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sweapi.resource;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.sensorhub.impl.service.WebSocketOutputStream;
import org.sensorhub.impl.service.sweapi.SWEApiSecurity;
import org.sensorhub.impl.service.sweapi.SWEApiServlet;
import org.slf4j.Logger;
import org.vast.util.Asserts;
import com.google.common.base.Strings;
import com.google.gson.stream.JsonWriter;


public class ResourceContext
{
    SWEApiServlet servlet;
    HttpServletRequest req;
    HttpServletResponse resp;
    Deque<String> path;
    ResourceRef parentResource = new ResourceRef();
    ResourceFormat format;
    PropertyFilter propFilter;
    WebSocketServletFactory wsFactory;
    WebSocketOutputStream wsOutputStream;
    
    
    /*
     * Auxiliary data generated during the request handling process for
     * consumption by later processing stages
     */
    Object data;
    
    
    public static class ResourceRef
    {
        @SuppressWarnings("rawtypes")
        public BaseResourceHandler type;
        public long internalID;
        public long version;
    }
    
    
    public ResourceContext(SWEApiServlet servlet, HttpServletRequest req, HttpServletResponse resp)
    {
        this.servlet = Asserts.checkNotNull(servlet, Servlet.class);
        this.req = Asserts.checkNotNull(req, HttpServletRequest.class);
        this.resp = Asserts.checkNotNull(resp, HttpServletResponse.class);
        
        String[] pathElts = req.getPathInfo().split("/");
        this.path = new ArrayDeque<>(pathElts.length);
        for (String elt: pathElts)
        {
            if (!Strings.isNullOrEmpty(elt))
                path.addLast(elt);
        }
    }
    
    
    public ResourceContext(SWEApiServlet servlet, HttpServletRequest req, HttpServletResponse resp, WebSocketServletFactory wsFactory)
    {
        this(servlet, req, resp);
        this.wsFactory = Asserts.checkNotNull(wsFactory, WebSocketServletFactory.class);
    }
    
    
    public boolean isEmpty()
    {
        return path.isEmpty();
    }
    
    
    public String popNextPathElt()
    {
        if (path.isEmpty())
            return null;
        return path.pollFirst();
    }
    
    
    public HttpServletRequest getRequest()
    {
        return req;
    }
    
    
    public HttpServletResponse getResponse()
    {
        return resp;
    }
    
    
    public long getParentID()
    {
        return parentResource.internalID;
    }
    
    
    public ResourceRef getParentRef()
    {
        return parentResource;
    }
    
    
    public void setParent(@SuppressWarnings("rawtypes") BaseResourceHandler parentHandler, long internalID)
    {
        parentResource.type = parentHandler;
        parentResource.internalID = internalID;
    }
    
    
    public void setParent(@SuppressWarnings("rawtypes") BaseResourceHandler parentHandler, long internalID, long version)
    {
        parentResource.type = parentHandler;
        parentResource.internalID = internalID;
        parentResource.version = internalID;
    }
    
    
    public void setFormatOptions(ResourceFormat format, PropertyFilter propFilter)
    {
        this.format = Asserts.checkNotNull(format, ResourceFormat.class);
        this.propFilter = propFilter;
    }
    
    
    public ResourceFormat getFormat()
    {
        return format;
    }


    public PropertyFilter getPropertyFilter()
    {
        return propFilter;
    }


    public Object getData()
    {
        return data;
    }
    
    
    public void setData(Object data)
    {
        this.data = data;
    }
    
    
    public boolean sendError(int code)
    {
        return sendError(code, null);
    }
    
    
    public boolean sendError(int code, String msg)
    {
        servlet.sendError(code, msg, resp);
        return false;
    }
    
    
    public boolean sendSuccess(int code)
    {
        return sendSuccess(code, null);
    }
    
    
    public boolean sendSuccess(int code, String msg)
    {
        try
        {
            resp.setStatus(code);
            if (msg != null)
                resp.getOutputStream().write(msg.getBytes());
        }
        catch (IOException e)
        {
            getLogger().error("Could not send response", e);
        }
        
        return true;
    }
    
    
    protected void writeError(String msg, JsonWriter writer) throws IOException
    {
        writer.beginObject();
        writer.name("error").value(msg);
        writer.endObject();
    }
    
    
    public Logger getLogger()
    {
        return servlet.getLogger();
    }
    
    
    public String getApiRootURL()
    {
        return servlet.getConfig().getPublicEndpoint();
    }
    
    
    public SWEApiSecurity getSecurityHandler()
    {
        return servlet.getSecurityHandler();
    }
    
    
    public boolean isWebSocket()
    {
        return wsFactory != null;
    }


    public WebSocketServletFactory getWebsocketFactory()
    {
        return wsFactory;
    }
    
    
    public void setWebsocketOutputStream(WebSocketOutputStream wsOutputStream)
    {
        this.wsOutputStream = wsOutputStream;
    }
    

    public WebSocketOutputStream getWebsocketOutputStream()
    {
        return wsOutputStream;
    }
}