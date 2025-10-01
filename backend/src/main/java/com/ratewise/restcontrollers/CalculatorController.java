package com.ratewise.restcontrollers;

import com.ratewise.dto.LandedCostRequest;
import com.ratewise.dto.LandedCostResponse;
import com.ratewise.services.CalculatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/calculate")
public class CalculatorController {

    private final CalculatorService calculatorService;

    public CalculatorController(CalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    @Operation(
        summary = "Compute landed cost",
        description = "Returns customs value, duty, taxes, and total landed cost for the given shipment."
    )
    @ApiResponse(responseCode = "200", description = "OK")
    @PostMapping("/landed-cost")
    public ResponseEntity<LandedCostResponse> calculateLandedCost(@Valid @RequestBody LandedCostRequest req) {
        try {
            Map<String, Object> result = calculatorService.calculateLandedCost(toMap(req));
            return ResponseEntity.ok(toResponse(result)); // success: error=null
        } catch (com.ratewise.exceptions.ApiValidationException e) {
            // If exactly one error, use the specific message; otherwise show a generic headline
            String top = (e.getErrors() != null && e.getErrors().size() == 1)
                    ? e.getErrors().get(0)
                    : "Invalid input.";
            return ResponseEntity.badRequest().body(LandedCostResponse.error(top, e.getErrors()));
        } catch (IllegalArgumentException e) {
            // single input issue
            return ResponseEntity.badRequest().body(LandedCostResponse.error(e.getMessage(), null));
        } catch (IllegalStateException e) {
            // business state issue
            return ResponseEntity.unprocessableEntity().body(LandedCostResponse.error(e.getMessage(), null));
        }
    }

    private Map<String, Object> toMap(LandedCostRequest req) {
        Map<String, Object> requestMap = new LinkedHashMap<>();
        requestMap.put("exporter", req.exporter());
        requestMap.put("importer", req.importer());
        requestMap.put("hsCode", req.hsCode());
        requestMap.put("productDescription", req.productDescription());
        requestMap.put("agreement", req.agreement());
        requestMap.put("goods_value", req.goodsValue());
        requestMap.put("quantity", req.quantity());
        requestMap.put("freight", req.freight());
        requestMap.put("insurance", req.insurance());
        requestMap.put("startDate", req.startDate());
        requestMap.put("endDate", req.endDate());
        return requestMap;
    }

    private LandedCostResponse toResponse(Map<String, Object> result) {
        return new LandedCostResponse(
            (Boolean) result.get("ok"),
            (String) result.get("exporter_input"),
            (String) result.get("importer_input"),
            (String) result.get("exporter_code"),
            (String) result.get("importer_code"),
            (String) result.get("hs_code"),
            (String) result.get("agreement"),
            (String) result.get("customs_basis"),
            result.get("rate_percent") != null ? ((Number) result.get("rate_percent")).doubleValue() : null,
            result.get("customs_value") != null ? ((Number) result.get("customs_value")).doubleValue() : null,
            result.get("duty") != null ? ((Number) result.get("duty")).doubleValue() : null,
            (String) result.get("tax_type"),
            result.get("tax_rate_percent") != null ? ((Number) result.get("tax_rate_percent")).doubleValue() : null,
            result.get("tax") != null ? ((Number) result.get("tax")).doubleValue() : null,
            result.get("quantity") != null ? ((Number) result.get("quantity")).intValue() : null,
            result.get("total_landed_cost") != null ? ((Number) result.get("total_landed_cost")).doubleValue() : null,
            null,   // error (success case)
            null    // errors list (success case)
        );
    }
}
