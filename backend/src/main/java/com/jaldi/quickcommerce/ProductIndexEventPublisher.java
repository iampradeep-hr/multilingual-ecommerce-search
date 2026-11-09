package com.jaldi.quickcommerce;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("docker")
public class ProductIndexEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String indexTopic;

    public ProductIndexEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${jaldi.kafka.topics.index}") String indexTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.indexTopic = indexTopic;
    }

    public void publish(Product product) {
        try {
            String message = objectMapper.writeValueAsString(product);
            kafkaTemplate.send(indexTopic, product.id(), message).get(10, TimeUnit.SECONDS);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Product could not be serialized for indexing.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Product indexing event was interrupted.", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Product indexing queue is unavailable.", exception);
        }
    }

    public void publishDelete(String productId) {
        try {
            kafkaTemplate.send(indexTopic, productId, "DELETE:" + productId).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Product deletion event was interrupted.", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Product indexing queue is unavailable.", exception);
        }
    }
}
