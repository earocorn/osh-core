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
import org.sensorhub.impl.service.sweapi.InvalidRequestException;
import org.sensorhub.impl.service.sweapi.SWEApiSecurity.ResourcePermissions;
import org.sensorhub.impl.service.sweapi.resource.ResourceContext;
import org.sensorhub.impl.service.sweapi.resource.ResourceContext.ResourceRef;
import org.vast.ogc.gml.IGeoFeature;


public class FeatureMembersHandler extends FeatureHandler
{
    public static final String[] NAMES = { "members" };
    
    
    public FeatureMembersHandler(IFeatureStore dataStore, ResourcePermissions permissions)
    {
        super(dataStore, permissions);
    }
    
    
    @Override
    public boolean doPost(ResourceContext ctx) throws IOException
    {
        //if (ctx.isEmpty() && !(ctx.getParentRef().type instanceof ProjectResourceType))
        //    return sendError(405, "Feature Collections can only be created within Projects", resp);
        
        return super.doPost(ctx);
    }

    
    @Override
    protected void buildFilter(final ResourceRef parent, final Map<String, String[]> queryParams, final FeatureFilter.Builder builder) throws InvalidRequestException
    {
        super.buildFilter(parent, queryParams, builder);
        
        // filter on parent if needed
        if (parent.internalID > 0)
        {
            builder.withParents()
                .withInternalIDs(parent.internalID)
                .done();
        }
    }


    @Override
    protected void validate(IGeoFeature resource)
    {
        // TODO Auto-generated method stub
        
    }
    
    
    @Override
    public String[] getNames()
    {
        return NAMES;
    }
}