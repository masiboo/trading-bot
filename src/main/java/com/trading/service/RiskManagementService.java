package com.trading.service;

import com.trading.config.TradingProperties;
import com.trading.model.TradingDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for enforcing risk management rules before trade execution.
 * Implements daily loss limits, position sizing, and exposure monitoring.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskManagementService {
    
    private final TradingProperties tradingProperties;
    
    // Track daily P&L
    private final AtomicReference<LocalDate> lastResetDate = new AtomicReference<>(LocalDate.now());
    private final AtomicReference<Double> currentDailyLoss = new AtomicReference<>(0.0);
    
    // Track open positions
    private final Map<String, Integer> openPositions = new HashMap<>();
    
    /**
     * Validates if a trade can be executed based on risk parameters.
     * 
     * @param decision The trading decision to validate
     * @param currentPortfolioValue Current portfolio value
     * @return true if trade is allowed, false otherwise
     */
    public boolean canExecuteTrade(TradingDecision decision, double currentPortfolioValue) {
        try {
            // Reset daily loss if new day
            resetDailyLossIfNewDay();
            
            // Check 1: Daily loss limit
            if (!checkDailyLossLimit(decision)) {
                log.warn("Trade blocked for {}: Daily loss limit exceeded", decision.getSymbol());
                return false;
            }
            
            // Check 2: Position size limit
            if (!checkPositionSize(decision, currentPortfolioValue)) {
                log.warn("Trade blocked for {}: Position size exceeds limit", decision.getSymbol());
                return false;
            }
            
            // Check 3: Maximum open positions
            if (!checkMaxOpenPositions(decision)) {
                log.warn("Trade blocked for {}: Maximum open positions reached", decision.getSymbol());
                return false;
            }
            
            // Check 4: Volatility check (optional)
            if (!checkVolatility(decision)) {
                log.warn("Trade blocked for {}: High volatility detected", decision.getSymbol());
                return false;
            }
            
            log.info("Trade approved for {} with action: {}", decision.getSymbol(), decision.getAction());
            return true;
            
        } catch (Exception e) {
            log.error("Error in risk management check: {}", e.getMessage(), e);
            return false;  // Fail-safe: block trade on error
        }
    }
    
    /**
     * Records the result of a trade execution for P&L tracking.
     */
    public void recordTradeResult(TradingDecision decision, double profitLoss) {
        try {
            // Update daily loss/profit
            double currentLoss = currentDailyLoss.get();
            currentDailyLoss.set(currentLoss - profitLoss);  // Subtract because profit is positive
            
            // Update open positions
            if (decision.getAction() == TradingDecision.Action.BUY) {
                openPositions.merge(decision.getSymbol(), 1, Integer::sum);
            } else if (decision.getAction() == TradingDecision.Action.SELL) {
                openPositions.merge(decision.getSymbol(), -1, Integer::sum);
            }
            
            log.info("Trade result recorded for {}: P&L = {}, Daily Loss = {}",
                    decision.getSymbol(), profitLoss, currentDailyLoss.get());
            
        } catch (Exception e) {
            log.error("Error recording trade result: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Checks if daily loss limit has been exceeded.
     */
    private boolean checkDailyLossLimit(TradingDecision decision) {
        if (decision.getAction() == TradingDecision.Action.HOLD) {
            return true;
        }
        
        double dailyLossLimit = tradingProperties.getRisk().getDailyLossLimit();
        double currentLoss = currentDailyLoss.get();
        
        // Estimate potential loss (simplified: assume 1% loss per trade)
        double potentialLoss = decision.getAmount() * 0.01;
        
        boolean allowed = (currentLoss + potentialLoss) <= dailyLossLimit;
        log.debug("Daily loss check: current={}, potential={}, limit={}, allowed={}",
                currentLoss, potentialLoss, dailyLossLimit, allowed);
        
        return allowed;
    }
    
    /**
     * Checks if position size is within limits.
     */
    private boolean checkPositionSize(TradingDecision decision, double portfolioValue) {
        if (decision.getAction() == TradingDecision.Action.HOLD) {
            return true;
        }
        
        double maxPositionSize = tradingProperties.getRisk().getMaxPositionSize();
        double maxAllowedAmount = portfolioValue * maxPositionSize;
        
        boolean allowed = decision.getAmount() <= maxAllowedAmount;
        log.debug("Position size check: amount={}, max={}, allowed={}",
                decision.getAmount(), maxAllowedAmount, allowed);
        
        return allowed;
    }
    
    /**
     * Checks if maximum open positions limit is reached.
     */
    private boolean checkMaxOpenPositions(TradingDecision decision) {
        if (decision.getAction() == TradingDecision.Action.HOLD || 
            decision.getAction() == TradingDecision.Action.SELL) {
            return true;
        }
        
        int maxOpenPositions = tradingProperties.getRisk().getMaxOpenPositions();
        int currentOpenCount = openPositions.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        
        boolean allowed = currentOpenCount < maxOpenPositions;
        log.debug("Open positions check: current={}, max={}, allowed={}",
                currentOpenCount, maxOpenPositions, allowed);
        
        return allowed;
    }
    
    /**
     * Checks for extreme volatility (placeholder implementation).
     */
    private boolean checkVolatility(TradingDecision decision) {
        // TODO: Implement volatility check using market data
        // For now, always allow
        return true;
    }
    
    /**
     * Resets daily loss tracking if a new day has started.
     */
    private void resetDailyLossIfNewDay() {
        LocalDate today = LocalDate.now();
        LocalDate lastReset = lastResetDate.get();
        
        if (!today.equals(lastReset)) {
            currentDailyLoss.set(0.0);
            openPositions.clear();
            lastResetDate.set(today);
            log.info("Daily loss tracking reset for new day");
        }
    }
    
    /**
     * Gets current daily loss amount.
     */
    public double getCurrentDailyLoss() {
        return currentDailyLoss.get();
    }
    
    /**
     * Gets count of open positions.
     */
    public int getOpenPositionCount() {
        return openPositions.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }
    
}
