package com.linkvalidator.core;

import com.linkvalidator.pojo.LinkCheckItem;
import com.linkvalidator.utilities.NdJsonWriter;
import com.linkvalidator.utilities.Utils;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import lombok.Getter;

import java.io.IOException;
import java.util.List;

public class FlowMethods {

    @Getter
    private static int rawLinkListSize;
    @Getter
    private static int idTotalCount;
    @Getter
    private static int idRetrievedListSize;

    @Step("Getting auth token for client: {clientId}")
    public static String getToken(String clientId) {
        Response response = RestAssured.given()
                .accept("application/json")
                .header("Client-Id", clientId)
                .when()
                .get("getAuthToken");

        JsonPath jsonPath = response.jsonPath();
        return jsonPath.getString("token.val");
    }

    @Step("Fetching content data (limit={limitedIdNumber}, offset={offsetParam})")
    public static Response getResponse(String clientId, int limitedIdNumber, int offsetParam) {
        String tokenValue = getToken(clientId);

        return RestAssured.given()
                .accept("application/json")
                .header("Client-Id", clientId)
                .queryParam("limit", limitedIdNumber)
                .queryParam("offset", offsetParam)
                .header("Authorization", "Bearer " + tokenValue)
                .when()
                .get("exportContentVerify");
    }

    @Step("Extracting list of IDs from response")
    public static List<Integer> retrieveListOfAllId(Response response) {
        idTotalCount = response.path("itemsCount");

        List<Integer> listId = response.path("data.id");
        idRetrievedListSize = listId.size();

        return listId;
    }

    @Step("Extracting all links from response")
    public static List<LinkCheckItem> retrieveListOfAllLinks(Response response) {
        JsonPath jsPath = response.jsonPath();
        List<LinkCheckItem> listAll = Utils.retrieveAllHrefAndSrcToList(jsPath);
        rawLinkListSize = listAll.size();
        return listAll;
    }

    @Step("Validating links for broken URLs")
    public static void validate(List<LinkCheckItem> listAll) {
        Utils.itemLIstValidator(listAll);
    }

    @Step("Sending broken links report to API")
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

    @Step("Clearing NDJSON report file")
    public static void cleanNdJsonFile() {
        NdJsonWriter.clear();
    }
}
