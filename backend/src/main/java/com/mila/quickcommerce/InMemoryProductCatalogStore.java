package com.mila.quickcommerce;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!docker")
public class InMemoryProductCatalogStore implements ProductCatalogStore {
    private final Map<String, Product> products = new ConcurrentHashMap<>();

    public InMemoryProductCatalogStore() {
        ProductCatalogService.seedProducts().forEach(product -> products.put(product.id(), product));
    }

    @Override
    public List<Product> allProducts() {
        return List.copyOf(products.values());
    }

    @Override
    public Product save(Product product) {
        products.put(product.id(), product);
        return product;
    }

    @Override
    public boolean existsById(String id) {
        return products.containsKey(id);
    }

    @Override
    public void deleteById(String id) {
        products.remove(id);
    }
}
