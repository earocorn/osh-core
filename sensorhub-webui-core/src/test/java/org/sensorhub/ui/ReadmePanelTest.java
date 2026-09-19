package org.sensorhub.ui;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import org.junit.Test;


public class ReadmePanelTest
{
    @Test
    public void selectsLocalizedReadmeBeforeEnglishFallback() throws Exception
    {
        assertEquals(
            Arrays.asList("i18n/README_es.md", "README_es.md", "i18n/README.md", "README.md"),
            ReadmePanel.getReadmeCandidates(new Locale("es", "MX")));

        try (InputStream input = ReadmePanel.openLocalizedReadme(ReadmePanelTest.class, new Locale("es")))
        {
            assertEquals("# Ayuda en español\n", new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }


    @Test
    public void fallsBackToEnglishWhenLocalizedReadmeIsMissing() throws Exception
    {
        try (InputStream input = ReadmePanel.openLocalizedReadme(ReadmePanelTest.class, Locale.FRENCH))
        {
            assertEquals("# English help\n", new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }


    @Test
    public void selectsGreekReadme() throws Exception
    {
        assertEquals(
            Arrays.asList("i18n/README_el.md", "README_el.md", "i18n/README.md", "README.md"),
            ReadmePanel.getReadmeCandidates(new Locale("el", "GR")));

        try (InputStream input = ReadmePanel.openLocalizedReadme(ReadmePanelTest.class, new Locale("el")))
        {
            assertEquals("# Βοήθεια στα ελληνικά\n", new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
