package com.volta.tests;

import com.volta.core.ConfigurationReader;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;

public class BaseTest {

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = ConfigurationReader.getProperty("baseURI");
        RestAssured.filters(new AllureRestAssured());
    }


}
