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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.impl.service.consys.resource.ResourceFormat;


public class TestConSysHtmlSecurity extends AbstractTestApiBase
{
    static final String XSS_SCRIPT = "<script>alert(1)</script>";
    static final String XSS_IMAGE = "<img src=x onerror=alert(1)>";
    
    TestSystems systemTests;
    TestDataStreams dataStreamTests;
    
    
    @Before
    public void setup() throws IOException, SensorHubException
    {
        super.setup();
        systemTests = new TestSystems();
        systemTests.apiRootUrl = apiRootUrl;
        dataStreamTests = new TestDataStreams();
        dataStreamTests.apiRootUrl = apiRootUrl;
    }
    
    
    @Test
    public void testSecurityHeadersPresent() throws Exception
    {
        var resp = sendGetRequestWithAccept("", ResourceFormat.JSON.getMimeType());
        
        checkStatusCode(resp, 200);
        assertEquals("nosniff", resp.headers().firstValue("X-Content-Type-Options").orElse(null));
        assertTrue(resp.headers().firstValue("Content-Security-Policy").orElse("").contains("frame-ancestors 'none'"));
        assertEquals("DENY", resp.headers().firstValue("X-Frame-Options").orElse(null));
    }
    
    
    @Test
    public void testDatastreamHtmlEscapesStoredNameAndDescription() throws Exception
    {
        var sysUrl = systemTests.addFeature(1, false);
        var ds = dataStreamTests.createDatastreamOmJson(XSS_SCRIPT, 1, 1);
        ds.addProperty("description", XSS_IMAGE);
        
        var url = getLocation(sendPostRequest(concat(sysUrl, DATASTREAM_COLLECTION), ds));
        var resp = sendGetRequestWithAccept(url + "?f=html", ResourceFormat.HTML.getMimeType());
        
        checkStatusCode(resp, 200);
        assertTrue(resp.headers().firstValue("Content-Type").orElse("").contains(ResourceFormat.HTML.getMimeType()));
        assertFalse(resp.body().contains(XSS_SCRIPT));
        assertFalse(resp.body().contains(XSS_IMAGE));
        assertTrue(resp.body().contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertTrue(resp.body().contains("&lt;img src=x onerror=alert(1)&gt;"));
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
