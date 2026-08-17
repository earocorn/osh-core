/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2026 Botts Innovative Research, Inc. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.consys;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.Test;
import org.sensorhub.impl.service.consys.resource.ResourceFormat;


public class TestConSysHtmlDisabled extends AbstractTestApiBase
{
    @Override
    protected void configureService(ConSysApiServiceConfig config)
    {
        config.allowHtmlResponses = false;
    }
    
    
    @Test
    public void testBrowserHtmlRequestFallsBackToJson() throws Exception
    {
        var resp = sendGetRequestWithAccept("", ResourceFormat.HTML.getMimeType());
        
        checkStatusCode(resp, 200);
        assertTrue(resp.headers().firstValue("Content-Type").orElse("").contains(ResourceFormat.JSON.getMimeType()));
        assertFalse(resp.body().contains("<html"));
    }
    
    
    @Test
    public void testExplicitHtmlRequestRejected() throws Exception
    {
        var resp = sendGetRequestWithAccept("collections?f=html", ResourceFormat.HTML.getMimeType());
        
        checkStatusCode(resp, 400);
    }
    
    
    @Test
    public void testHtmlAlternatesNotAdvertised() throws Exception
    {
        var resp = sendGetRequestWithAccept("collections", ResourceFormat.JSON.getMimeType());
        
        checkStatusCode(resp, 200);
        assertFalse(resp.body().contains("This document as HTML"));
        assertFalse(resp.body().contains("\"type\":\"" + ResourceFormat.HTML.getMimeType() + "\""));
    }
    
    
    protected HttpResponse<String> sendGetRequestWithAccept(String path, String accept) throws IOException
    {
        try
        {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(concat(apiRootUrl, path)))
                .header("Accept", accept)
                .build();
            
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
        catch (InterruptedException e)
        {
            throw new IOException(e);
        }
    }
}
