package com.trading.service;

import com.trading.config.TradingProperties;
import com.trading.model.TradingDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RiskManagementService.
 */
@ExtendWith(MockitoExtension.class)
class RiskManagementServiceTest {
    
    @Mock
    private TradingProperties tradingProperties;
    
    private RiskManagementService riskManagementService;
    
    @BeforeEach
    void setUp() {
        // Initialize with default properties
        tradingProperties = new TradingProperties();
        tradingProperties.getRisk().setDailyLossLimit(500.0);
        tradingProperties.getRisk().setMaxPositionSize(0.05);
        tradingProperties.getRisk().setMaxOpenPositions(5);
        
        riskManagementService = new RiskManagementService(tradingProperties);
    }
    
    @Test
    void testCanExecuteTrade_WithinLimits() {
        TradingDecision decision = TradingDecision.builder()
                .symbol("BTC_USDT")
                .action(TradingDecision.Action.BUY)
                .amount(100.0)
                .confidence(0.8)
                .reason("Test")
                .timestamp(Instant.now())
                .build();
        
        double portfolioValue = 10000.0;
        
        boolean canExecute = riskManagementService.canExecuteTrade(decision, portfolioValue);
        assertTrue(canExecute, "Trade should be allowed within risk limits");
    }
    
    @Test
    void testCanExecuteTrade_HoldAction() {
        TradingDecision decision = TradingDecision.builder()
                .symbol("BTC_USDT")
                .action(TradingDecision.Action.HOLD)
                .amount(0.0)
                .confidence(0.5)
                .reason("Test")
                .timestamp(Instant.now())
                .build();
        
        double portfolioValue = 10000.0;
        
        boolean canExecute = riskManagementService.canExecuteTrade(decision, portfolioValue);
        assertTrue(canExecute, "HOLD action should always be allowed");
    }
    
    @Test
    void testRecordTradeResult() {
        TradingDecision decision = TradingDecision.builder()
                .symbol("BTC_USDT")
                .action(TradingDecision.Action.BUY)
                .amount(100.0)
                .confidence(0.8)
                .reason("Test")
                .timestamp(Instant.now())
                .build();
        
        double profitLoss = 50.0;
        
        riskManagementService.recordTradeResult(decision, profitLoss);
        
        // Daily loss should be negative of profit
        assertEquals(-50.0, riskManagementService.getCurrentDailyLoss(), 0.01);
    }
    
    @Test
    void testGetCurrentDailyLoss() {
        double initialLoss = riskManagementService.getCurrentDailyLoss();
        assertEquals(0.0, initialLoss);
    }
    
}
