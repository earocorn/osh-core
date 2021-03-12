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

import java.math.BigInteger;
import java.util.stream.Stream;
import org.sensorhub.api.datastore.feature.IFoiStore;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.obs.ObsStatsQuery;
import org.sensorhub.api.feature.FeatureId;
import org.sensorhub.api.datastore.obs.IObsStore.ObsField;
import org.sensorhub.api.obs.IObsData;
import org.sensorhub.api.obs.ObsData;
import org.sensorhub.api.obs.ObsStats;
import org.sensorhub.impl.service.sweapi.AbstractDataStoreWrapper;
import org.sensorhub.impl.service.sweapi.IdConverter;
import org.vast.util.Asserts;


public class ObsStoreWrapper extends AbstractDataStoreWrapper<BigInteger, IObsData, ObsField, ObsFilter, IObsStore> implements IObsStore
{
    final IdConverter idConverter;
    
    
    public ObsStoreWrapper(IObsStore readStore, IObsStore writeStore, IdConverter idConverter)
    {
        super(readStore, writeStore);
        this.idConverter = Asserts.checkNotNull(idConverter, IdConverter.class);
    }


    @Override
    public ObsFilter.Builder filterBuilder()
    {
        return new ObsFilter.Builder();
    }


    @Override
    public BigInteger add(IObsData obs)
    {
        var dsInternalID = idConverter.toInternalID(obs.getDataStreamID());
        
        var foiId = IObsData.NO_FOI;
        if (obs.getFoiID() != foiId)
        {
            var foiInternalID = idConverter.toInternalID(obs.getFoiID().getInternalID()); 
            var foiUID = obs.getFoiID().getUniqueID();
            foiId = new FeatureId(foiInternalID, foiUID);
        }
        
        obs = ObsData.Builder.from(obs)
            .withDataStream(dsInternalID)
            .withFoi(foiId)
            .build();
        
        return toPublicKey(getWriteStore().add(obs));
    }


    @Override
    public Stream<ObsStats> getStatistics(ObsStatsQuery query)
    {
        return getReadStore().getStatistics(query);
    }


    @Override
    public void linkTo(IFoiStore foiStore)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public IDataStreamStore getDataStreams()
    {
        return getReadStore().getDataStreams();
    }


    @Override
    protected BigInteger toInternalKey(BigInteger publicKey)
    {
        return idConverter.toInternalID(publicKey);
    }


    @Override
    protected BigInteger toPublicKey(BigInteger internalKey)
    {
        return idConverter.toPublicID(internalKey);
    }

}