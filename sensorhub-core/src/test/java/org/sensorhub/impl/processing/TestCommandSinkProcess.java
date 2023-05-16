/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.processing;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.command.CommandStatusEvent;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleEvent.ModuleState;
import org.sensorhub.api.processing.IProcessModule;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.sensor.SensorConfig;
import org.sensorhub.impl.SensorHub;
import org.sensorhub.impl.module.ModuleRegistry;
import org.sensorhub.impl.sensor.FakeSensor;
import org.sensorhub.impl.sensor.FakeSensorControl1;
import org.sensorhub.impl.sensor.FakeSensorData;
import org.vast.cdm.common.DataStreamWriter;
import org.vast.data.TextEncodingImpl;
import org.vast.swe.AsciiDataWriter;


public class TestCommandSinkProcess implements IEventListener
{
    static String FAKE_SENSOR1_ID = "FAKE_SENSOR1";
    static String NAME_OUTPUT1 = "weather";
    static String NAME_INPUT1 = "cmd1";
    static final double SAMPLING_PERIOD = 0.1;
    static final int SAMPLE_COUNT = 10;

    ModuleRegistry registry;
    DataStreamWriter writer;
    volatile int eventCount = 0;
        
    
    @Before
    public void setupFramework() throws Exception
    {
        // init sensorhub with in-memory config
        var hub = new SensorHub();
        hub.start();
        registry = hub.getModuleRegistry();
    }
    
    
    protected ISensorModule<?> createSensor() throws Exception
    {
        // create test sensor
        SensorConfig sensorCfg = new SensorConfig();
        sensorCfg.autoStart = false;
        sensorCfg.moduleClass = FakeSensor.class.getCanonicalName();
        sensorCfg.id = FAKE_SENSOR1_ID;
        sensorCfg.name = "Sensor1";
        IModule<?> sensor = registry.loadModule(sensorCfg);
        sensor.init();
        var sensorOutput = new FakeSensorData((FakeSensor)sensor, NAME_OUTPUT1, SAMPLING_PERIOD, SAMPLE_COUNT);
        ((FakeSensor)sensor).setDataInterfaces(sensorOutput);
        var controlInput = new FakeSensorControl1((FakeSensor)sensor, NAME_INPUT1);
        ((FakeSensor)sensor).setControlInterfaces(controlInput);
        controlInput.registerListener(this);
        sensor.start();
        return (FakeSensor)sensor;
    }
    
    
    protected void runProcess(IProcessModule<?> process) throws Exception
    {
        // prepare event writer
        writer = new AsciiDataWriter();
        writer.setDataEncoding(new TextEncodingImpl(",", ""));
        writer.setOutput(System.out);
        
        process.start();
        //new SMLUtils(SMLUtils.V2_0).writeProcess(System.out, process.getCurrentDescription(), true);
        ((FakeSensor)registry.getModuleById(FAKE_SENSOR1_ID)).startSendingData(100);
                
        long t0 = System.currentTimeMillis();
        synchronized (this) 
        {
            while (eventCount < SAMPLE_COUNT)
            {
                if (System.currentTimeMillis() - t0 >= 10000L)
                    Assert.fail("No data received before timeout");
                wait(1000L);
            }
        }
        
        System.out.println();
    }
    
    
    protected IProcessModule<?> createSMLProcess(String smlUrl) throws Exception
    {
        SMLProcessConfig processCfg = new SMLProcessConfig();
        processCfg.autoStart = false;
        processCfg.name = "SensorML Process #1";
        processCfg.moduleClass = SMLProcessImpl.class.getCanonicalName();
        processCfg.sensorML = smlUrl;
        
        @SuppressWarnings("unchecked")
        IProcessModule<SMLProcessConfig> process = (IProcessModule<SMLProcessConfig>)registry.loadModule(processCfg);
        process.init();
        
        process.waitForState(ModuleState.INITIALIZED, 5000);
        return process;
    }
    
    
    @Test
    public void testSMLSimpleProcess() throws Exception
    {
        createSensor();
        String smlUrl = TestCommandSinkProcess.class.getResource("/test-processchain-controlloop.xml").getFile();
        IProcessModule<?> process = createSMLProcess(smlUrl);
        runProcess(process);
    }
    
    
    @Override
    public void handleEvent(Event e)
    {
        if (e instanceof CommandStatusEvent)
        {
            System.out.println(e);
            eventCount++;
        }
    }
    
        
    @After
    public void cleanup()
    {
        try
        {
            registry.shutdown(false, false);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
