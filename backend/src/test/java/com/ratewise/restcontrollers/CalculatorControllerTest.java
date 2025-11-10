package com.ratewise.restcontrollers;

import com.ratewise.services.CalculatorService;
import com.ratewise.dto.CalculatorRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.TestExecutionListeners;

import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CalculatorControllerTest {
    @Mock
    private CalculatorService calculatorService;
    
    @InjectMocks
    private CalculatorController calculatorController;
    

    // ensure that the function calculate landed cost actually calls calculatorService.calculateLandedCost
    @Test
    void calculateLandedCost_ShouldCallService() {
        // Original Method takes in a calculator request
        CalculatorRequest request = new CalculatorRequest(
            "Singapore",
            "United States",
            "010121",
            null,
            "MFN",
            1000.0,
            2,
            50.0,
            100.0,
            "2025-10-28"
        );

        Map<String, Object> expectedResult = new HashMap<>();
        expectedResult.put("dutyRate", 5.0);
        expectedResult.put("dutyAmount", 50.0);
        expectedResult.put("totalLandedCost", 1200.0);

        when(calculatorService.calculateLandedCost(any())).thenReturn(expectedResult);

        Map<String, Object> testResult = calculatorController.calculateLandedCost(request);

        assertEquals(expectedResult, testResult);

        verify(calculatorService, times(1)).calculateLandedCost(any());
    }


    // Test case with all optional fields null
    @Test
    void calculateLandedCost_ShouldOmitFieldsIfNull() {
        CalculatorRequest request = new CalculatorRequest(
            "Singapore",
            "United States",
            "010121",
            null,
            "MFN",
            null,  
            null,  
            null,  
            null,  
            null  
        );

        Map<String, Object> mockResult = new HashMap<>();
        when(calculatorService.calculateLandedCost(any())).thenReturn(mockResult);

        calculatorController.calculateLandedCost(request);

        verify(calculatorService).calculateLandedCost(argThat(payload ->
            !payload.containsKey("goodsValue") && 
            !payload.containsKey("quantity") &&
            !payload.containsKey("freight") &&
            !payload.containsKey("insurance") &&
            !payload.containsKey("effectiveDate")
        ));
    }

    // Ensure date is properly added and queried
    @Test
    void calculateLandedCost_shouldReflectEffectiveDate_WhenProvided() {

        
        // test date is "2025-10-28"
        CalculatorRequest request = new CalculatorRequest(
            "Singapore",
            "United States",
            "010121",
            null,
            "MFN",
            1000.0,
            2,
            50.0,
            100.0,
            "2025-10-28"
        );

        Map<String, Object> mockResult = new HashMap<>();
        when(calculatorService.calculateLandedCost(any())).thenReturn(mockResult);

        // test the actual controller logic but drop the output
        calculatorController.calculateLandedCost(request);

        verify(calculatorService).calculateLandedCost(argThat(payload -> 
            payload.containsKey("effectiveDate") &&
            payload.get("effectiveDate").equals("2025-10-28")
        ));

    }

}
