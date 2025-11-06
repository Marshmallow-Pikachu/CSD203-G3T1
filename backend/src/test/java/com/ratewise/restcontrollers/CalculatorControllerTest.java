package com.ratewise.restcontrollers;

import com.ratewise.services.CalculatorService;
import com.ratewise.dto.CalculatorRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CalculatorControllerTest {
    @Mock
    private CalculatorService calculatorService;
    
    @InjectMocks
    private CalculatorController calculatorController;
    
    @Test
    void calculateLandedCost_shouldOmitNullFieldsIfNull() {
        // Original Method takes in a calculator request
        CalculatorRequest request = new CalculatorRequest(
            "Singapore",
            "United States",
            "010121",
            "MFN",
            null, 
            null,  
            null,  
            null,  
            null   
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
}
