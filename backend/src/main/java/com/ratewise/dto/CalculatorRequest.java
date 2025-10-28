package com.ratewise.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request DTO for landed cost calculation.
 * - effectiveDate: optional ISO date string (yyyy-MM-dd or full ISO). When provided the calculator
 *   evaluates tariffs that are applicable on that exact date. If multiple tariffs apply,
 *   the service should pick the lowest tariff rate.
 *
 * Field mapping:
 * - frontend sends "goods_value" -> goodsValue
 * - frontend may send "effectiveDate"
 */
public record CalculatorRequest(
    String exporter,
    String importer,
    String hsCode,
    String agreement,
    @JsonProperty("goods_value") Double goodsValue,
    Integer quantity,
    Double freight,
    Double insurance,
    @JsonProperty("effectiveDate") String effectiveDate
) {}