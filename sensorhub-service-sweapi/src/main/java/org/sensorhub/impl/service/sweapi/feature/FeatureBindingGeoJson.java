/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sweapi.feature;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import javax.xml.namespace.QName;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.impl.service.sweapi.IdEncoder;
import org.sensorhub.impl.service.sweapi.resource.RequestContext;
import org.sensorhub.impl.service.sweapi.resource.ResourceBinding;
import org.vast.ogc.gml.GeoJsonBindings;
import org.vast.ogc.gml.IFeature;
import org.vast.ogc.gml.IGeoFeature;
import org.vast.ogc.gml.ITemporalFeature;
import org.vast.util.Asserts;
import org.vast.util.TimeExtent;
import com.google.gson.stream.JsonWriter;
import net.opengis.gml.v32.AbstractGeometry;


/**
 * <p>
 * GeoJSON formatter for feature resources
 * </p>
 *
 * @author Alex Robin
 * @since Jan 26, 2021
 */
public class FeatureBindingGeoJson extends AbstractFeatureBindingGeoJson<IFeature>
{
    interface IGeoTemporalFeature extends IGeoFeature, ITemporalFeature {}
    
    
    FeatureBindingGeoJson(RequestContext ctx, IdEncoder idEncoder, boolean forReading) throws IOException
    {
        super(ctx, idEncoder, forReading);
    }
    
    
    protected GeoJsonBindings getJsonBindings()
    {
        return new GeoJsonBindings() {
            protected void writeDateTimeValue(JsonWriter writer, OffsetDateTime dateTime) throws IOException
            {
                super.writeDateTimeValue(writer, dateTime.truncatedTo(ChronoUnit.SECONDS));
            }
        };
    }
    
    
    @Override
    protected IFeature getFeatureWithId(FeatureKey key, IFeature f)
    {
        Asserts.checkNotNull(key, FeatureKey.class);
        Asserts.checkNotNull(f, IFeature.class);
        
        return new IGeoTemporalFeature() {
            
            public String getUniqueIdentifier() { return f.getUniqueIdentifier(); }
            public String getName() { return f.getName(); }
            public String getDescription() { return f.getDescription(); }
            public Map<QName, Object> getProperties() { return f.getProperties(); }

            @Override
            public String getId()
            {
                var externalID = FeatureBindingGeoJson.this.encodeID(key.getInternalID());
                return Long.toString(externalID, ResourceBinding.ID_RADIX);
            }
            
            @Override
            public AbstractGeometry getGeometry()
            {
                return f instanceof IGeoFeature ?
                    ((IGeoFeature)f).getGeometry() : null;
            }

            @Override
            public TimeExtent getValidTime()
            {
                return f instanceof ITemporalFeature ?
                    ((ITemporalFeature)f).getValidTime() : null;
            }
        };
    }
}
