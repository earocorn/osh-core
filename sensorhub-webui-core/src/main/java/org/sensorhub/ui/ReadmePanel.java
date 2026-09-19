package org.sensorhub.ui;

import static org.sensorhub.ui.AdminI18n.tr;

import com.vaadin.annotations.JavaScript;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.JavaScriptComponentState;
import com.vaadin.ui.*;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.ui.api.UIConstants;
import org.sensorhub.ui.data.MyBeanItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReadmePanel extends VerticalLayout {

    // This determines which tab is visible
    // Hack needed for desired accordion behavior in this older version of Vaadin
    private boolean visibleTab = false;

    @JavaScript({"vaadin://js/jquery.min.js", "vaadin://js/lodash.min.js", "vaadin://js/backbone.min.js", "vaadin://js/joint.js", "vaadin://js/marked.min.js", "vaadin://js/readme.js"})
    public class ReadmeJS extends AbstractJavaScriptComponent {
        private static final Logger logger = LoggerFactory.getLogger(ReadmePanel.class);
        private boolean hasContent = false;

        public static class ReadmeState extends JavaScriptComponentState {
            public String readmeText;
        }

        private ReadmeJS(final MyBeanItem<ModuleConfig> beanItem) {
            try (InputStream readmeIs = openLocalizedReadme(
                beanItem.getBean().getClass(), AdminI18n.getCurrentLocale()))
            {
                if (readmeIs == null)
                {
                    hasContent = false;
                }
                else
                {
                    hasContent = true;
                    getState().readmeText = new String(readmeIs.readAllBytes(), StandardCharsets.UTF_8);
                    markAsDirty();
                }
            }
            catch (Exception e)
            {
                logger.error("Error reading readme file", e);
            }
        }

        @Override
        protected ReadmeState getState() {
            return (ReadmeState) super.getState();
        }

        public boolean hasContent() {
            return hasContent;
        }
    }

    public ReadmePanel(final MyBeanItem<ModuleConfig> beanItem) {
        ReadmeJS readmeJS = new ReadmeJS(beanItem);
        if (readmeJS.hasContent()) {
            // Use JS markdown parser if a readme exists
            addComponent(readmeJS);
        } else {
            // Otherwise, display instructions for adding a readme file
            var header = new HorizontalLayout();
            header.setSpacing(true);
            Label title = new Label(tr("section.noReadme"));
            title.addStyleName(UIConstants.STYLE_H2);
            header.addComponent(title);
            addComponent(header);

            Button detailsBtn = new Button(tr("action.detailedInstructions"));
            detailsBtn.setIcon(FontAwesome.CARET_RIGHT);
            //detailsBtn.setWidth(100.0f, Unit.PERCENTAGE);

            VerticalLayout instructions = new VerticalLayout();
            instructions.setMargin(true);
            instructions.setSpacing(true);
            Label instructionsLabel = new Label(generateInstructions(beanItem), ContentMode.HTML);
            instructions.addComponent(instructionsLabel);
            instructions.setVisible(false);
            instructions.addStyleNames("v-csslayout-well", "v-scrollable");

            detailsBtn.addClickListener(event -> {
                if (visibleTab) {
                    detailsBtn.setIcon(FontAwesome.CARET_RIGHT);
                    instructions.setVisible(false);
                    visibleTab = false;
                } else {
                    detailsBtn.setIcon(FontAwesome.CARET_DOWN);
                    instructions.setVisible(true);
                    visibleTab = true;
                }
            });

            addComponent(detailsBtn);
            addComponent(instructions);

        }
    }

    private String generateInstructions(final MyBeanItem<ModuleConfig> beanItem) {
        String packagePath = beanItem.getBean().getClass().getPackage().getName().replace(".", "/");
        String resourcePath = "src/main/resources/" + packagePath + "/i18n";
        List<String> localizedReadmes = new ArrayList<>();
        StringBuilder readmeFiles = new StringBuilder(resourcePath + "/README.md\n");
        for (Locale locale: AdminI18n.getSupportedLocales())
        {
            if (Locale.ENGLISH.getLanguage().equals(locale.getLanguage()))
                continue;

            String fileName = "README_" + locale.getLanguage() + ".md";
            localizedReadmes.add("<code>" + fileName + "</code>");
            readmeFiles.append(resourcePath).append('/').append(fileName).append('\n');
        }

        return "<p>" + tr("readme.missing.intro") + "</p>\n" +
                "<p>" + tr("readme.missing.location", "<code>" + resourcePath + "/README.md</code>") + "<br>\n" +
                tr("readme.missing.localized", String.join(", ", localizedReadmes)) + "</p>" +
                "<p>" + tr("readme.missing.build") + "</p>\n" +
                "<pre>\n" +
                readmeFiles +
                "</pre>\n";
    }


    static InputStream openLocalizedReadme(Class<?> moduleConfigClass, Locale locale)
    {
        for (String resourceName: getReadmeCandidates(locale))
        {
            InputStream input = moduleConfigClass.getResourceAsStream(resourceName);
            if (input != null)
                return input;
        }

        return null;
    }


    static List<String> getReadmeCandidates(Locale locale)
    {
        String language = AdminI18n.normalize(locale).getLanguage();
        List<String> candidates = new ArrayList<>();
        if (!Locale.ENGLISH.getLanguage().equals(language))
        {
            candidates.add("i18n/README_" + language + ".md");
            candidates.add("README_" + language + ".md");
        }
        candidates.add("i18n/README.md");
        candidates.add("README.md");
        return candidates;
    }
}
