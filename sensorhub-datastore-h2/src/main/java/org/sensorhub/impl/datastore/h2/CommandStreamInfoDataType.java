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
import org.sensorhub.api.command.CommandStreamInfo;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.feature.FeatureId;
import org.sensorhub.impl.datastore.h2.kryo.KryoDataType;
import org.sensorhub.impl.datastore.h2.kryo.PersistentClassResolver;
import org.sensorhub.impl.serialization.kryo.BigIdSerializers;
import org.sensorhub.impl.serialization.kryo.VersionedSerializer;
import org.sensorhub.impl.serialization.kryo.compat.v1.FeatureIdSerializerV1;
import org.sensorhub.impl.serialization.kryo.compat.v2.CommandStreamInfoSerializerV2;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import net.opengis.OgcPropertyList;


class CommandStreamInfoDataType extends KryoDataType
{
    CommandStreamInfoDataType(MVMap<String, Integer> kryoClassMap, int idScope)
    {
        this.classResolver = () -> new PersistentClassResolver(kryoClassMap);
        this.configurator = kryo -> {
            
            // avoid using collection serializer on OgcPropertyList because
            // the add method doesn't behave as expected
            kryo.addDefaultSerializer(OgcPropertyList.class, FieldSerializer.class);
            
            // register custom serializers w/ backward compatibility
            kryo.addDefaultSerializer(CommandStreamInfo.class,
                VersionedSerializer.<CommandStreamInfo>factory(H2Utils.CURRENT_VERSION)
                    .put(2, new CommandStreamInfoSerializerV2(kryo))
                    .build());
            
            kryo.addDefaultSerializer(FeatureId.class,
                VersionedSerializer.<FeatureId>factory(H2Utils.CURRENT_VERSION)
                    .put(2, new FeatureIdSerializerV1(kryo, idScope))
                    .build());
            
            kryo.addDefaultSerializer(BigId.class, BigIdSerializers.factory(idScope));
        };
    }
}