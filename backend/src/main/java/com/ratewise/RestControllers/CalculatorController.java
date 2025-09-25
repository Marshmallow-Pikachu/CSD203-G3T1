
package com.ratewise.restcontrollers;
import com.ratewise.services.CalculatorService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * CalculationController
 *
 * REST API for the calculator feature.
 * Endpoint: POST /api/calculate/landed-cost
 */

@RestController
@RequestMapping("/api/calculate")
public class CalculatorController {

    private final CalculatorService calculatorService;

    public CalculatorController(CalculatorService calculatorService) {
        this.calculatorService = calculatorService;
    }

    /**
    * POST /api/calculate/landed-cost
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
    *   "insurance": 100
    * }
    *
    * Returns landed cost breakdown:
    * - customs basis (CIF/FOB)
    * - duty rate & duty amount
    * - tax rate & tax amount
    * - total landed cost
    */
    @PostMapping("/landed-cost")
    public Map<String, Object> calculateLandedCost(@RequestBody Map<String, Object> request) {
        return calculatorService.calculateLandedCost(request);
    }
}
