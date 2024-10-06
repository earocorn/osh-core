package org.sensorhub.impl.service.consys.client;

import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.swe.v20.*;
import org.junit.Test;
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.feature.FeatureId;
import org.sensorhub.api.system.ISystemWithDesc;
import org.sensorhub.impl.service.consys.obs.DataStreamBindingJson;
import org.sensorhub.impl.service.consys.obs.ObsBindingOmJson;
import org.sensorhub.impl.service.consys.resource.RequestContext;
import org.sensorhub.impl.service.consys.stream.StreamHandler;
import org.sensorhub.impl.service.consys.system.SystemBindingSmlJson;
import org.sensorhub.impl.system.wrapper.SystemWrapper;
import org.vast.data.TextEncodingImpl;
import org.vast.ogc.geopose.PoseImpl;
import org.vast.sensorML.SMLHelper;
import org.vast.util.TimeExtent;

import java.io.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;

public class AdjudicationTests {

    SMLHelper sml = new SMLHelper();
    GMLFactory gml = new GMLFactory();

    static class InMemoryBufferStreamHandler implements StreamHandler
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        public void setStartCallback(Runnable onStart) {}
        public void setCloseCallback(Runnable onClose) {}
        public void sendPacket() throws IOException {}
        public void close() {}
        public OutputStream getOutputStream() { return os; }
        public InputStream getAsInputStream() { return new ByteArrayInputStream(os.toByteArray()); }
    }

    @Test
    public void checkSystem() throws IOException {
        var buffer = new InMemoryBufferStreamHandler();
        var ctx = new RequestContext(buffer);
        var binding = new SystemBindingSmlJson(ctx, null, false);

        binding.serialize(null, createAdjudicationSystem("lane1"), false);
        System.out.println(ctx.getOutputStream().toString());
    }

    @Test
    public void checkDataStream() throws IOException {
        var buffer = new InMemoryBufferStreamHandler();
        var ctx = new RequestContext(buffer);
        var binding = new DataStreamBindingJson(ctx, null, null , false, Collections.emptyMap());

        var ds = createAdjudicationDataStream();

        binding.serializeCreate(ds);
        System.out.println(ctx.getOutputStream().toString());
    }

    @Test
    public void checkObservation() throws IOException {
        var buffer = new InMemoryBufferStreamHandler();
        var ctx = new RequestContext(buffer);
        var binding = new ObsBindingOmJson(ctx, null, false, null);

        var obs = createObservation();

        binding.serializeCreate(obs);
        System.out.println(ctx.getOutputStream().toString());
    }

    private ISystemWithDesc createAdjudicationSystem(String laneId)
    {
        var location = new PoseImpl.PoseBuilder()
                .position(new double[] {1.0, 2.0, 3.0})
                .build();

        var system = sml.createPhysicalSystem()
                .uniqueID("urn:osh:adjudication:" + laneId)
                .name("Occupancy Adjudication " + laneId)
                .description("Adjudication records for occupancies from " + laneId)
                .id("ADJUDICATION_" + laneId.toUpperCase())
                .definition("http://www.w3.org/ns/sosa/Sensor")
                .validFrom(OffsetDateTime.now())
                .position(location)
                .build();
        return new SystemWrapper(system);
    }

    private DataComponent createAdjudicationOutput()
    {

        String[] adjudicationCodes = new String[] {
                "Code 1: Contraband Found",
                "Code 2: Other",
                "Code 3: Medical Isotope Found",
                "Code 4: NORM Found",
                "Code 5: Declared Shipment of Radioactive Material",
                "Code 6: Physical Inspection Negative",
                "Code 7: RIID/ASP Indicates Background Only",
                "Code 8: Other",
                "Code 9: Authorized Test, Maintenence, or Training Activity",
                "Code 10: Unauthorized Activity",
                "Code 11: Other",
                ""
        };

        String[] inspectionStatuses = new String[] {
                "NONE",
                "REQUESTED",
                "COMPLETE"
        };

        return sml.createRecord()
                .name("adjudication")
                .addField("username", sml.createText()
                        .label("Username")
                        .definition(SMLHelper.getPropertyUri("Username"))
                        .build())
                .addField("feedback", sml.createText()
                        .label("Feedback")
                        .definition(SMLHelper.getPropertyUri("Feedback"))
                        .build())
                .addField("adjudicationCode", sml.createCategory()
                        .label("Adjudication Code")
                        .definition(SMLHelper.getPropertyUri("AdjudicationCode"))
                        .dataType(DataType.UTF_STRING)
                        .addAllowedValues(adjudicationCodes)
                        .build())
                .addField("isotopes", sml.createText()
                        .label("Isotopes")
                        .definition(SMLHelper.getPropertyUri("Username"))
                        .build())
                .addField("secondaryInspectionStatus", sml.createCategory()
                        .label("Secondary Inspection Status")
                        .definition(SMLHelper.getPropertyUri("SecondaryInspectionStatus"))
                        .addAllowedValues(inspectionStatuses)
                        .value("NONE")
                        .build())
                .addField("filePaths", sml.createText()
                        .label("Supplemental File Paths")
                        .definition(SMLHelper.getPropertyUri("FilePaths"))
                        .build())
                .addField("occupancyId", sml.createText()
                        .label("Occupancy ID")
                        .definition(SMLHelper.getPropertyUri("OccupancyID"))
                        .build())
                .addField("alarmingSystemUid", sml.createText()
                        .label("UID of Alarming System")
                        .definition(SMLHelper.getPropertyUri("SystemUID"))
                        .build())
                .build();
    }

    private IDataStreamInfo createAdjudicationDataStream()
    {
        var recordStruct = createAdjudicationOutput();

        return new DataStreamInfo.Builder()
                .withName("Occupancy Adjudication")
                .withRecordDescription(recordStruct)
                .withRecordEncoding(new TextEncodingImpl())
                .withValidTime(TimeExtent.now())
                .withSystem(FeatureId.NULL_FEATURE)
                .build();
    }

    private IObsData createObservation()
    {
        DataBlock dataBlock = createAdjudicationOutput().createDataBlock();

        dataBlock.setStringValue(0, "alexalmanza");
        dataBlock.setStringValue(1, "These are my personal comments on the alarm.");
        dataBlock.setStringValue(2, "Code 6: Physical Inspection Negative");
        dataBlock.setStringValue(3, "");
        dataBlock.setStringValue(4, "REQUESTED");
        dataBlock.setStringValue(5, "https://www.drive.google.com/file1");
        dataBlock.setStringValue(6, "13m2l1m2k3m1l2meklml12ml1m23kl");
        dataBlock.setStringValue(7, "urn:osh:sensor:rapiscan:rpm001");

        return new ObsData.Builder()
                .withResult(dataBlock)
                .withResultTime(Instant.now())
                .withPhenomenonTime(Instant.now())
                .build();
    }

}
