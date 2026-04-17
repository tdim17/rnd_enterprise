package com.linkvalidator.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.linkvalidator.core.ConfigurationReader;
import com.linkvalidator.pojo.ExportItem;
import com.linkvalidator.utilities.Utils;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import lombok.Getter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class DraftMethods {



    // ----------- Separate Methods for practicing --------------------------------------------------------------------

    private static final String clientId = ConfigurationReader.getProperty("Client-Id");
    private static String tokenValue = "";

    @Getter
    private static int idTotalCount = 0;


    @Test
    void getToken() {

        RestAssured.baseURI = ConfigurationReader.getProperty("baseURI");

        Response response = RestAssured.given()
                .accept("application/json")
                .header("Client-Id", clientId)
                .when()
                .get("getAuthToken");

        // System.out.println("Status Code: " + response.statusCode());
        // response.prettyPrint();

        JsonPath jsonPath = response.jsonPath();
        tokenValue = jsonPath.getString("token.val");

        System.out.println("tokenValue = " + tokenValue);

        // >>>>>>>>>>>>> System.out.println("------------------------------");
    }


    @Test
    void getResponse() {

        int limitNumber = 100;

        getToken();
        System.out.println("Token: " + tokenValue);

        Response response = RestAssured.given()
                .accept("application/json")
                .header("Client-Id", clientId)
                .queryParam("limit", limitNumber)
                .queryParam("offset", 4500)
                .queryParam("id", 3)  // retrieving the single object of Response
                .header("Authorization", "Bearer " + tokenValue)
                .when()
                .get("exportContentVerify");

//        response.prettyPrint();
//        System.out.println("response.body().asString() = " + response.body().asPrettyString());

        idTotalCount = response.path("itemsCount");
        System.out.println("Total Amount of ID     = " + idTotalCount);

        List<Integer> listId = response.path("data.id");
        // System.out.println("objectId = " + listId);
        System.out.println("Retrieved Amount of ID = " + listId.size());

//        JsonPath jsonPath = response.jsonPath();
//        List<Object> list = jsonPath.getList("data.id");
//        System.out.println("listId.size() = " + list.size());

    }

    @Test
    void createPayload() throws JsonProcessingException {
        String ndJsonName = ConfigurationReader.getProperty("reportNdJson");
        System.out.println(Utils.payloadFromNdJson(ndJsonName));
    }

    @Test
    void readPayload() {
        String ndJsonName = ConfigurationReader.getProperty("reportNdJson");
        List<ExportItem> exportItems = Utils.readFromNdJson(ndJsonName);
        // System.out.println(exportItems);
        exportItems.forEach(System.out::println);
    }


    // Counts number of IDs
    @Test
    void countPayloadSize() throws JsonProcessingException {
        String ndJsonName = ConfigurationReader.getProperty("reportNdJson");
        System.out.println("Payload ID-amount " + Utils.countIdNumbers(ndJsonName));
    }

    // Counts number of non-empty Lines from NDJson file
    @Test
    void countPayloadLinksNumber() throws IOException {
        System.out.println("Broken links Number in report = " + Utils.countLines(ConfigurationReader.getProperty("reportNdJson")));
    }


}
