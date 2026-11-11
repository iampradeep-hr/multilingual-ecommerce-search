package com.mila.quickcommerce;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("docker")
public class KafkaProductSubmissionService implements ProductSubmissionService {
    private final ProductCatalogService catalog;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String ingestTopic;
    private final ProductIndexEventPublisher indexEventPublisher;

    public KafkaProductSubmissionService(
            ProductCatalogService catalog,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            ProductIndexEventPublisher indexEventPublisher,
            @Value("${mila.kafka.topics.ingest}") String ingestTopic) {
        this.catalog = catalog;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.indexEventPublisher = indexEventPublisher;
        this.ingestTopic = ingestTopic;
    }

    @Override
    public Submission submit(ProductDraft draft) {
        Product product = catalog.createProduct(draft);
        try {
            String message = objectMapper.writeValueAsString(product);
            kafkaTemplate.send(ingestTopic, product.id(), message).get(10, TimeUnit.SECONDS);
            return new Submission(product, true);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Product could not be serialized for Kafka.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Product queue request was interrupted.", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Product queue is unavailable.", exception);
        }
    }

    @Override
    public boolean delete(String productId) {
        if (!catalog.productExists(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product was not found.");
        }
        try {
            indexEventPublisher.publishDelete(productId);
            return true;
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Product deletion queue is unavailable.", exception);
        }
    }
}
