package com.mila.quickcommerce;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!docker")
public class DirectProductSubmissionService implements ProductSubmissionService {
    private final ProductCatalogService catalog;

    public DirectProductSubmissionService(ProductCatalogService catalog) {
        this.catalog = catalog;
    }

    @Override
    public Submission submit(ProductDraft draft) {
        return new Submission(catalog.addProduct(draft), false);
    }

    @Override
    public boolean delete(String productId) {
        catalog.deleteProduct(productId);
        return false;
    }
}
