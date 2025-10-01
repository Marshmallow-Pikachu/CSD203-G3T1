package com.ratewise.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TariffListItem")
public record TariffListItem(
    @Schema(example = "010121") String hs_code,
    @Schema(example = "Pure-bred breeding horses") String hs_description,

    @Schema(example = "SG") String exporter_code,
    @Schema(example = "Singapore") String exporter_name,

    @Schema(example = "JP") String importer_code,
    @Schema(example = "Japan") String importer_name,

    @Schema(example = "CPTPP") String agreement_code,
    @Schema(example = "Comprehensive and Progressive Agreement for Trans-Pacific Partnership") String agreement_name,

    @Schema(example = "4.5") Double rate_percent,
    @Schema(example = "CIF") String customs_basis
) {}
