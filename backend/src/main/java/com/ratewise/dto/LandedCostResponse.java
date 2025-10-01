package com.ratewise.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LandedCostResponse")
public record LandedCostResponse(
    @Schema(example = "true") Boolean ok,
    @Schema(example = "Singapore") String exporter_input,
    @Schema(example = "United States") String importer_input,
    @Schema(example = "SG") String exporter_code,
    @Schema(example = "US") String importer_code,
    @Schema(example = "010121") String hs_code,
    @Schema(example = "MFN") String agreement,
    @Schema(example = "CIF") String customs_basis,
    @Schema(example = "5.0") Double rate_percent,
    @Schema(example = "1150.0") Double customs_value,
    @Schema(example = "57.5") Double duty,
    @Schema(example = "VAT") String tax_type,
    @Schema(example = "8.0") Double tax_rate_percent,
    @Schema(example = "96.6") Double tax,
    @Schema(example = "2") Integer quantity,
    @Schema(example = "1304.1") Double total_landed_cost
) {}
