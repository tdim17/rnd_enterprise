package com.linkvalidator.tests;

import com.linkvalidator.core.ConfigurationReader;
import com.linkvalidator.core.FlowMethods;
import com.linkvalidator.pojo.LinkCheckItem;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class TestBrokenLinks extends BaseTest {

    @Test
    void testLinkChecker() throws IOException {

        FlowMethods.cleanNdJsonFile();

        final String clientId = ConfigurationReader.getProperty("Client-Id");

        int idNumberLimit = Integer.parseInt(ConfigurationReader.getProperty("idNumberInResponseLimit"));
        int offsetParam = Integer.parseInt(ConfigurationReader.getProperty("offsetParam"));
        int iterationsLimit = Integer.parseInt(ConfigurationReader.getProperty("iterationsLimit"));

        int counter = 1;

        System.out.println("OffsetParam = " + offsetParam);
        System.out.println("ID-Numbers limit = " + idNumberLimit);
        System.out.println("Iterations limit = " + iterationsLimit);
        System.out.println("====================================================================");
        System.out.println();


        while (true) {
            System.out.println("Iteration #" + counter);
            System.out.println("Unique URLs List:");
            Response response = FlowMethods.getResponse(clientId, idNumberLimit, offsetParam);

            List<Integer> listOfAllId = FlowMethods.retrieveListOfAllId(response);

            if (listOfAllId.isEmpty() ) {
                System.err.println("List ID is empty --> nothing to validate");
                break;
            } else if (counter > iterationsLimit) {
                System.err.println("Stopped due to iteration limit");
                break;
            }

            List<LinkCheckItem> listOfAllLinks = FlowMethods.retrieveListOfAllLinks(response);
            FlowMethods.validate(listOfAllLinks);

            offsetParam += idNumberLimit;
            counter++;
        }

        System.out.println("Total Amount of ID = " + FlowMethods.getIdTotalCount());

        FlowMethods.sendReportToAPI(clientId);

    }

}
