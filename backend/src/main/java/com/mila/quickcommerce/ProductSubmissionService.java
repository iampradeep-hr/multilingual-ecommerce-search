package com.mila.quickcommerce;

public interface ProductSubmissionService {
    Submission submit(ProductDraft draft);

    boolean delete(String productId);

    record Submission(Product product, boolean acceptedForProcessing) {}
}
