package com.jaldi.quickcommerce;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Profile("docker")
public class ProductIngestWorker {
    private final ObjectMapper objectMapper;
    private final ProductCatalogService catalog;
    private final ProductIndexEventPublisher indexEventPublisher;

    public ProductIngestWorker(
            ObjectMapper objectMapper,
            ProductCatalogService catalog,
            ProductIndexEventPublisher indexEventPublisher) {
        this.objectMapper = objectMapper;
        this.catalog = catalog;
        this.indexEventPublisher = indexEventPublisher;
    }

    @KafkaListener(topics = "${jaldi.kafka.topics.ingest}", groupId = "jaldi-product-ingest")
    public void ingest(String message) {
        Product product = deserialize(message);
        Product savedProduct = catalog.saveProduct(product);
        indexEventPublisher.publish(savedProduct);
    }

    private Product deserialize(String message) {
        try {
            return objectMapper.readValue(message, Product.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid product ingest event.", exception);
        }
    }
}
