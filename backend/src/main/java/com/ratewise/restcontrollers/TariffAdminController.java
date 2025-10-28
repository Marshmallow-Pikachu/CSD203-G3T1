package com.ratewise.restcontrollers;

import com.ratewise.services.TariffAdminService;
import com.ratewise.dto.TariffAdminRequest;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.List;

/**
 * Admin controller for tariff CRUD. Mounted under /api/v1/admin/tariffs and protected
 * by role ADMIN via existing WebSecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/admin/tariffs")
public class TariffAdminController {

    private final TariffAdminService adminService;

    public TariffAdminController(TariffAdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody TariffAdminRequest req) {
        Map<String, Object> created = adminService.createTariff(req);
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable int id) {
        Map<String, Object> row = adminService.getById(id);
        return ResponseEntity.ok(row);
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listAll() {
        return ResponseEntity.ok(adminService.listAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable int id, @RequestBody TariffAdminRequest req) {
        Map<String, Object> updated = adminService.updateTariff(id, req);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        adminService.deleteTariff(id);
        return ResponseEntity.noContent().build();
    }
}