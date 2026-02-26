package com.volta.tests;

import com.volta.core.ConfigurationReader;
import com.volta.core.FlowMethods;
import org.junit.jupiter.api.Test;
import java.io.IOException;

public class TestBrokenLinks {

    @Test
    void testLinkChecker() throws IOException {

        final String clientId = ConfigurationReader.getProperty("Client-Id");

        FlowMethods.getToken(clientId);
        FlowMethods.checkAPIResponse(clientId);
        FlowMethods.sendReportToAPI(clientId);
    }

//    @Test
//    void clearNdJson(){
//        FlowMethods.cleanNdJsonFile();
//        System.out.println("NDJson file is cleaned!");
//    }

}
