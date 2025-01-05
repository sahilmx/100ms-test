package dataproviders;

import org.testng.annotations.DataProvider;

import java.util.List;
import java.util.Map;

public class RecordingApiDataProvider {
    @DataProvider(name = "startRecordingData")
    public Object[][] startRecordingData() {
        return new Object[][]{
                {
                        Map.of(
                                "meeting_url", "https://example.com/meeting",
                                "resolution", Map.of(
                                        "width", 1280,
                                        "height", 720
                                ),
                                "transcription", Map.of(
                                        "enabled", true,
                                        "output_modes", List.of("txt", "srt", "json"),
                                        "custom_vocabulary", List.of("100ms", "WebSDK", "Flutter", "Sundar", "Pichai", "DALL-E"),
                                        "summary", Map.of(
                                                "enabled", true,
                                                "context", "this is a general call",
                                                "sections", List.of(
                                                        Map.of("title", "Agenda", "format", "bullets"),
                                                        Map.of("title", "Key Points", "format", "bullets"),
                                                        Map.of("title", "Action Items", "format", "bullets"),
                                                        Map.of("title", "Short Summary", "format", "paragraph")
                                                ),
                                                "temperature", 0.5
                                        )
                                )
                        )
                }
        };
    }
}
