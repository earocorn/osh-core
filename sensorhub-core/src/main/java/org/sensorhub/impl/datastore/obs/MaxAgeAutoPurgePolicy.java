/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.obs;

import org.sensorhub.api.database.IObsDbAutoPurgePolicy;
import org.sensorhub.api.database.IProcedureObsDatabase;
import org.slf4j.Logger;
import org.vast.util.DateTimeFormat;


/**
 * <p>
 * Implementation of purging policy removing records when they reach a 
 * certain age
 * </p>
 *
 * @author Alex Robin
 * @since Oct 29, 2019
 */
public class MaxAgeAutoPurgePolicy implements IObsDbAutoPurgePolicy
{
    MaxAgeAutoPurgeConfig config;
    DateTimeFormat df = new DateTimeFormat();
    
    MaxAgeAutoPurgePolicy(MaxAgeAutoPurgeConfig config)
    {
        this.config = config;
    }
    
    
    @Override
    public int trimStorage(IProcedureObsDatabase db, Logger log)
    {
        return 0;
        /*int numDeletedRecords = 0;
        
        for (IRecordStoreInfo streamInfo: storage.getRecordStores().values())
        {
            double[] timeRange = storage.getRecordsTimeRange(streamInfo.getName());
            double beginTime = timeRange[0];
            double endTime = timeRange[1];
            
            if (beginTime < endTime - config.maxRecordAge)
            {
                final double[] obsoleteTimeRange = new double[] {beginTime, endTime - config.maxRecordAge};
                
                if (log.isInfoEnabled())
                {
                    log.info("Purging {} data for period {}/{}",
                             streamInfo.getName(),
                             df.formatIso(obsoleteTimeRange[0], 0),
                             df.formatIso(obsoleteTimeRange[1], 0));
                }

                // remove records
                numDeletedRecords += storage.removeRecords(new DataFilter(streamInfo.getName())
                {
                    @Override
                    public double[] getTimeStampRange()
                    {
                        return obsoleteTimeRange;
                    }                    
                });
                
                log.info("{} records deleted", numDeletedRecords);
                
                // remove data source descriptions
                storage.removeDataSourceDescriptionHistory(obsoleteTimeRange[0], obsoleteTimeRange[1]);
            }
        }        
        
        storage.commit();
        return numDeletedRecords;*/
    }

}
