package com.jaldi.quickcommerce;

import java.util.List;

public interface ProductCatalogStore {
    List<Product> allProducts();

    Product save(Product product);
}
