package com.volta.tests;

import com.volta.core.ConfigurationReader;
import com.volta.core.FlowMethods;
import io.restassured.response.Response;
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

    @Test
    void testLinkCheckerLoop() throws IOException {
        final String clientId = ConfigurationReader.getProperty("Client-Id");

        System.out.println("FlowMethods.getIdRetrievedListSize() = " + FlowMethods.getIdRetrievedListSize());

        FlowMethods.checkAPIResponse(clientId);

        System.out.println("FlowMethods.getIdRetrievedListSize() = " + FlowMethods.getIdRetrievedListSize());


    }




}
