package com.jaldi.quickcommerce;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Profile("docker")
public class ProductIndexWorker {
    private final ObjectMapper objectMapper;
    private final ProductSearchService productSearch;
    private final ProductCatalogService catalog;

    public ProductIndexWorker(
            ObjectMapper objectMapper,
            ProductSearchService productSearch,
            ProductCatalogService catalog) {
        this.objectMapper = objectMapper;
        this.productSearch = productSearch;
        this.catalog = catalog;
    }

    @KafkaListener(topics = "${jaldi.kafka.topics.index}", groupId = "jaldi-product-index")
    public void index(String message) {
        if (message.startsWith("DELETE:")) {
            String productId = message.substring("DELETE:".length());
            if (catalog.productExists(productId)) {
                catalog.deleteProduct(productId);
            }
            return;
        }
        try {
            productSearch.index(objectMapper.readValue(message, Product.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid product index event.", exception);
        }
    }
}
