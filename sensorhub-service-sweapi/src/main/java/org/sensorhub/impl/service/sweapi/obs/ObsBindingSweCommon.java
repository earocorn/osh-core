/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sweapi.obs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.impl.service.sweapi.ServiceErrors;
import org.sensorhub.impl.service.sweapi.SWECommonUtils;
import org.sensorhub.impl.service.sweapi.obs.ObsHandler.ObsHandlerContextData;
import org.sensorhub.impl.service.sweapi.resource.PropertyFilter;
import org.sensorhub.impl.service.sweapi.resource.RequestContext;
import org.sensorhub.impl.service.sweapi.resource.ResourceFormat;
import org.sensorhub.impl.service.sweapi.resource.ResourceLink;
import org.sensorhub.utils.SWEDataUtils;
import org.sensorhub.impl.service.sweapi.resource.ResourceBinding;
import org.vast.cdm.common.DataStreamParser;
import org.vast.cdm.common.DataStreamWriter;
import org.vast.data.TextEncodingImpl;
import org.vast.data.XMLEncodingImpl;
import org.vast.swe.SWEHelper;
import org.vast.swe.ScalarIndexer;
import org.vast.swe.XmlDataParser;
import org.vast.swe.AsciiDataParser;
import org.vast.swe.fast.JsonDataParserGson;
import org.vast.swe.fast.JsonDataWriter;
import org.vast.swe.fast.TextDataWriter;
import org.vast.swe.fast.XmlDataWriter;
import net.opengis.swe.v20.BinaryEncoding;
import net.opengis.swe.v20.JSONEncoding;
import net.opengis.swe.v20.TextEncoding;
import net.opengis.swe.v20.XMLEncoding;


public class ObsBindingSweCommon extends ResourceBinding<BigId, IObsData>
{       
    ObsHandlerContextData contextData;
    DataStreamParser resultReader;
    DataStreamWriter resultWriter;
    ScalarIndexer timeStampIndexer;

    
    ObsBindingSweCommon(RequestContext ctx, IdEncoders idEncoders, boolean forReading, IObsStore obsStore) throws IOException
    {
        super(ctx, idEncoders);
        this.contextData = (ObsHandlerContextData)ctx.getData();
                
        var dsInfo = contextData.dsInfo;
        if (forReading)
        {
            var is = ctx.getInputStream();
            resultReader = getSweCommonParser(dsInfo, is, ctx.getFormat());
            timeStampIndexer = SWEHelper.getTimeStampIndexer(contextData.dsInfo.getRecordStructure());
        }
        else
        {
            var os = ctx.getOutputStream();
            resultWriter = getSweCommonWriter(dsInfo, os, ctx.getPropertyFilter(), ctx.getFormat());
            
            // if request is coming from a browser, use well-known mime type
            // so browser can display the response
            if (ctx.isBrowserHtmlRequest())
            {
                if (ctx.getFormat().equals(ResourceFormat.SWE_TEXT))
                    ctx.setResponseContentType(ResourceFormat.TEXT_PLAIN.getMimeType());
                else if (ctx.getFormat().equals(ResourceFormat.SWE_XML))
                    ctx.setResponseContentType(ResourceFormat.APPLI_XML.getMimeType());
                else
                    ctx.setResponseContentType(ctx.getFormat().getMimeType());
            }
            else
                ctx.setResponseContentType(ctx.getFormat().getMimeType());
        }
    }
    
    
    @Override
    public IObsData deserialize() throws IOException
    {
        var rec = resultReader.parseNextBlock();
        if (rec == null)
            return null;
        
        // get time stamp
        double time;
        if (timeStampIndexer != null)
            time = timeStampIndexer.getDoubleValue(rec);
        else
            time = System.currentTimeMillis() / 1000.;
        
        // TODO read FOI ID from context or dedicated field
        
        
        // create obs object
        return new ObsData.Builder()
            .withDataStream(contextData.dsID)
            .withFoi(contextData.foiId)
            .withPhenomenonTime(SWEDataUtils.toInstant(time))
            .withResult(rec)
            .build();
    }


