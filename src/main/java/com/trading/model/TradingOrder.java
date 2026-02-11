package com.trading.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents a trading order placed on an exchange.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradingOrder {
    
    public enum Status {
        PENDING, EXECUTED, FAILED, CANCELLED
    }
    
    private String orderId;
    private String symbol;
    private TradingDecision.Action action;
    private double amount;
    private double executionPrice;
    private Status status;
    private Instant createdAt;
    private Instant executedAt;
    private String errorMessage;
    
}
