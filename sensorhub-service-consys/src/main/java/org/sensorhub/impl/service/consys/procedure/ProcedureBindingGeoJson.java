/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.consys.procedure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import javax.xml.namespace.QName;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.api.database.IProcedureDatabase;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.procedure.IProcedureWithDesc;
import org.sensorhub.impl.service.consys.feature.AbstractFeatureBindingGeoJson;
import org.sensorhub.impl.service.consys.resource.RequestContext;
import org.sensorhub.impl.service.consys.resource.ResourceFormat;
import org.sensorhub.impl.service.consys.resource.ResourceLink;
import org.sensorhub.impl.service.consys.sensorml.ProcedureAdapter;
import org.vast.ogc.gml.GeoJsonBindings;
import org.vast.ogc.gml.IFeature;
import org.vast.util.Asserts;
import org.vast.util.TimeExtent;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.opengis.sensorml.v20.AbstractProcess;


/**
 * <p>
 * GeoJSON formatter for system resources
 * </p>
 *
 * @author Alex Robin
 * @since Jan 26, 2021
 */
public class ProcedureBindingGeoJson extends AbstractFeatureBindingGeoJson<IProcedureWithDesc, IProcedureDatabase>
{
    
    public ProcedureBindingGeoJson(RequestContext ctx, IdEncoders idEncoders, IProcedureDatabase db, boolean forReading) throws IOException
    {
        super(ctx, idEncoders, db, forReading);
    }
    
    
    @Override
    protected GeoJsonBindings getJsonBindings()
    {
        return new GeoJsonBindings() {
            public IFeature readFeature(JsonReader reader) throws IOException
            {
                var f = super.readFeature(reader);
                return new ProcedureAdapter(f);
            }
            
            protected void writeCommonFeatureProperties(JsonWriter writer, IFeature bean) throws IOException
            {
                super.writeCommonFeatureProperties(writer, bean);
            }
            
            protected void writeCustomJsonProperties(JsonWriter writer, IFeature bean) throws IOException
            {
                if (showLinks.get())
                {
                    var links = new ArrayList<ResourceLink>();
                    
                    links.add(new ResourceLink.Builder()
                        .rel("canonical")
                        .href("/" + ProcedureHandler.NAMES[0] + "/" + bean.getId())
                        .type(ResourceFormat.JSON.getMimeType())
                        .build());
                    
                    links.add(new ResourceLink.Builder()
                        .rel("alternate")
                        .title("Detailed description of procedure in SensorML format")
                        .href("/" + ProcedureHandler.NAMES[0] + "/" + bean.getId() + "?f=sml")
                        .type(ResourceFormat.SML_JSON.getMimeType())
                        .build());
                    
                    links.add(new ResourceLink.Builder()
                        .rel("alternate")
                        .title("Detailed description of procedure in HTML format")
                        .href("/" + ProcedureHandler.NAMES[0] + "/" + bean.getId() + "?f=html")
                        .type(ResourceFormat.HTML.getMimeType())
                        .build());
                    
                    writeLinksAsJson(writer, links);
                }
            }
        };
    }
    
    
    @Override
    protected IProcedureWithDesc getFeatureWithId(FeatureKey key, IProcedureWithDesc proc)
    {
        Asserts.checkNotNull(key, FeatureKey.class);
        Asserts.checkNotNull(proc, IProcedureWithDesc.class);
        
        return new IProcedureWithDesc()
        {
            public String getUniqueIdentifier() { return proc.getUniqueIdentifier(); }
            public String getType() { return proc.getType(); }
            public String getName() { return proc.getName(); }
            public String getDescription() { return proc.getDescription(); }
            public Map<QName, Object> getProperties() { return proc.getProperties(); }  
            public TimeExtent getValidTime() { return proc.getValidTime(); }
            public AbstractProcess getFullDescription() { return proc.getFullDescription(); }
        
            public String getId()
            {
                return idEncoders.getProcedureIdEncoder().encodeID(key.getInternalID());
            }
        };
    }
}
