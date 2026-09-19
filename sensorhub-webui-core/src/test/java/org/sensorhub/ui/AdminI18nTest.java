package org.sensorhub.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.sensor.SensorSystemConfig;
import org.junit.Test;


public class AdminI18nTest
{
    private static final Locale GREEK = new Locale("el");


    @Test
    public void translatesAndInterpolatesMessages()
    {
        assertEquals("Sensores", AdminI18n.tr(new Locale("es"), "nav.sensors"));
        assertEquals("3 modules", AdminI18n.tr(Locale.ENGLISH, "status.modules", 3));
        assertEquals("3 módulos", AdminI18n.tr(new Locale("es"), "status.modules", 3));
        assertEquals("Impossible d'arrêter Scanner", AdminI18n.tr(Locale.FRENCH, "error.moduleStop", "Scanner"));
        assertEquals("Αισθητήρες", AdminI18n.tr(GREEK, "nav.sensors"));
        assertEquals("3 μονάδες", AdminI18n.tr(GREEK, "status.modules", 3));
    }


    @Test
    public void normalizesLocalesAndFallsBackToEnglish()
    {
        assertEquals("es", AdminI18n.normalize(new Locale("es", "MX")).getLanguage());
        assertEquals("el", AdminI18n.normalize(new Locale("el", "GR")).getLanguage());
        assertEquals(Locale.ENGLISH, AdminI18n.normalize(Locale.GERMAN));
        assertEquals("Sensors", AdminI18n.tr(Locale.GERMAN, "nav.sensors"));
    }


    @Test
    public void translatedBundlesContainEveryEnglishKey() throws Exception
    {
        Properties english = loadProperties("AdminMessages.properties");
        Properties spanish = loadProperties("AdminMessages_es.properties");
        Properties french = loadProperties("AdminMessages_fr.properties");
        Properties greek = loadProperties("AdminMessages_el.properties");

        assertEquals(english.keySet(), spanish.keySet());
        assertEquals(english.keySet(), french.keySet());
        assertEquals(english.keySet(), greek.keySet());
    }


    @Test
    public void translatesConfigurationMetadataAndFallsBack() throws Exception
    {
        assertEquals(
            "ID del módulo",
            AdminI18n.trConfig(new Locale("es"), ModuleConfig.class, "id.label", "Module ID"));
        assertEquals(
            "Emplacement fixe",
            AdminI18n.trConfig(Locale.FRENCH, SensorSystemConfig.class, "location.label", "Fixed Location"));
        assertEquals(
            "Untranslated",
            AdminI18n.trConfig(Locale.FRENCH, ModuleConfig.class, "missing.label", "Untranslated"));
        assertEquals(
            "Αναγνωριστικό μονάδας",
            AdminI18n.trConfig(GREEK, ModuleConfig.class, "id.label", "Module ID"));

        assertTranslatedBundleComplete("/org/sensorhub/api/module/i18n/ConfigMessages");
        assertTranslatedBundleComplete("/org/sensorhub/api/sensor/i18n/ConfigMessages");
        assertTranslatedBundleComplete("/org/sensorhub/impl/sensor/i18n/ConfigMessages");
    }


    private Properties loadProperties(String fileName) throws Exception
    {
        String path = "/org/sensorhub/ui/i18n/" + fileName;
        InputStream input = getClass().getResourceAsStream(path);
        assertNotNull("Missing resource " + path, input);

        Properties properties = new Properties();
        try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8))
        {
            properties.load(reader);
        }
        return properties;
    }


    private void assertTranslatedBundleComplete(String basePath) throws Exception
    {
        Properties english = loadPropertiesAtPath(basePath + ".properties");
        Properties spanish = loadPropertiesAtPath(basePath + "_es.properties");
        Properties french = loadPropertiesAtPath(basePath + "_fr.properties");
        Properties greek = loadPropertiesAtPath(basePath + "_el.properties");

        assertEquals(english.keySet(), spanish.keySet());
        assertEquals(english.keySet(), french.keySet());
        assertEquals(english.keySet(), greek.keySet());
    }


    private Properties loadPropertiesAtPath(String path) throws Exception
    {
        InputStream input = getClass().getResourceAsStream(path);
        assertNotNull("Missing resource " + path, input);

        Properties properties = new Properties();
        try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8))
        {
            properties.load(reader);
        }
        return properties;
    }
}
