package com.mila.quickcommerce;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class AdminProductController {
    private final ProductCatalogService catalog;
    private final GemmaTagService gemmaTagService;
    private final ProductSubmissionService productSubmissionService;

    public AdminProductController(
            ProductCatalogService catalog,
            GemmaTagService gemmaTagService,
            ProductSubmissionService productSubmissionService) {
        this.catalog = catalog;
        this.gemmaTagService = gemmaTagService;
        this.productSubmissionService = productSubmissionService;
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
    public ResponseEntity<Product> addProduct(@Valid @RequestBody ProductDraft draft) {
        if (draft.mrp() < draft.price()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MRP must not be less than the selling price.");
        }
        ProductSubmissionService.Submission submission = productSubmissionService.submit(draft);
        return ResponseEntity.status(submission.acceptedForProcessing() ? HttpStatus.ACCEPTED : HttpStatus.CREATED)
                .body(submission.product());
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String productId) {
        boolean acceptedForProcessing = productSubmissionService.delete(productId);
        return ResponseEntity.status(acceptedForProcessing ? HttpStatus.ACCEPTED : HttpStatus.NO_CONTENT).build();
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
