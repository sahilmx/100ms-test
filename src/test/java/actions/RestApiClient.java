package actions;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class RestApiClient {

    private boolean DEBUG_MODE;
    private String MANAGEMENT_TOKEN;

    public RestApiClient(String token) {
        // Get DEBUG_MODE value from system properties (defaulting to false if not set)
        String debugModeProperty = System.getProperty("DEBUG_MODE", "false");
        DEBUG_MODE = Boolean.parseBoolean(debugModeProperty);
        MANAGEMENT_TOKEN = token;
    }
    public Response sendPostRequest(String endpoint, String body) {
        RequestSpecification request = RestAssured.given();
        request.header("Authorization", "Bearer " + MANAGEMENT_TOKEN);
        request.header("Content-Type", "application/json");
        request.body(body);

        if (DEBUG_MODE) {
            request.log().all();
        }

        Response response = request.post(endpoint);

        if (DEBUG_MODE) {
            response.then().log().all();
        }

        return response;
    }
}
