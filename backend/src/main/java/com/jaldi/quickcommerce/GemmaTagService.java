package com.jaldi.quickcommerce;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class GemmaTagService {
    private static final Map<String, String> LANGUAGES = Map.ofEntries(
            Map.entry("english", "English"), Map.entry("hinglish", "Hinglish"),
            Map.entry("hindi", "Hindi"), Map.entry("marathi", "Marathi"),
            Map.entry("bengali", "Bengali"), Map.entry("assamese", "Assamese"),
            Map.entry("gujarati", "Gujarati"), Map.entry("punjabi", "Punjabi"),
            Map.entry("odia", "Odia"), Map.entry("tamil", "Tamil"),
            Map.entry("telugu", "Telugu"), Map.entry("kannada", "Kannada"),
            Map.entry("malayalam", "Malayalam"),
            Map.entry("typos", "common misspellings"),
            Map.entry("aiWords", "related shopper search phrases"));
    private static final List<String> REQUIRED_LANGUAGES = List.of(
            "english", "hinglish", "hindi", "marathi", "gujarati", "punjabi",
            "tamil", "telugu", "kannada", "malayalam", "typos", "aiWords");
    private static final Map<String, Object> TAG_SCHEMA = createTagSchema();

    private final String provider;
    private final String serviceUrl;
    private final String serviceToken;
    private final String model;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GemmaTagService(
            @Value("${jaldi.gemma.provider:ollama}") String provider,
            @Value("${jaldi.gemma.url:http://localhost:11434}") String serviceUrl,
            @Value("${jaldi.gemma.token:}") String serviceToken,
            @Value("${jaldi.gemma.model:gemma3:4b}") String model,
            ObjectMapper objectMapper) {
        this.provider = provider;
        this.serviceUrl = serviceUrl.replaceAll("/+$", "");
        this.serviceToken = serviceToken;
        this.model = model;
        this.objectMapper = objectMapper;

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(180));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public Map<String, List<String>> generateTags(TagGenerationRequest input) {
        if (serviceUrl.isBlank() || ("remote".equals(provider) && serviceToken.isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Gemma tag service is not configured.");
        }

        try {
            Map<String, Object> response = "remote".equals(provider)
                    ? generateWithRemoteService(input)
                    : generateWithOllama(input);
            return cleanTags(response);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Gemma tag service request failed.", exception);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Gemma returned invalid tag data.", exception);
        }
    }

    private Map<String, Object> generateWithOllama(TagGenerationRequest input) {
        Map<String, Object> response = restClient.post()
                .uri(serviceUrl + "/api/chat")
                .body(Map.of(
                        "model", model,
                        "messages", List.of(
                                Map.of("role", "system", "content", buildSystemPrompt()),
                                Map.of("role", "user", "content", buildUserPrompt(input))),
                        "format", TAG_SCHEMA,
                        "stream", false,
                        "options", Map.of(
                                "temperature", 0.1,
                                "num_predict", 600,
                                "repeat_penalty", 1.1,
                                "num_ctx", 2048)))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        Object message = response == null ? null : response.get("message");
        if (!(message instanceof Map<?, ?> messageData) || !(messageData.get("content") instanceof String generated)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemma returned an empty response.");
        }
        try {
            return objectMapper.readValue(generated, new TypeReference<>() {});
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemma returned invalid JSON tags.", exception);
        }
    }

    private Map<String, Object> generateWithRemoteService(TagGenerationRequest input) {
        Map<String, List<String>> response = restClient.post()
                .uri(serviceUrl + "/generate-tags")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken)
                .body(input)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemma returned an empty response.");
        }
        return new LinkedHashMap<>(response);
    }

    private String buildSystemPrompt() {
        String languages = LANGUAGES.entrySet().stream()
                .map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
                .collect(java.util.stream.Collectors.joining(", "));
        return "Generate useful search tags for this exact grocery product for Indian shoppers. "
                + "Return only a JSON object with these keys: " + languages + ". "
                + "Every value must be an array of up to 3 short strings, following the provided JSON schema. "
                + "Always generate tags for Marathi, Gujarati, Punjabi, Tamil, Telugu, Kannada, and Malayalam. "
                + "Use each language's native script for those tags, and never leave those language arrays empty. "
                + "Include common transliterations, alternate spellings, and natural need-based search phrases "
                + "that shoppers might use when asking for this product. Use an empty array if unsure about a translation. "
                + "Do not invent product variants or product claims, and never include unrelated products. "
                + "Every tag must be a synonym, translation, misspelling, or need-based phrase for this exact product. "
                + "Keep Hinglish tags in Roman script. Never return offensive or insulting terms. "
                + "Keep each language concise; omit optional language arrays when you cannot provide accurate tags.";
    }

    private String buildUserPrompt(TagGenerationRequest input) {
        return "Product name: " + input.name() + "\n"
                + "Brand: " + (input.brand() == null ? "" : input.brand()) + "\n"
                + "Category: " + (input.category() == null ? "" : input.category());
    }

    private static Map<String, Object> createTagSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (String language : LANGUAGES.keySet()) {
            properties.put(language, Map.of(
                    "type", "array",
                    "items", Map.of("type", "string"),
                    "maxItems", 3));
        }
        return Map.of(
                "type", "object",
                "properties", properties,
                "required", REQUIRED_LANGUAGES,
                "additionalProperties", false);
    }

    private Map<String, List<String>> cleanTags(Map<String, Object> response) {
        Map<String, List<String>> cleaned = new LinkedHashMap<>();
        for (String language : LANGUAGES.keySet()) {
            Object value = response.get(language);
            if (value instanceof List<?> values) {
                List<String> tags = new ArrayList<>();
                for (Object tag : values) {
                    if (tag instanceof String text && !text.isBlank()) {
                        String trimmed = text.trim();
                        if (!tags.contains(trimmed)) {
                            tags.add(trimmed);
                        }
                        if (tags.size() == 12) {
                            break;
                        }
                    }
                }
                if (!tags.isEmpty()) {
                    cleaned.put(language, List.copyOf(tags));
                }
            }
        }
        if (cleaned.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemma returned no usable tags.");
        }
        return Map.copyOf(cleaned);
    }
}
