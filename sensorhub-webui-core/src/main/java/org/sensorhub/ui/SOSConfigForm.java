/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui;

import java.util.LinkedHashMap;
import java.util.Map;
import org.sensorhub.ui.data.BaseProperty;


@SuppressWarnings("serial")
public class SOSConfigForm extends GenericConfigForm
{
    protected static final String SOS_PACKAGE = "org.sensorhub.impl.service.sos.";
    protected static final String PROP_DATAPROVIDERS = "customDataProviders";
    protected static final String PROP_ENDPOINT = "endPoint";
    
    
    @Override
    public boolean isFieldVisible(String propId)
    {
        // hide offeringID since we cannot configure it manually anymore
        if (propId.equals("offeringID"))
            return false;
        
        return super.isFieldVisible(propId);
    }


    @Override
    public Map<String, Class<?>> getPossibleTypes(String propId, BaseProperty<?> prop)
    {
        if (propId.equals(PROP_DATAPROVIDERS))
        {
            Map<String, Class<?>> classList = new LinkedHashMap<>();
            try
            {
                classList.put("Streaming Data Source", Class.forName(SOS_PACKAGE + "SystemDataProviderConfig"));
                classList.put("Historical Data Source", Class.forName(SOS_PACKAGE + "SystemDataProviderConfig"));
            }
            catch (ClassNotFoundException e)
            {
                getOshLogger().error("Cannot find SOS provider class", e);
            }
            return classList;
        }
        
        return super.getPossibleTypes(propId, prop);
    }
}
