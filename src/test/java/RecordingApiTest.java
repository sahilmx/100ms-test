import actions.RestApiClient;
import dataproviders.RecordingApiDataProvider;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class RecordingApiTest {

    private String BASE_URL;
    private String MANAGEMENT_TOKEN;
    private String ROOM_ID;
    private String MEETING_URL;
    private String ANOTHER_MEETING_URL;

    private Map<String, Object> startRecordingResponse; // To store start recording response
    public RestApiClient restApiClient;

    @BeforeClass
    public void setup() throws IOException {
        Properties envProperties = new Properties();
        FileInputStream envFile = new FileInputStream("src/test/resources/env.properties");
        envProperties.load(envFile);
        BASE_URL = envProperties.getProperty("BASE_URL");
        MANAGEMENT_TOKEN = envProperties.getProperty("MANAGEMENT_TOKEN");
        ROOM_ID = envProperties.getProperty("ROOM_ID");
        MEETING_URL = envProperties.getProperty("MEETING_URL");
        ANOTHER_MEETING_URL = envProperties.getProperty("ANOTHER_MEETING_URL");
        restApiClient= new RestApiClient(MANAGEMENT_TOKEN);
        envFile.close();
        RestAssured.baseURI = BASE_URL;
    }

    @Test(testName = "Start recording API test", dataProvider = "startRecordingData", dataProviderClass = RecordingApiDataProvider.class, priority = 1)
    public void testStartRecording(Map<String, Object> requestBody) {
        // Convert requestBody to JSON string
        String jsonRequestBody = new com.google.gson.Gson().toJson(requestBody);

        // Send POST request to start recording API
        Response response = restApiClient.sendPostRequest("/v2/recordings/room/" + ROOM_ID + "/start", jsonRequestBody);

        // Assert the API response status code
        Assert.assertEquals(response.getStatusCode(), 200, "Start recording should succeed.");

        // Store the response for further assertions
        startRecordingResponse = response.jsonPath().getMap("$");

        // Additional assertions
        Assert.assertNotNull(startRecordingResponse.get("id"), "Response should contain 'id'.");
        Assert.assertNotNull(startRecordingResponse.get("room_id"), "Response should contain 'room_id'.");
        Assert.assertEquals(startRecordingResponse.get("room_id"), ROOM_ID, "'room_id' should match the requested room ID.");

        Assert.assertNotNull(startRecordingResponse.get("status"), "Response should contain 'status'.");
        Assert.assertEquals(startRecordingResponse.get("status"), "starting", "'status' should be 'starting'.");

        Assert.assertNotNull(startRecordingResponse.get("created_at"), "Response should contain 'created_at'.");
        Assert.assertEquals(startRecordingResponse.get("meeting_url"), requestBody.get("meeting_url"), "'meeting_url' should match the request.");

        // Assert asset_types from response (if included)
        Object assetTypes = startRecordingResponse.get("asset_types");
        Assert.assertTrue(assetTypes instanceof java.util.List, "'asset_types' should be a list.");
        Assert.assertTrue(((java.util.List<?>) assetTypes).contains("room-composite"), "'asset_types' should include 'room-composite'.");
        Assert.assertTrue(((java.util.List<?>) assetTypes).contains("chat"), "'asset_types' should include 'chat'.");

        // Assert transcription summary configuration if enabled
        Map<String, Object> transcription = (Map<String, Object>) requestBody.get("transcription");
        if ((boolean) transcription.get("enabled")) {
            Assert.assertTrue(((java.util.List<?>) assetTypes).contains("transcript"), "'asset_types' should include 'transcript' when transcription is enabled.");
            Assert.assertTrue(((java.util.List<?>) assetTypes).contains("summary"), "'asset_types' should include 'summary' when transcription summary is enabled.");
        }
    }

    @Test(testName = "Stop Recording API Test", priority = 2)
    public void testStopRecordingAndVerify() {
        // Send POST request to stop recording API
        Response stopResponse = restApiClient.sendPostRequest("/v2/recordings/room/" + ROOM_ID + "/stop", "{}");

        // Assert the API response status code
        Assert.assertEquals(stopResponse.getStatusCode(), 200, "Stop recording should succeed.");

        // Extract data from the stop recording response
        Map<String, Object> stopRecordingData = stopResponse.jsonPath().getMap("data[0]");

        // Assertions for mandatory fields in the stop response
        Assert.assertNotNull(stopRecordingData, "Stop recording response data should not be null.");
        Assert.assertNotNull(stopRecordingData.get("id"), "Response should contain 'id'.");
        Assert.assertNotNull(stopRecordingData.get("room_id"), "Response should contain 'room_id'.");
        Assert.assertNotNull(stopRecordingData.get("session_id"), "Response should contain 'session_id'.");
        Assert.assertNotNull(stopRecordingData.get("status"), "Response should contain 'status'.");
        Assert.assertNotNull(stopRecordingData.get("created_at"), "Response should contain 'created_at'.");

        // Cross-validate with the start recording response
        Assert.assertNotNull(startRecordingResponse, "Start recording response should not be null.");
        Assert.assertEquals(stopRecordingData.get("room_id"), startRecordingResponse.get("room_id"), "Room ID should match.");
        Assert.assertEquals(stopRecordingData.get("meeting_url"), startRecordingResponse.get("meeting_url"), "Meeting URL should match.");
        Assert.assertEquals(stopRecordingData.get("destination"), startRecordingResponse.get("destination"), "Destination should match.");

        // Validate status progression
        Assert.assertEquals(stopRecordingData.get("status"), "stopping", "Status should be 'stopping'.");

        // Asset Types Validation
        Object stopAssetTypes = stopRecordingData.get("asset_types");
        Assert.assertTrue(stopAssetTypes instanceof java.util.List, "'asset_types' should be a list.");
        Assert.assertEquals(stopAssetTypes, startRecordingResponse.get("asset_types"), "Asset types should match between start and stop responses.");

        // Validate timestamps (if available)
        String stopUpdatedAt = (String) stopRecordingData.get("updated_at");
        String stopCreatedAt = (String) stopRecordingData.get("created_at");
        Assert.assertNotNull(stopCreatedAt, "'created_at' should not be null.");
        Assert.assertNotNull(stopUpdatedAt, "'updated_at' should not be null.");
        Assert.assertTrue(stopUpdatedAt.compareTo(stopCreatedAt) >= 0, "'updated_at' should be after or equal to 'created_at'.");

        // Ensure no unexpected nulls in stop response
        Assert.assertNull(stopRecordingData.get("stopped_at"), "'stopped_at' should be null until recording is completely stopped.");
        Assert.assertNotNull(stopRecordingData.get("created_at"), "'created_at' should not be null.");

        // Validate fields such as 'started_by' and 'stopped_by' (if applicable)
        Assert.assertTrue(
                stopRecordingData.get("started_by").toString().isEmpty() || stopRecordingData.get("started_by") instanceof String,
                "'started_by' should be empty or a valid string."
        );
        Assert.assertTrue(
                stopRecordingData.get("stopped_by").toString().isEmpty() || stopRecordingData.get("stopped_by") instanceof String,
                "'stopped_by' should be empty or a valid string."
        );
    }



}
