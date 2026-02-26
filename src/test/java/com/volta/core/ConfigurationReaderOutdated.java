package com.volta.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigurationReaderOutdated {

    // 1 - Create the Properties object (create an object)
    /*  make it "private" so we are limiting access to the object
        make it "static" to be sure it's created and loaded before everything else. */

    private static Properties properties = new Properties();

    static{

        try {
            //2 - open file from current working directory (cwd)
            FileInputStream file = new FileInputStream("Сonfiguration.properties");
            //3 - Load the "properties" object with "file" (load properties)
            properties.load(file);

            //close the file in the memory
            file.close();

        } catch (IOException e) {
            System.out.println("FILE NOT FOUND WITH GIVEN PATH!!!");
            e.printStackTrace();
        }
    }

    // Create a utility method to use the object to read
    //4 - Use "properties" object to read from the file (read properties)

    public static String getProperty(String keyword){
        return properties.getProperty(keyword);     // That is not the same as getProperty(String keyword) the line before. Just a similar name :)
    }

}
