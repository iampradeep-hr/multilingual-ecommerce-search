package com.mila.quickcommerce;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record ProductDraft(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 60) String brand,
        @NotBlank String category,
        @NotBlank @Size(max = 40) String unit,
        @Min(1) int price,
        @Min(1) int mrp,
        @Size(max = 24) String badge,
        @NotBlank @Size(max = 8) String emoji,
        @NotBlank String tint,
        @NotNull List<@NotBlank @Size(max = 64) String> tags,
        @NotNull Map<@Size(max = 24) String, List<@NotBlank @Size(max = 64) String>> languageTags,
        @NotNull List<@NotBlank @Size(max = 40) String> stockCities) {
}
