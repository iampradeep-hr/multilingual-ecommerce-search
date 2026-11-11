package com.mila.quickcommerce;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.context.event.EventListener;

@Service
public class ProductSearchService {
    private static final String COLLECTION = "products";
    private static final Pattern NON_SEARCH_CHARACTERS = Pattern.compile("[^\\p{L}\\p{M}\\p{N}]+");
    private static final Set<String> QUERY_STOP_WORDS = Set.of(
            "a", "an", "the", "is", "are", "hai", "he", "ka", "ki", "ke", "raha", "rahe", "rahi", "baba");

    private final boolean enabled;
    private final ProductCatalogStore store;
    private final RestClient restClient;

    public ProductSearchService(
            @Value("${mila.typesense.enabled:false}") boolean enabled,
            @Value("${mila.typesense.url:http://localhost:8108}") String serviceUrl,
            @Value("${mila.typesense.api-key:}") String apiKey,
            ProductCatalogStore store) {
        this.enabled = enabled;
        this.store = store;
        RestClient.Builder builder = RestClient.builder()
                .requestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory(
                        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()));
        if (enabled) {
            if (serviceUrl.isBlank() || apiKey.isBlank()) {
                throw new IllegalStateException("Typesense is enabled but its URL or API key is missing.");
            }
            builder.baseUrl(serviceUrl.replaceAll("/+$", ""))
                    .defaultHeader("X-TYPESENSE-API-KEY", apiKey);
        }
        this.restClient = builder.build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void synchronizeCatalog() {
        if (!enabled) {
            return;
        }
        ensureCollection();
        store.allProducts().forEach(this::index);
    }

    public List<String> searchIds(String query, List<Product> products) {
        if (query == null || query.isBlank()) {
            return products.stream().map(Product::id).toList();
        }
        if (!enabled) {
            return products.stream()
                    .filter(product -> matchesLocally(product, query))
                    .map(Product::id)
                    .toList();
        }

        try {
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/collections/{collection}/documents/search")
                            .queryParam("q", query)
                            .queryParam("query_by", "search_text")
                            .queryParam("drop_tokens_threshold", 1)
                            .queryParam("typo_tokens_threshold", 1)
                            .build(COLLECTION))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (response == null || !(response.get("hits") instanceof List<?> hits)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Typesense returned an invalid search response.");
            }
            List<String> ids = new ArrayList<>();
            for (Object hit : hits) {
                if (hit instanceof Map<?, ?> hitData && hitData.get("document") instanceof Map<?, ?> document
                        && document.get("id") instanceof String id) {
                    ids.add(id);
                }
            }
            return ids;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Typesense search request failed.", exception);
        }
    }

    public void index(Product product) {
        if (!enabled) {
            return;
        }
        try {
            restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/collections/{collection}/documents")
                            .queryParam("action", "upsert")
                            .build(COLLECTION))
                    .body(Map.of(
                            "id", product.id(),
                            "search_text", searchableText(product),
                            "reviews", product.reviews()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Typesense product indexing failed.", exception);
        }
    }

    public void delete(String productId) {
        if (!enabled) {
            return;
        }
        try {
            restClient.delete()
                    .uri("/collections/{collection}/documents/{id}", COLLECTION, productId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() != HttpStatus.NOT_FOUND) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Typesense product deletion failed.", exception);
            }
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Typesense product deletion failed.", exception);
        }
    }

    private void ensureCollection() {
        try {
            restClient.post()
                    .uri("/collections")
                    .body(Map.of(
                            "name", COLLECTION,
                            "fields", List.of(
                                    Map.of("name", "search_text", "type", "string"),
                                    Map.of("name", "reviews", "type", "int32", "sort", true))))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() != HttpStatus.CONFLICT) {
                throw new IllegalStateException("Typesense product collection could not be created.", exception);
            }
        } catch (RestClientException exception) {
            throw new IllegalStateException("Typesense product collection could not be created.", exception);
        }
    }

    private boolean matchesLocally(Product product, String query) {
        String text = searchableText(product).toLowerCase(Locale.ROOT);
        String normalizedQuery = normalize(query);
        if (text.contains(normalizedQuery)) {
            return true;
        }
        return tokens(query).stream().filter(token -> !QUERY_STOP_WORDS.contains(token))
                .anyMatch(token -> text.contains(token));
    }

    private String searchableText(Product product) {
        List<String> fields = new ArrayList<>(List.of(product.name(), product.brand(), product.category()));
        fields.addAll(product.tags());
        product.languageTags().values().forEach(fields::addAll);
        return String.join(" ", fields);
    }

    private List<String> tokens(String text) {
        return List.of(NON_SEARCH_CHARACTERS.split(text.toLowerCase(Locale.ROOT))).stream()
                .filter(token -> !token.isBlank())
                .toList();
    }

    private String normalize(String text) {
        return String.join(" ", tokens(text));
    }
}
