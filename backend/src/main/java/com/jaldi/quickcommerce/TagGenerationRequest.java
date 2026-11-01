package com.jaldi.quickcommerce;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagGenerationRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 60) String brand,
        @Size(max = 60) String category) {
}
