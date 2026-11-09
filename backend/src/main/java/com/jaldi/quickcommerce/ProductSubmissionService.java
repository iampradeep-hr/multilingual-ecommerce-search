package com.jaldi.quickcommerce;

public interface ProductSubmissionService {
    Submission submit(ProductDraft draft);

    boolean delete(String productId);

    record Submission(Product product, boolean acceptedForProcessing) {}
}
