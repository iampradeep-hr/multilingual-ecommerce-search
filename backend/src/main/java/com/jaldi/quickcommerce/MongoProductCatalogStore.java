package com.jaldi.quickcommerce;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("docker")
public class MongoProductCatalogStore implements ProductCatalogStore, ApplicationRunner {
    private final MongoProductRepository repository;

    public MongoProductCatalogStore(MongoProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Product> starterProducts = ProductCatalogService.seedProducts();
        starterProducts.stream()
                .filter(product -> !repository.existsById(product.id()))
                .forEach(repository::save);

        Product seededMilk = starterProducts.stream()
                .filter(product -> product.id().equals("milk"))
                .findFirst()
                .orElseThrow();
        repository.findById(seededMilk.id())
                .filter(product -> product.tags().isEmpty() && product.languageTags().isEmpty())
                .ifPresent(ignored -> repository.save(seededMilk));

        repository.findAll().forEach(product -> {
            Product enriched = ProductCatalogService.addKnownSearchAliases(product);
            if (!enriched.equals(product)) {
                repository.save(enriched);
            }
        });
    }

    @Override
    public List<Product> allProducts() {
        return repository.findAll();
    }

    @Override
    public Product save(Product product) {
        return repository.save(product);
    }

    @Override
    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
