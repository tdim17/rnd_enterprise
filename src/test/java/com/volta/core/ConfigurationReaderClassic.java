package com.volta.core;

import java.io.InputStream;
import java.util.Properties;

public class ConfigurationReaderClassic {

    private static final Properties properties = new Properties();

    static {
        // load resource from classpath
        try (InputStream file =
                     ConfigurationReaderClassic.class
                             .getClassLoader()
                             .getResourceAsStream("Configuration.properties")) {

            if (file == null) {
                throw new RuntimeException(
                        "Сonfiguration.properties not found in classpath"
                );
            }

            properties.load(file);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to load Сonfiguration.properties", e
            );
        }
    }

    public static String getProperty(String keyword) {
        return properties.getProperty(keyword);
    }

}
