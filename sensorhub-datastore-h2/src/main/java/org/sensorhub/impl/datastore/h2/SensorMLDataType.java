/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.h2;

import org.h2.mvstore.MVMap;
import org.sensorhub.impl.datastore.h2.kryo.KryoDataType;
import org.sensorhub.impl.datastore.h2.kryo.PersistentClassResolver;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import net.opengis.OgcPropertyList;


/**
 * <p>
 * H2 DataType implementation for ISystemWithDesc objects
 * </p>
 *
 * @author Alex Robin
 * @date Apr 7, 2018
 */
public class SensorMLDataType extends KryoDataType
{
    SensorMLDataType(MVMap<String, Integer> kryoClassMap)
    {
        this.classResolver = () -> new PersistentClassResolver(kryoClassMap);
        this.configurator = kryo -> {
            
            // avoid using collection serializer on OgcPropertyList because
            // the add method doesn't behave as expected
            kryo.addDefaultSerializer(OgcPropertyList.class, FieldSerializer.class);
        };
    }
}