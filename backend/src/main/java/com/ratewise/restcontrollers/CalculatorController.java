package com.ratewise.restcontrollers;

import com.ratewise.services.CalculatorService;
import com.ratewise.dto.CalculatorRequest;
import java.util.Map;
import java.util.HashMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CalculatorController
 *
 * REST API for the calculator feature.
 * Endpoint: POST /api/v1/calculator/landed-cost
 *
 * Behavior:
 * - Accepts CalculatorRequest DTO.
 * - If request.effectiveDate is provided it is forwarded as "effectiveDate" and represents the single
 *   exact date used to resolve tariff applicability. Any start/end ranges are ignored.
 * - For backward compatibility this controller also forwards "endDate" with the same value;
 *   remove the "endDate" forwarding once the service layer is updated to use "effectiveDate".
 */
@RestController
@RequestMapping("/api/v1/calculator")
public class CalculatorController {

    private final CalculatorService calculatorService;

    public CalculatorController(CalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    /**
     * POST /api/v1/calculator/landed-cost
     *
     * Request body example:
     * {
     *   "exporter": "Singapore",
     *   "importer": "United States",
     *   "hsCode": "010121",
     *   "agreement": "MFN",
     *   "goods_value": 1000,
     *   "quantity": 2,
     *   "freight": 50,
     *   "insurance": 100,
     *   "effectiveDate": "2025-10-28" // optional, single exact lookup date
     * }
     *
     * Returns landed cost breakdown:
     * - customs basis (CIF/FOB)
     * - duty rate & duty amount (tariff rate chosen for exact effectiveDate; lowest if multiple)
     * - tax rate & tax amount
     * - total landed cost
     */
    @PostMapping("/landed-cost")
    public Map<String, Object> calculateLandedCost(@RequestBody CalculatorRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("exporter", request.exporter());
        payload.put("importer", request.importer());
        payload.put("hsCode", request.hsCode());
        payload.put("productDescription", request.productDescription());
        payload.put("agreement", request.agreement());
        if (request.goodsValue() != null) payload.put("goods_value", request.goodsValue());
        if (request.quantity() != null) payload.put("quantity", request.quantity());
        if (request.freight() != null) payload.put("freight", request.freight());
        if (request.insurance() != null) payload.put("insurance", request.insurance());
        // forward single exact lookup date (if provided). Service must interpret it as exact date.
        if (request.effectiveDate() != null && !request.effectiveDate().isBlank()) {
            payload.put("effectiveDate", request.effectiveDate());
        }

        return calculatorService.calculateLandedCost(payload);
    }
}