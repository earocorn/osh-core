package org.sensorhub.impl.sensor;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;
import org.sensorhub.api.sensor.IdentificationInfo;
import org.sensorhub.api.sensor.SensorConfig;
import org.vast.util.ResponsibleParty;

public class TestIdentificationInfo
{
    @Test
    public void overridesSurviveDescriptionRegeneration() throws Exception
    {
        var config = new SensorConfig();
        config.identificationInfo = new IdentificationInfo();
        config.identificationInfo.systemUID = "urn:test:configured";
        config.identificationInfo.systemKindUID = "urn:test:kind";
        var party = new ResponsibleParty();
        party.setOrganizationName("Operator");
        party.getEmails().addAll(List.of("first@example.org", "second@example.org"));
        party.getVoiceNumbers().addAll(List.of("123", "456"));
        party.setWebsite("https://example.org");
        config.identificationInfo.responsibleParties = List.of(party);

        var sensor = new FakeSensor();
        sensor.init(config);
        assertEquals(config.identificationInfo.systemUID, sensor.getUniqueIdentifier());
        assertFalse(sensor.randomUniqueID);
        for (int i = 0; i < 2; i++)
        {
            var description = sensor.getCurrentDescription();
            assertEquals(sensor.getUniqueIdentifier(), description.getUniqueIdentifier());
            assertEquals(config.identificationInfo.systemKindUID, description.getTypeOf().getHref());
            assertEquals(1, description.getContactsList().size());
            var contact = description.getContactsList().get(0).getContactList().get(0);
            assertEquals("Operator", contact.getOrganisationName());
            assertEquals(party.getEmails(), contact.getContactInfo().getAddress().getElectronicMailAddressList());
            assertEquals(party.getVoiceNumbers(), contact.getContactInfo().getPhone().getVoiceList());
            assertEquals(party.getWebsite(), contact.getContactInfo().getOnlineResource().getLinkage());
            sensor.updateSensorDescription();
        }
    }
}
