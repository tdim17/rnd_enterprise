package com.linkvalidator.core;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class ConfigurationReader {


    private static final Properties properties = new Properties();

    static {

        try {
            String path = System.getProperty("Configuration.properties");

            InputStream input;

            if (path != null && !path.isBlank()) {
                // CI / Jenkins / external config
                input = new FileInputStream(path);
            } else {
                // local fallback (IntelliJ)
                input = new FileInputStream("Configuration.properties");
            }

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
