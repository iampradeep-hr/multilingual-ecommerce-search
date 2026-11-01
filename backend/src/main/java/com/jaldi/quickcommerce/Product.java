package com.jaldi.quickcommerce;

import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("products")
public record Product(
        @Id String id,
        String name,
        String brand,
        String category,
        String unit,
        int price,
        int mrp,
        double rating,
        int reviews,
        String badge,
        String emoji,
        String tint,
        List<String> tags,
        Map<String, List<String>> languageTags,
        List<String> stockCities) {
    public Product(
            String id,
            String name,
            String brand,
            String category,
            String unit,
            int price,
            int mrp,
            double rating,
            int reviews,
            String badge,
            String emoji,
            String tint) {
        this(id, name, brand, category, unit, price, mrp, rating, reviews, badge, emoji, tint,
                List.of(), Map.of(), List.of());
    }

    public Product(
            String id,
            String name,
            String brand,
            String category,
            String unit,
            int price,
            int mrp,
            double rating,
            int reviews,
            String badge,
            String emoji,
            String tint,
            List<String> tags) {
        this(id, name, brand, category, unit, price, mrp, rating, reviews, badge, emoji, tint,
                tags, Map.of(), List.of());
    }

    public Product {
        tags = List.copyOf(tags);
        languageTags = languageTags.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
        stockCities = List.copyOf(stockCities);
    }
}
