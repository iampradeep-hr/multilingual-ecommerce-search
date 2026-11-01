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
        if (repository.count() == 0) {
            repository.saveAll(ProductCatalogService.seedProducts());
        }
    }

    @Override
    public List<Product> allProducts() {
        return repository.findAll();
    }

    @Override
    public Product save(Product product) {
        return repository.save(product);
    }
}
