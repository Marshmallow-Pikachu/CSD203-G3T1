package com.ratewise;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1")
public class RestAPIController {
    private final JdbcTemplate jdbcTemplate;

    public RestAPIController (JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

   
    @GetMapping("/realquery")
    public Map<String, Object> realSearch(
        @RequestParam String hscode, 
        @RequestParam String exp_country,
        @RequestParam String imp_country,
        @RequestParam double cost,
        @RequestParam int quantity,
        @RequestParam double freight,
        @RequestParam double insurance
        /*,@RequestParam boolean FTA*/) {

        
        // check if dest_country uses CIF or FOB
        // eg singapore uses CIF. US uses FOB
        // !might need this------------------------------------------
        // boolean CIF; 

                String sql = """
            SELECT 
                tr.rate_percent,
                ag.agreement_name,
                hc.hs_code,
                hc.description AS hs_description,
                exp.country_name AS exporter,
                imp.country_name AS importer
            FROM tariff_rates tr
            JOIN hs_codes hc ON tr.hs_code_id = hc.id
            JOIN countries exp ON tr.exporter_id = exp.id
            JOIN countries imp ON tr.importer_id = imp.id
            JOIN agreements ag ON tr.agreement_id = ag.id
            WHERE hc.hs_code = ?
            AND exp.country_name = ?
            AND imp.country_name = ?
            AND (tr.valid_to IS NULL OR tr.valid_to >= CURRENT_DATE)
            ORDER BY tr.rate_percent ASC
            LIMIT 1;
        """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql,
            hscode, exp_country, imp_country);

        if (results.isEmpty()) {
            throw new RuntimeException("No tariff rate found for given parameters.");
        }

        Map<String, Object> tariffData = results.get(0);
        double ratePercent = ((Number) tariffData.get("rate_percent")).doubleValue();
        
        double tariffDuty = cost * quantity * (ratePercent / 100);
        double totalCharges = cost * quantity + tariffDuty + freight + insurance;

        Map<String, Object> response = new TreeMap<>();
        response.put("exporter", exp_country);
        response.put("importer", imp_country);
        response.put("hs_code", hscode);
        response.put("tariff_rate_percent", ratePercent);
        response.put("tariff_duty", tariffDuty);
        response.put("freight", freight);
        response.put("insurance", insurance);
        response.put("total_charges", totalCharges);

        return response;
    }
    
    @GetMapping("query")
    public Map<String, Object> dummySearch() {
        String hscode = "040221"; //milk
        String exp_country = "Singapore";
        String imp_country = "United States";
        double cost = 100.01;
        int quantity = 2;
        double freight = 30.33;
        double insurance = 5.00;
        boolean FTA = false;
        

        // To Do
        // 1. Currently does not take into account whether there is a free trade agreement
        // 2. Does not account for CIF FOB
        String sql = """
            SELECT 
                tr.rate_percent,
                ag.agreement_name,
                hc.hs_code,
                hc.description AS hs_description,
                exp.country_name AS exporter,
                imp.country_name AS importer
            FROM tariff_rates tr
            JOIN hs_codes hc ON tr.hs_code_id = hc.id
            JOIN countries exp ON tr.exporter_id = exp.id
            JOIN countries imp ON tr.importer_id = imp.id
            JOIN agreements ag ON tr.agreement_id = ag.id
            WHERE hc.hs_code = ?
            AND exp.country_name = ?
            AND imp.country_name = ?
            AND (tr.valid_to IS NULL OR tr.valid_to >= CURRENT_DATE)
            ORDER BY tr.rate_percent ASC
            LIMIT 1;
        """;

        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql,
            hscode, exp_country, imp_country);

        if (results.isEmpty()) {
            throw new RuntimeException("No tariff rate found for given parameters.");
        }

        Map<String, Object> tariffData = results.get(0);
        double ratePercent = ((Number) tariffData.get("rate_percent")).doubleValue();
        
        double tariffDuty = cost * quantity * (ratePercent / 100);
        double totalCharges = cost * quantity + tariffDuty + freight + insurance;

        Map<String, Object> response = new TreeMap<>();
        response.put("exporter", exp_country);
        response.put("importer", imp_country);
        response.put("hs_code", hscode);
        response.put("tariff_rate_percent", ratePercent);
        response.put("tariff_duty", tariffDuty);
        response.put("freight", freight);
        response.put("insurance", insurance);
        response.put("total_charges", totalCharges);

        return response;
    }


    // MUST NOT BE ENALBED ON PRODUCTION

    // @GetMapping("allData")
    // public String getMethodName(@RequestParam String param) {
    //     return new String();
    // }
    
    // public List<List<Map<String, Object>>> getAllDataFromDatabase() {
    //             List<List<Map<String,Object>>> rows = new ArrayList<List<Map<String,Object>>>();
    //     try {
            
    //         DatabaseMetaData metaData = jdbcTemplate.getDataSource().getConnection().getMetaData();
    //         ResultSet tables = metaData.getTables(null, null, "%", new String[] {"TABLE"});
    //         while(tables.next()) {
    //             String tableName = tables.getString("TABLE_NAME");
    //             if (tableName.equals("users")) continue;
    //             System.out.println(tableName);
    //             rows.add(jdbcTemplate.queryForList("SELECT * FROM " + tableName));
    //             // store or process rows grouped by tableName
    //         }
    //     } catch (Exception e) {
    //         System.out.println("yeet");
    //     }
    //     return rows;
    // }
}
