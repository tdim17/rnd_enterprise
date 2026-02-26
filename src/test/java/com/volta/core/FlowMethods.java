package com.volta.core;

import com.volta.pojo.LinkCheckItem;
import com.volta.utilities.NdJsonWriter;
import com.volta.utilities.Utils;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import lombok.Getter;

import java.io.IOException;
import java.util.List;

public class FlowMethods {

    @Getter
    private static int rawLinkListSize = 0;
    @Getter
    private static int idTotalCount = 0;
    @Getter
    private static int idRetrievedListSize = 0;


    public static String getToken(String clientId) {

        RestAssured.baseURI = ConfigurationReader.getProperty("baseURI");

        Response response = RestAssured.given()
                .accept("application/json")
                .header("Client-Id", clientId)
                .when()
                .get("getAuthToken");

        // System.out.println("Status Code: " + response.statusCode());
        // response.prettyPrint();

        JsonPath jsonPath = response.jsonPath();
        String tokenValue = jsonPath.getString("token.val");
        // System.out.println("tokenValue = " + tokenValue);

        return tokenValue;
    }

    public static void checkAPIResponse(String clientId) {

        int limitedIdNumber = Integer.parseInt(ConfigurationReader.getProperty("limitedIdNumberInResponse"));
        int offsetParam = Integer.parseInt(ConfigurationReader.getProperty("offsetParam"));

        String tokenValue = getToken(clientId);

        Response response = RestAssured.given()
                .accept("application/json")
                .header("Client-Id", clientId)
                .queryParam("limit", limitedIdNumber)
                .queryParam("offset", offsetParam) // я закончил на id=1000 (меньше всего ошибок с 1500 по 2000 => 0 Broken links. Всего было 4987, сейчас 4989!!)
                //.queryParam("id", 3)
                .header("Authorization", "Bearer " + tokenValue)
                .when()
                .get("exportContentVerify");

        // response.prettyPrint();
        // System.out.println("response.body().asString() = " + response.body().asPrettyString());
        // response.body();

        // Make a parsing of the string:
        JsonPath jsPath = response.jsonPath();
        // System.out.println("jsPath.prettyPrint() = " + jsPath.prettyPrint());

        idTotalCount = response.path("itemsCount");
        // System.out.println("Total Amount of ID     = " + itemsCount);

        List<Integer> listId = response.path("data.id");
        idRetrievedListSize = listId.size();
        System.out.println("Retrieved Amount of ID = " + idRetrievedListSize);


        List<LinkCheckItem> listAll = Utils.retrieveAllHrefAndSrcToList(jsPath);
        rawLinkListSize = listAll.size();
        System.out.println("Raw URLs list Size = " + rawLinkListSize);
        System.out.println("--------------------------------");
        // System.out.println("listAll: " + listAll);

        // Different options to use:
        // Utils.itemLIstExtractor(listAll);
        // Utils.itemLIstUniquesExtractor(listAll);
        // Enterprise validator:
        Utils.itemLIstValidator(listAll);
    }

    public static void sendReportToAPI(String clientId) throws IOException {

        String ndJsonName = ConfigurationReader.getProperty("reportNdJson");
        String payloadBody = Utils.payloadFromNdJson(ndJsonName);

        System.out.println("Payload ID-size     = " + Utils.countIdNumbers(ndJsonName));
        System.out.println("Broken links inside = " + Utils.countLines(ConfigurationReader.getProperty("reportNdJson")));

        String tokenValue = getToken(clientId);

        Response response = RestAssured.given()
                .accept("application/json")
                .contentType(ContentType.JSON)
                .header("Client-Id", clientId)
                .header("Authorization", "Bearer " + tokenValue)
                .body(payloadBody)
                .when()
                .post("importContentVerify");

        int statusCode = response.getStatusCode();
        System.out.println("Status Code = " + statusCode);
        System.out.println(response.body().prettyPrint());
    }


    public static void cleanNdJsonFile() {
        NdJsonWriter.clear();
    }


}
