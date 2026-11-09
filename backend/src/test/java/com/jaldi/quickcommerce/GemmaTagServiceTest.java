package com.jaldi.quickcommerce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class GemmaTagServiceTest {
    private HttpServer server;
    private AtomicReference<String> authorization;
    private AtomicReference<String> requestBody;
    private GemmaTagService service;

    @BeforeEach
    void startStubService() throws Exception {
        authorization = new AtomicReference<>();
        requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/generate-tags", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = tagResponse().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/api/chat", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {"message":{"content":"{\\"english\\":[\\"milk\\"],\\"hindi\\":[\\"दूध\\"]}"}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        service = new GemmaTagService(
                "remote",
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "test-token",
                "gemma3:4b",
                new ObjectMapper());
    }

    @AfterEach
    void stopStubService() {
        server.stop(0);
    }

    @Test
    void sendsAuthenticatedProductAndReturnsOnlyCleanRecognizedTags() {
        var tags = service.generateTags(new TagGenerationRequest("Amul milk", "Amul", "Dairy & eggs"));

        assertEquals("Bearer test-token", authorization.get());
        assertEquals("{\"name\":\"Amul milk\",\"brand\":\"Amul\",\"category\":\"Dairy & eggs\"}", requestBody.get());
        assertEquals(List.of("milk"), tags.get("english"));
        assertEquals(List.of("दूध"), tags.get("hindi"));
        assertFalse(tags.containsKey("unrecognized"));
    }

    @Test
    void asksLocalOllamaForJsonTags() {
        service = new GemmaTagService(
                "ollama",
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "",
                "gemma3:4b",
                new ObjectMapper());

        var tags = service.generateTags(new TagGenerationRequest("Amul milk", "Amul", "Dairy & eggs"));

        assertEquals(List.of("milk"), tags.get("english"));
        assertEquals(List.of("दूध"), tags.get("hindi"));
        assertTrue(requestBody.get().contains("\"model\":\"gemma3:4b\""));
        assertTrue(requestBody.get().contains("\"temperature\":0.1"));
        assertTrue(requestBody.get().contains("\"num_predict\":600"));
        assertTrue(requestBody.get().contains("\"repeat_penalty\":1.1"));
        assertTrue(requestBody.get().contains("\"num_ctx\":2048"));
        assertTrue(requestBody.get().contains("\"format\":"));
        assertTrue(requestBody.get().contains("\"type\":\"object\""));
        assertTrue(requestBody.get().contains(
                "\"required\":[\"english\",\"hinglish\",\"hindi\",\"marathi\",\"gujarati\",\"punjabi\",\"tamil\",\"telugu\",\"kannada\",\"malayalam\",\"typos\",\"aiWords\"]"));
        assertFalse(requestBody.get().contains("\"urdu\""));
        assertTrue(requestBody.get().contains("\"maxItems\":3"));
        assertTrue(requestBody.get().contains("\"additionalProperties\":false"));
    }

    private String tagResponse() {
        return """
                {"english":[" milk ","milk"],"hindi":["दूध"],"unrecognized":["ignore"]}
                """;
    }
}
