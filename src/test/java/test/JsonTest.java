package test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Simple JSON Serialization Test
 * Test if JSON storage functionality works
 */
public class JsonTest {

    public static void main(String[] args) {
        System.out.println("=== JSON Serialization Test ===");

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            // Test simple object serialization
            TestRepository repo = new TestRepository();
            repo.id = 1L;
            repo.name = "Test Repository";
            repo.url = "https://github.com/test/repo.git";
            repo.createTime = LocalDateTime.now();

            String json = mapper.writeValueAsString(repo);
            System.out.println("Serialized JSON:");
            System.out.println(json);

            // Test deserialization
            TestRepository deserialized = mapper.readValue(json, TestRepository.class);
            System.out.println("\nDeserialized object:");
            System.out.println("ID: " + deserialized.id);
            System.out.println("Name: " + deserialized.name);
            System.out.println("URL: " + deserialized.url);
            System.out.println("Create Time: " + deserialized.createTime);

            // Test list serialization
            List<TestRepository> repos = Arrays.asList(repo);
            String listJson = mapper.writeValueAsString(repos);
            System.out.println("\nList JSON:");
            System.out.println(listJson);

            System.out.println("\nSUCCESS: JSON serialization works correctly!");

        } catch (Exception e) {
            System.err.println("FAILED: JSON test failed:");
            e.printStackTrace();
        }
    }

    public static class TestRepository {
        public Long id;
        public String name;
        public String url;
        public LocalDateTime createTime;

        public TestRepository() {
            // Default constructor for Jackson
        }
    }
}