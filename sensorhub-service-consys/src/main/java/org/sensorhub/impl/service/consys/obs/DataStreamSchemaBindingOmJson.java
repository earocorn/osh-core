/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.consys.obs;

import static org.sensorhub.impl.service.consys.SWECommonUtils.OM_COMPONENTS_FILTER;
import java.io.IOException;
import java.util.Collection;
import javax.xml.stream.XMLStreamException;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.system.SystemId;
import org.sensorhub.impl.service.consys.ResourceParseException;
import org.sensorhub.impl.service.consys.SWECommonUtils;
import org.sensorhub.impl.service.consys.resource.RequestContext;
import org.sensorhub.impl.service.consys.resource.ResourceBindingJson;
import org.sensorhub.impl.service.consys.resource.ResourceFormat;
import org.sensorhub.impl.service.consys.resource.ResourceLink;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.SWEStaxBindings;
import org.vast.swe.json.SWEJsonStreamReader;
import org.vast.swe.json.SWEJsonStreamWriter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;


public class DataStreamSchemaBindingOmJson extends ResourceBindingJson<DataStreamKey, IDataStreamInfo>
{
    String rootURL;
    SWEStaxBindings sweBindings;
    SWEJsonStreamReader sweReader;
    SWEJsonStreamWriter sweWriter;
    
    
    public DataStreamSchemaBindingOmJson(RequestContext ctx, IdEncoders idEncoders, boolean forReading) throws IOException
    {
        super(ctx, idEncoders, forReading);
        init(ctx, forReading);
    }
    
    
    public DataStreamSchemaBindingOmJson(RequestContext ctx, IdEncoders idEncoders, JsonReader reader) throws IOException
    {
        super(ctx, idEncoders, reader);
        init(ctx, true);
    }
    
    
    void init(RequestContext ctx, boolean forReading)
    {
        this.rootURL = ctx.getApiRootURL();
        this.sweBindings = new SWEStaxBindings();
        
        if (forReading)
            this.sweReader = new SWEJsonStreamReader(reader);
        else
            this.sweWriter = new SWEJsonStreamWriter(writer);
    }
    
    
    @Override
    public IDataStreamInfo deserialize(JsonReader reader) throws IOException
    {
        DataComponent resultStruct = null;
        
        try
        {
            // read BEGIN_OBJECT only if not already read by caller
            // this happens when reading embedded schema and auto-detecting obs format
            if (reader.peek() == JsonToken.BEGIN_OBJECT)
                reader.beginObject();
            
            while (reader.hasNext())
            {
                var prop = reader.nextName();
                
                if ("resultSchema".equals(prop))
                {
                    sweReader.nextTag();
                    resultStruct = sweBindings.readDataComponent(sweReader);
                    resultStruct.setName(SWECommonUtils.NO_NAME);
                }
                else
                    reader.skipValue();
            }
            reader.endObject();
        }
        catch (XMLStreamException e)
        {
            throw new ResourceParseException(INVALID_JSON_ERROR_MSG + e.getMessage());
        }
        catch (IllegalStateException e)
        {
            throw new ResourceParseException(INVALID_JSON_ERROR_MSG + e.getMessage());
        }
        
        
        var dsInfo = new DataStreamInfo.Builder()
            .withName(SWECommonUtils.NO_NAME) // name will be set later
            .withSystem(SystemId.NO_SYSTEM_ID) // System ID will be set later
            .withRecordDescription(resultStruct)
            .withRecordEncoding(new TextEncodingImpl())
            .build();
        
        return dsInfo;
    }


    @Override
    public void serialize(DataStreamKey key, IDataStreamInfo dsInfo, boolean showLinks, JsonWriter writer) throws IOException
    {
        var dsId = idEncoders.getDataStreamIdEncoder().encodeID(key.getInternalID());
        
        writer.beginObject();
        writer.name("datastream@id").value(dsId);
        writer.name("obsFormat").value(ResourceFormat.OM_JSON.toString());
        
        // result structure 
        try
        {
            writer.name("resultSchema");
            sweWriter.resetContext();
            
            // hide time and FOI components if any
            var dataStruct = dsInfo.getRecordStructure().copy();
            if (dataStruct instanceof DataRecord)
            {
                var it = ((DataRecord)dataStruct).getFieldList().iterator();
                while (it.hasNext())
                {
                    if (!OM_COMPONENTS_FILTER.accept(it.next()))
                        it.remove();
                }
            }
            
            sweBindings.writeDataComponent(sweWriter, dataStruct, false);
        }
        catch (Exception e)
        {
            throw new IOException("Error writing O&M result structure", e);
        }
        
        writer.endObject();
        writer.flush();
    }


    @Override
    public void startCollection() throws IOException
    {
        startJsonCollection(writer);
    }


    @Override
    public void endCollection(Collection<ResourceLink> links) throws IOException
    {
        endJsonCollection(writer, links);
    }
}
