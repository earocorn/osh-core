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

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.LinkedHashMap;
import org.h2.mvstore.MVStore;
import org.junit.After;
import org.junit.Test;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.impl.datastore.AbstractTestObsStore;
import org.sensorhub.impl.datastore.h2.MVDatabaseConfig.IdProviderType;
import org.vast.data.DataBlockDouble;


public class TestMVObsStore extends AbstractTestObsStore<MVObsStoreImpl>
{
    private static String DB_FILE_PREFIX = "test-mvobs-";
    protected File dbFile;
    protected MVStore mvStore;
    
    
    protected MVObsStoreImpl initStore() throws Exception
    {
        dbFile = File.createTempFile(DB_FILE_PREFIX, ".dat");
        dbFile.deleteOnExit();
        return openMVStore();
    }
    
    
    protected void forceReadBackFromStorage()
    {
        this.obsStore = openMVStore();
    }
    
    
    private MVObsStoreImpl openMVStore()
    {
        if (mvStore != null)
        {
            mvStore.commit();
            mvStore.close();
            System.out.println("MVStore flushed to disk");
        }
        
        mvStore = new MVStore.Builder()
                .fileName(dbFile.getAbsolutePath())
                .autoCommitBufferSize(10)
                .cacheSize(1)
                .open();
        
        return MVObsStoreImpl.open(mvStore, DATABASE_NUM, IdProviderType.SEQUENTIAL,
            MVDataStoreInfo.builder()
                .withName(OBS_DATASTORE_NAME)
                .build());
    }
    
    
    @After
    public void cleanup() throws Exception
    {
        if (mvStore != null)
            mvStore.close();
        
        if (dbFile != null) 
        {
            System.out.println("DB file size was " + dbFile.length()/1024 + "KB\n");
            
            Files.list(dbFile.toPath().getParent())
                 .filter(f -> f.getFileName().toString().startsWith(DB_FILE_PREFIX))
                 .forEach(f -> f.toFile().delete());
        }            
    }
    
    
    @Test
    public void testGetNumRecordsTwoDataStreams() throws Exception
    {
        super.testGetNumRecordsTwoDataStreams();
        
        // check that 2 series were created
        assertEquals(2, obsStore.obsSeriesMainIndex.size());
    }
    
    
    @Test
    public void testSelectByDataStreamWithFoiIdMatchingOtherDataStreamId() throws Exception
    {
        var ds1 = addSimpleDataStream(bigId(1), "out1");
        var ds2 = addSimpleDataStream(bigId(2), "out2");
        
        var ds1Obs = addSimpleObsWithoutResultTime(ds1, ds2, Instant.parse("2020-01-01T00:00:00Z"), 5);
        var ds2Obs = addSimpleObsWithoutResultTime(ds2, bigId(42), Instant.parse("2020-01-02T00:00:00Z"), 7);
        
        forceReadBackFromStorage();
        
        var filter = new ObsFilter.Builder()
            .withDataStreams(ds1)
            .build();
        checkSelectedEntries(obsStore.selectEntries(filter), ds1Obs, filter);
        
        filter = new ObsFilter.Builder()
            .withDataStreams(ds2)
            .build();
        checkSelectedEntries(obsStore.selectEntries(filter), ds2Obs, filter);
        
        var allExpected = new LinkedHashMap<>(ds1Obs);
        allExpected.putAll(ds2Obs);
        filter = new ObsFilter.Builder()
            .withDataStreams(ds1, ds2)
            .build();
        checkSelectedEntries(obsStore.selectEntries(filter), allExpected, filter);
    }


    @Test
    public void testSelectByDataStreamFiltersMismatchedSeriesRecords() throws Exception
    {
        var ds1 = addSimpleDataStream(bigId(1), "out1");
        var ds2 = addSimpleDataStream(bigId(2), "out2");

        var ds2Obs = addSimpleObsWithoutResultTime(ds2, ds1, Instant.parse("2020-01-01T00:00:00Z"), 1);
        var ds2SeriesID = ((MVTimeSeriesRecordKey)ds2Obs.keySet().iterator().next()).seriesID;

        var corruptObs = new ObsData.Builder()
            .withDataStream(ds1)
            .withFoi(ds2)
            .withPhenomenonTime(Instant.parse("2020-01-01T00:01:00Z"))
            .withResult(new DataBlockDouble(5))
            .build();
        obsStore.obsRecordsIndex.put(
            new MVTimeSeriesRecordKey(DATABASE_NUM, ds2SeriesID, corruptObs.getPhenomenonTime()),
            corruptObs);

        forceReadBackFromStorage();

        var filter = new ObsFilter.Builder()
            .withDataStreams(ds2)
            .build();
        checkSelectedEntries(obsStore.selectEntries(filter), ds2Obs, filter);
        assertEquals(ds2Obs.size(), obsStore.countMatchingEntries(filter));
    }

}
