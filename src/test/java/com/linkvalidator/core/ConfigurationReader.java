package com.linkvalidator.core;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class ConfigurationReader {

    private static final Properties properties = new Properties();

    static {
        try {
            String path = System.getProperty("Configuration.properties");

            InputStream input = (path != null && !path.isBlank())
                    ? new FileInputStream(path)           // CI / Jenkins
                    : new FileInputStream("Configuration.properties"); // local

            properties.load(input);
            input.close();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration properties", e);
        }
    }

    public static String getProperty(String keyword) {
        return properties.getProperty(keyword);
    }
}
