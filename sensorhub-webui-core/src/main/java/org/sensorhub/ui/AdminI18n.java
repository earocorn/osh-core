/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 ******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;


/**
 * Translation access for the server-rendered administration interface.
 */
public final class AdminI18n
{
    public static final String SESSION_LOCALE_ATTRIBUTE = "osh.admin.locale";
    public static final String LANGUAGE_COOKIE = "osh-language";
    private static final String BUNDLE_NAME = "org.sensorhub.ui.i18n.AdminMessages";
    private static final List<Locale> SUPPORTED_LOCALES = Arrays.asList(
        Locale.ENGLISH,
        new Locale("es"),
        Locale.FRENCH,
        new Locale("el")
    );


    private AdminI18n()
    {
    }


    public static List<Locale> getSupportedLocales()
    {
        return SUPPORTED_LOCALES;
    }


    public static Locale initializeLocale(UI ui, VaadinRequest request)
    {
        Object savedLocale = VaadinSession.getCurrent().getAttribute(SESSION_LOCALE_ATTRIBUTE);
        Locale cookieLocale = readCookieLocale(request);
        Locale requestedLocale = cookieLocale != null ? cookieLocale :
            savedLocale instanceof Locale ? (Locale)savedLocale : request.getLocale();
        Locale locale = normalize(requestedLocale);
        VaadinSession.getCurrent().setAttribute(SESSION_LOCALE_ATTRIBUTE, locale);
        ui.setLocale(locale);
        return locale;
    }


    static Locale readCookieLocale(VaadinRequest request)
    {
        String cookieHeader = request.getHeader("Cookie");
        if (cookieHeader == null)
            return null;

        for (String cookie: cookieHeader.split(";"))
        {
            String[] parts = cookie.trim().split("=", 2);
            if (parts.length == 2 && LANGUAGE_COOKIE.equals(parts[0]))
            {
                Locale cookieLocale = Locale.forLanguageTag(parts[1]);
                for (Locale supportedLocale: SUPPORTED_LOCALES)
                {
                    if (supportedLocale.getLanguage().equals(cookieLocale.getLanguage()))
                        return supportedLocale;
                }
            }
        }

        return null;
    }


    public static void saveLocale(UI ui, Locale locale)
    {
        Locale normalizedLocale = normalize(locale);
        VaadinSession.getCurrent().setAttribute(SESSION_LOCALE_ATTRIBUTE, normalizedLocale);
        ui.setLocale(normalizedLocale);
    }


    public static Locale normalize(Locale locale)
    {
        if (locale != null)
        {
            for (Locale supportedLocale: SUPPORTED_LOCALES)
            {
                if (supportedLocale.getLanguage().equals(locale.getLanguage()))
                    return supportedLocale;
            }
        }

        return Locale.ENGLISH;
    }


    public static String languageName(Locale locale)
    {
        Locale normalizedLocale = normalize(locale);
        return normalizedLocale.getDisplayLanguage(normalizedLocale);
    }


    public static String tr(String key, Object... args)
    {
        return tr(getCurrentLocale(), key, args);
    }


    public static String tr(Locale locale, String key, Object... args)
    {
        Locale normalizedLocale = normalize(locale);

        try
        {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, normalizedLocale);
            String message = bundle.getString(key);
            if (args != null)
            {
                for (int i = 0; i < args.length; i++)
                    message = message.replace("{" + i + "}", String.valueOf(args[i]));
            }
            return message;
        }
        catch (MissingResourceException e)
        {
            return key;
        }
    }


    /**
     * Translates configuration metadata stored beside the configuration class.
     * Bundles follow the convention {@code <config-package>.i18n.ConfigMessages}
     * and keys are prefixed with the class name, including enclosing classes.
     */
    public static String trConfig(Class<?> configClass, String keySuffix, String fallback)
    {
        return trConfig(getCurrentLocale(), configClass, keySuffix, fallback);
    }


    public static String trConfig(Locale locale, Class<?> configClass, String keySuffix, String fallback)
    {
        if (configClass == null || keySuffix == null)
            return fallback;

        Package configPackage = configClass.getPackage();
        if (configPackage == null)
            return fallback;

        String packageName = configPackage.getName();
        String bundleName = packageName + ".i18n.ConfigMessages";
        String relativeClassName = configClass.getName()
            .substring(packageName.length() + 1)
            .replace('$', '.');
        String key = relativeClassName + "." + keySuffix;

        try
        {
            ResourceBundle bundle = ResourceBundle.getBundle(
                bundleName,
                normalize(locale),
                configClass.getClassLoader());
            return bundle.containsKey(key) ? bundle.getString(key) : fallback;
        }
        catch (MissingResourceException e)
        {
            return fallback;
        }
    }


    public static Locale getCurrentLocale()
    {
        UI ui = UI.getCurrent();
        Locale locale = ui != null ? normalize(ui.getLocale()) : null;
        VaadinSession session = VaadinSession.getCurrent();
        if (locale == null && session != null)
        {
            Object savedLocale = session.getAttribute(SESSION_LOCALE_ATTRIBUTE);
            if (savedLocale instanceof Locale)
                locale = normalize((Locale)savedLocale);
        }

        return locale != null ? locale : Locale.ENGLISH;
    }
}
