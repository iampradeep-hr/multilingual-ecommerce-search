package com.jaldi.quickcommerce;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class AdminProductController {
    private final ProductCatalogService catalog;
    private final GemmaTagService gemmaTagService;

    public AdminProductController(ProductCatalogService catalog, GemmaTagService gemmaTagService) {
        this.catalog = catalog;
        this.gemmaTagService = gemmaTagService;
    }

    @GetMapping("/session")
    public Map<String, String> session(Principal principal) {
        return Map.of("username", principal.getName());
    }

    @GetMapping("/products")
    public List<Product> products() {
        return catalog.allProducts();
    }

    @PostMapping("/products")
    public Product addProduct(@Valid @RequestBody ProductDraft draft) {
        if (draft.mrp() < draft.price()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MRP must not be less than the selling price.");
        }
        return catalog.addProduct(draft);
    }

    @PostMapping("/products/preview-tags")
    public Map<String, List<String>> previewTags(@RequestBody Map<String, String> input) {
        return catalog.previewTags(input.getOrDefault("name", ""), input.getOrDefault("brand", ""),
                input.getOrDefault("category", ""));
    }

    @PostMapping("/products/generate-tags")
    public Map<String, List<String>> generateTags(@Valid @RequestBody TagGenerationRequest input) {
        return gemmaTagService.generateTags(input);
    }
}
