package com.jaldi.quickcommerce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProductCatalogServiceTest {
    private final ProductCatalogStore store = new InMemoryProductCatalogStore();
    private final ProductCatalogService catalog = new ProductCatalogService(
            store, new ProductSearchService(false, "http://localhost:8108", "", store));

    @Test
    void searchMatchesProductNameAndCategory() {
        List<Product> products = catalog.search("milk", "All items", "popular");
        assertEquals(1, products.size());
        assertEquals("milk", products.getFirst().id());
    }

    @Test
    void hinglishDoodhQueryFindsMilk() {
        List<Product> products = catalog.search("doodh", "All items", "popular");
        assertEquals(1, products.size());
        assertEquals("milk", products.getFirst().id());
    }

    @Test
    void newMilkProductsReceiveKnownSearchAliasesAutomatically() {
        Product product = catalog.addProduct(new ProductDraft(
                "Heritage Daily Health Toned Milk",
                "Heritage",
                "Dairy & eggs",
                "500 ml",
                32,
                36,
                "",
                "🥛",
                "#e7edf4",
                List.of("toned milk"),
                Map.of("english", List.of("toned milk")),
                List.of()));

        assertTrue(product.tags().contains("doodh"));
        assertTrue(product.languageTags().get("hinglish").contains("doodh"));
        assertTrue(product.languageTags().get("hindi").contains("दूध"));
        assertFalse(product.languageTags().containsKey("urdu"));
        assertTrue(catalog.search("doodh", "All items", "popular").stream()
                .anyMatch(result -> result.id().equals(product.id())));
    }

    @Test
    void newWaterProductsReceivePaniSearchAliasesAutomatically() {
        Product product = catalog.addProduct(new ProductDraft(
                "Bisleri Packaged Water Bottle",
                "Bisleri",
                "Beverages",
                "500 ml",
                20,
                20,
                "",
                "💧",
                "#edf2e4",
                List.of("packaged drinking water"),
                Map.of("english", List.of("packaged drinking water")),
                List.of()));

        assertTrue(product.tags().contains("pani"));
        assertTrue(product.tags().contains("पानी"));
        assertTrue(product.languageTags().get("hinglish").contains("paani"));
        assertTrue(catalog.search("pani", "All items", "popular").stream()
                .anyMatch(result -> result.id().equals(product.id())));
    }

    @Test
    void existingWaterProductsReceiveKnownSearchAliases() {
        Product existing = new Product(
                "bisleri", "Bisleri Packaged Water", "Bisleri", "Beverages", "500 ml",
                20, 20, 0, 0, "", "💧", "#edf2e4",
                List.of("bottled water"),
                Map.of("english", List.of("packaged drinking water")),
                List.of());

        Product enriched = ProductCatalogService.addKnownSearchAliases(existing);

        assertTrue(enriched.tags().contains("pani"));
        assertTrue(enriched.languageTags().get("hinglish").contains("paani"));
        assertTrue(enriched.languageTags().get("hindi").contains("पानी"));
        assertTrue(enriched.tags().containsAll(existing.tags()));
    }

    @Test
    void categoryAndPriceSortAreApplied() {
        List<Product> products = catalog.search("", "Dairy & eggs", "price-low");
        assertEquals(6, products.size());
        assertEquals("milk", products.getFirst().id());
        assertFalse(products.isEmpty());
    }

    @Test
    void hinglishMosquitoRepellentQueryFindsProducts() {
        List<Product> products = catalog.search("machhar bhagane ki dawa", "All items", "popular");

        assertEquals(2, products.size());
        assertTrue(products.stream().allMatch(product -> product.brand().equals("Mortein")));
    }

    @Test
    void hindiProductTagsAreSearchable() {
        List<Product> products = catalog.search("मच्छर भगाने की दवा", "All items", "popular");

        assertEquals(2, products.size());
    }

    @Test
    void starterCatalogHasLocalizedStaples() {
        assertEquals("Red Onions", catalog.search("pyaaz", "All items", "popular").getFirst().name());
        assertEquals("Everyday Basmati Rice",
                catalog.search("बासमती चावल", "All items", "popular").getFirst().name());
        assertTrue(catalog.allProducts().size() >= 38);
    }
}
