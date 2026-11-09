package com.jaldi.quickcommerce;

import java.util.List;

public interface ProductCatalogStore {
    List<Product> allProducts();

    Product save(Product product);

    boolean existsById(String id);

    void deleteById(String id);
}
