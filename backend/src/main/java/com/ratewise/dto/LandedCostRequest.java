package com.ratewise.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(name = "LandedCostRequest")
public record LandedCostRequest(
    @NotBlank @Schema(example = "Singapore") String exporter,
    @NotBlank @Schema(example = "United States") String importer,
    @Schema(example = "010121", description = "HS code (6-digit) or null if using productDescription") String hsCode,
    @Schema(example = "Coffee, not roasted", description = "Optional if hsCode is provided") String productDescription,
    @NotBlank @Schema(example = "MFN") String agreement,

    @JsonProperty("goods_value")
    @Positive @Schema(example = "1000") Double goodsValue,

    @Positive @Schema(example = "2") Integer quantity,

    @PositiveOrZero @Schema(example = "50") Double freight,

    @PositiveOrZero @Schema(example = "100") Double insurance,

    @Schema(example = "01/09/2025", description = "Accepted formats: dd/MM/yyyy or yyyy-MM-dd") String startDate,
    @Schema(example = "30/09/2025", description = "Accepted formats: dd/MM/yyyy or yyyy-MM-dd") String endDate
) {}
