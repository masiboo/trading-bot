package com.trading.controller;

import com.trading.service.RiskManagementService;
import com.trading.service.OrderExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for monitoring trading bot status and metrics.
 */
@RestController
@RequestMapping("/trading")
@RequiredArgsConstructor
@Slf4j
public class TradingController {
    
    private final RiskManagementService riskManagementService;
    private final OrderExecutionService orderExecutionService;
    
    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis() + "");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get current trading metrics.
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("dailyLoss", riskManagementService.getCurrentDailyLoss());
        metrics.put("openPositions", riskManagementService.getOpenPositionCount());
        metrics.put("executedOrders", orderExecutionService.getExecutedOrderCount());
        metrics.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Get trading status.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", true);
        status.put("dailyLoss", riskManagementService.getCurrentDailyLoss());
        status.put("openPositions", riskManagementService.getOpenPositionCount());
        status.put("executedOrders", orderExecutionService.getExecutedOrderCount());
        status.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(status);
    }
    
}
