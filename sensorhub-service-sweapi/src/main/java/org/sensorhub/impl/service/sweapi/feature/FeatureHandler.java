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
import java.util.Map;
import org.sensorhub.api.datastore.feature.FeatureFilter;
import org.sensorhub.api.datastore.feature.IFeatureStore;
import org.sensorhub.api.datastore.feature.FeatureFilter.Builder;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.impl.service.sweapi.IdEncoder;
import org.sensorhub.impl.service.sweapi.InvalidRequestException;
import org.sensorhub.impl.service.sweapi.SWEApiSecurity.ResourcePermissions;
import org.sensorhub.impl.service.sweapi.resource.ResourceContext;
import org.sensorhub.impl.service.sweapi.resource.ResourceFormat;
import org.sensorhub.impl.service.sweapi.resource.ResourceContext.ResourceRef;
import org.sensorhub.impl.service.sweapi.resource.ResourceBinding;
import org.vast.ogc.gml.IGeoFeature;


public class FeatureHandler extends AbstractFeatureHandler<IGeoFeature, FeatureFilter, FeatureFilter.Builder, IFeatureStore>
{
    public static final int EXTERNAL_ID_SEED = 815420;
    public static final String[] NAMES = { "features" };
    
    
    public FeatureHandler(IFeatureStore dataStore, ResourcePermissions permissions)
    {
        super(dataStore, new IdEncoder(EXTERNAL_ID_SEED), permissions);
    }


    @Override
    protected ResourceBinding<FeatureKey, IGeoFeature> getBinding(ResourceContext ctx, boolean forReading) throws IOException
    {
        var format = ctx.getFormat();
        
        if (format.isOneOf(ResourceFormat.JSON, ResourceFormat.GEOJSON))
            return new FeatureBindingGeoJson(ctx, idEncoder, forReading);
        else
            throw new InvalidRequestException(UNSUPPORTED_FORMAT_ERROR_MSG + format);
    }
    
    
    @Override
    protected boolean isValidID(long internalID)
    {
        return dataStore.contains(internalID);
    }
    
    
    @Override
    public boolean doPost(ResourceContext ctx) throws IOException
    {
        if (ctx.isEmpty() && !(ctx.getParentRef().type instanceof FeatureHandler))
            return ctx.sendError(405, "Features can only be created within Feature Collections");
        
        return super.doPost(ctx);
    }


    @Override
    protected void buildFilter(ResourceRef parent, Map<String, String[]> queryParams, Builder builder) throws InvalidRequestException
    {
        super.buildFilter(parent, queryParams, builder);
    }


    @Override
    public String[] getNames()
    {
        return NAMES;
    }


    @Override
    protected void validate(IGeoFeature resource)
    {
        // TODO Auto-generated method stub
        
    }
}