    @Override
    public void serialize(BigId key, IObsData obs, boolean showLinks) throws IOException
    {
        resultWriter.write(obs.getResult());
        resultWriter.flush();
    }
    
    
    protected DataStreamParser getSweCommonParser(IDataStreamInfo dsInfo, InputStream is, ResourceFormat format) throws IOException
    {
        DataStreamParser dataParser = null;
        
        // init SWE datastream writer depending on desired format and native encoding
        if (dsInfo.getRecordEncoding() instanceof TextEncoding)
        {
            if (format.equals(ResourceFormat.SWE_JSON))
            {
                dataParser = new JsonDataParserGson();
            }
            else if (format.equals(ResourceFormat.SWE_TEXT))
            {
                dataParser = new AsciiDataParser();
                dataParser.setDataEncoding(dsInfo.getRecordEncoding());
            } 
            else if (format.equals(ResourceFormat.SWE_XML))
            {
                dataParser = new XmlDataParser();
                dataParser.setDataEncoding(new XMLEncodingImpl());
            }
            else if (format.equals(ResourceFormat.SWE_BINARY))
            {
                var defaultBinaryEncoding = SWEHelper.getDefaultBinaryEncoding(dsInfo.getRecordStructure());
                dataParser = SWEHelper.createDataParser(defaultBinaryEncoding);
            }
        }
        else if (dsInfo.getRecordEncoding() instanceof BinaryEncoding)
        {
            if (format.isOneOf(ResourceFormat.SWE_BINARY))
            {
                dataParser = SWEHelper.createDataParser(dsInfo.getRecordEncoding());
            }
            else if (ResourceFormat.allowNonBinaryFormat(dsInfo.getRecordEncoding()))
            {
                if (format.isOneOf(ResourceFormat.SWE_TEXT))
                {
                    dataParser = new AsciiDataParser();
                    dataParser.setDataEncoding(new TextEncodingImpl());
                }
                else if (format.isOneOf(ResourceFormat.SWE_JSON))
                {
                    dataParser = new JsonDataParserGson();
                }
                else if (format.isOneOf(ResourceFormat.SWE_XML))
                {
                    dataParser = new XmlDataParser();
                }
            }
        }
        
        if (dataParser == null)
            throw ServiceErrors.unsupportedFormat(format);
        
        dataParser.setInput(is);
        dataParser.setDataComponents(dsInfo.getRecordStructure());
        
        return dataParser;
    }
    
    
    protected DataStreamWriter getSweCommonWriter(IDataStreamInfo dsInfo, OutputStream os, PropertyFilter propFilter, ResourceFormat format) throws IOException
    {
        DataStreamWriter dataWriter = null;
        var sweEncoding = SWECommonUtils.getEncoding(dsInfo, format);
        
        if (sweEncoding instanceof JSONEncoding)
        {
            //dataWriter = new JsonDataWriterGson();
            dataWriter = new JsonDataWriter();
        }
        else if (sweEncoding instanceof TextEncoding)
        {
            dataWriter = new TextDataWriter();
            dataWriter.setDataEncoding(sweEncoding);
        } 
        else if (sweEncoding instanceof XMLEncoding)
        {
            dataWriter = new XmlDataWriter();
            dataWriter.setDataEncoding(sweEncoding);
        }
        else if (sweEncoding instanceof BinaryEncoding)
        {
            dataWriter = SWEHelper.createDataWriter(sweEncoding);
        }
        else
            throw new IllegalStateException("Unsupported encoding: " + sweEncoding.getClass().getSimpleName());
        
        dataWriter.setOutput(os);
        dataWriter.setDataComponents(dsInfo.getRecordStructure());
        
        return dataWriter;
    }


    @Override
    public void startCollection() throws IOException
    {
        resultWriter.startStream(true);
    }


    @Override
    public void endCollection(Collection<ResourceLink> links) throws IOException
    {
        resultWriter.endStream();
        resultWriter.flush();
    }
}
