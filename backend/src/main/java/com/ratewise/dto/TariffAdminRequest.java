package com.ratewise.dto;

import java.math.BigDecimal;

/**
 * DTO for creating/updating a tariff row.
 * Fields use codes (exporter/importer HS/agreement) so service can resolve ids.
 */
public class TariffAdminRequest {
    public String exporterCode;
    public String importerCode;
    public String hsCode;
    public String agreementCode;
    public BigDecimal ratePercent;
    public String validFrom; // ISO yyyy-MM-dd
    public String validTo;   // ISO yyyy-MM-dd or null

    // No-arg constructor for Jackson
    public TariffAdminRequest() {}

    public TariffAdminRequest(String exporterCode, String importerCode, String hsCode, String agreementCode,
                              BigDecimal ratePercent, String validFrom, String validTo) {
        this.exporterCode = exporterCode;
        this.importerCode = importerCode;
        this.hsCode = hsCode;
        this.agreementCode = agreementCode;
        this.ratePercent = ratePercent;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }
}