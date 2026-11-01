package com.jaldi.quickcommerce;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoProductRepository extends MongoRepository<Product, String> {
}
