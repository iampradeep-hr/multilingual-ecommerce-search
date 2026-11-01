package com.jaldi.quickcommerce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProductCatalogServiceTest {
    private final ProductCatalogService catalog = new ProductCatalogService(new InMemoryProductCatalogStore());

    @Test
    void searchMatchesProductNameAndCategory() {
        List<Product> products = catalog.search("milk", "All items", "popular");
        assertEquals(1, products.size());
        assertEquals("milk", products.getFirst().id());
    }

    @Test
    void categoryAndPriceSortAreApplied() {
        List<Product> products = catalog.search("", "Dairy & eggs", "price-low");
        assertEquals(3, products.size());
        assertEquals("milk", products.getFirst().id());
        assertFalse(products.isEmpty());
    }
}
