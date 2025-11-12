package com.ratewise.restcontrollers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CountryControllerIT {
    
    @Autowired
    private CountryController controller;  // ← Just autowire the controller directly

    @Test
    void testListCountriesReturnsData() {
        List<Map<String, Object>> countries = controller.listCountries();
        
        assertNotNull(countries);
        assertTrue(countries.size() > 0);
    }
    
    @Test
    void testGetCountryReturnsData() {
        Map<String, Object> country = controller.getCountry("SG");
        
        assertNotNull(country);
        assertEquals("SG", country.get("country_code"));
    }
}
