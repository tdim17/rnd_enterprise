package com.volta.utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.volta.core.ConfigurationReader;
import com.volta.pojo.LinkCheckItem;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class NdJsonWriter {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String FILE_PATH = ConfigurationReader.getProperty("reportNdJson");

    public static void write(LinkCheckItem item) {
        try (BufferedWriter writer =
                     new BufferedWriter(new FileWriter(FILE_PATH, true))) {

            String jsonLine = mapper.writeValueAsString(item);
            writer.write(jsonLine);
            writer.newLine();

        } catch (IOException e) {
            throw new RuntimeException("Failed to write NDJSON", e);
        }
    }

    public static void clear() {
        try (FileWriter writer = new FileWriter(FILE_PATH, false)) {
            // просто открыли с append=false → файл обнулился
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
