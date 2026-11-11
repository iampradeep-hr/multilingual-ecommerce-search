package com.mila.quickcommerce;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class StorefrontController {
    private static final Pattern PINCODE_PATTERN = Pattern.compile("\\d{6}");
    private static final Map<String, String> CITIES_BY_PINCODE = Map.of(
            "400001", "Mumbai",
            "560001", "Bengaluru",
            "110001", "New Delhi",
            "600001", "Chennai",
            "700001", "Kolkata");

    private final ProductCatalogService catalog;

    public StorefrontController(ProductCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/categories")
    public List<Category> categories() {
        return catalog.categories();
    }

    @GetMapping("/search")
    public List<Product> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "All items") String category,
            @RequestParam(defaultValue = "popular") String sort) {
        return catalog.search(q, category, sort);
    }

    @GetMapping("/locations/resolve")
    public StoreLocation resolveLocation(@RequestParam String pincode) {
        if (!PINCODE_PATTERN.matcher(pincode).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid 6-digit PIN code.");
        }

        String city = CITIES_BY_PINCODE.getOrDefault(pincode, "Your neighbourhood");
        return new StoreLocation(pincode, city, city + " Central Store", "10–20 min");
    }
}
