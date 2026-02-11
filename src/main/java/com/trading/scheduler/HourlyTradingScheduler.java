package com.trading.scheduler;

import com.trading.config.TradingProperties;
import com.trading.model.MarketData;
import com.trading.model.TradingDecision;
import com.trading.model.TradingOrder;
import com.trading.repository.MarketDataRepository;
import com.trading.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Scheduler for executing the complete hourly trading cycle.
 * Orchestrates data ingestion, AI prediction, strategy execution, and order placement.
 * 
 * Execution schedule: Every hour at the start (0 0 * * * ?)
 * Cron format: second minute hour day month day-of-week
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HourlyTradingScheduler {
    
    private final CryptoDataIngestionService cryptoDataIngestionService;
    private final FiatDataIngestionService fiatDataIngestionService;
    private final MarketDataRepository marketDataRepository;
    private final AIPredictionService aiPredictionService;
    private final TradingStrategyService tradingStrategyService;
    private final RiskManagementService riskManagementService;
    private final OrderExecutionService orderExecutionService;
    private final TradingProperties tradingProperties;
    
    private double currentPortfolioValue;
    
    /**
     * Main hourly trading cycle execution.
     * Triggered at the start of every hour (00:00 UTC).
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void executeHourlyTradingCycle() {
        long startTime = System.currentTimeMillis();
        log.info("========== Starting hourly trading cycle at {} ==========", Instant.now());
        
        try {
            // Initialize portfolio value on first run
            if (currentPortfolioValue == 0) {
                currentPortfolioValue = tradingProperties.getTrading().getInitialPortfolioValue();
                log.info("Initialized portfolio value: ${}", currentPortfolioValue);
            }
            
            // Step 1: Fetch market data for all configured pairs
            log.info("Step 1: Fetching market data...");
            for (String symbol : tradingProperties.getPairs()) {
                try {
                    MarketData marketData = cryptoDataIngestionService.fetchAndStoreMarketData(symbol);
                    if (marketData != null) {
                        log.debug("Fetched market data for {}: Close=${}", symbol, marketData.getClose());
                    }
                } catch (Exception e) {
                    log.error("Error fetching market data for {}: {}", symbol, e.getMessage());
                }
            }
            
            // Step 2: Fetch fiat/forex data (optional)
            log.info("Step 2: Fetching fiat exchange rates...");
            try {
                // Example: Get USD/EUR rates
                var rates = fiatDataIngestionService.getLatestExchangeRates("USD", "EUR,GBP,JPY");
                log.debug("Fetched exchange rates: {}", rates);
            } catch (Exception e) {
                log.warn("Error fetching forex data: {}", e.getMessage());
            }
            
            // Step 3: Generate predictions and execute trades for each pair
            log.info("Step 3: Generating predictions and executing trades...");
            int successfulTrades = 0;
            int blockedTrades = 0;
            
            for (String symbol : tradingProperties.getPairs()) {
                try {
                    // Get latest market data
                    MarketData currentMarketData = marketDataRepository.getLatestMarketData(symbol);
                    if (currentMarketData == null) {
                        log.warn("No market data available for {}, skipping", symbol);
                        continue;
                    }
                    
                    // Get historical data for AI model (last 24 hours)
                    List<MarketData> historicalData = marketDataRepository
                            .getHistoricalMarketData(symbol, "-24h");
                    
                    // Generate AI prediction
                    AIPredictionService.PricePrediction prediction = 
                            aiPredictionService.predictNextHourMovement(symbol, historicalData);
                    
                    log.debug("AI Prediction for {}: {} with confidence {}",
                            symbol, prediction.direction, prediction.confidence);
                    
                    // Make trading decision
                    TradingDecision decision = tradingStrategyService.makeDecision(
                            symbol, prediction, currentMarketData, currentPortfolioValue);
                    
                    log.info("Trading decision for {}: {} (amount: ${})",
                            symbol, decision.getAction(), decision.getAmount());
                    
                    // Check risk management
                    if (!riskManagementService.canExecuteTrade(decision, currentPortfolioValue)) {
                        log.warn("Trade blocked by risk management for {}", symbol);
                        blockedTrades++;
                        continue;
                    }
                    
                    // Execute order
                    TradingOrder order = orderExecutionService.executeOrder(decision);
                    
                    if (order.getStatus() == TradingOrder.Status.EXECUTED ||
                        order.getStatus() == TradingOrder.Status.PENDING) {
                        
                        // Record trade result (simplified: assume 0.5% profit/loss)
                        double profitLoss = decision.getAmount() * 0.005 * 
                                (decision.getAction() == TradingDecision.Action.BUY ? 1 : -1);
                        
                        riskManagementService.recordTradeResult(decision, profitLoss);
                        currentPortfolioValue += profitLoss;
                        
                        log.info("Order executed: {} for {} (P&L: ${})",
                                order.getOrderId(), symbol, profitLoss);
                        
                        successfulTrades++;
                    } else {
                        log.error("Order execution failed for {}: {}",
                                symbol, order.getErrorMessage());
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing {} in trading cycle: {}", symbol, e.getMessage(), e);
                }
            }
            
            // Step 4: Log summary
            long duration = System.currentTimeMillis() - startTime;
            log.info("========== Hourly trading cycle completed ==========");
            log.info("Summary: {} successful trades, {} blocked trades, duration: {}ms",
                    successfulTrades, blockedTrades, duration);
            log.info("Current portfolio value: ${}", currentPortfolioValue);
            log.info("Daily loss: ${}", riskManagementService.getCurrentDailyLoss());
            log.info("Open positions: {}", riskManagementService.getOpenPositionCount());
            
        } catch (Exception e) {
            log.error("Critical error in hourly trading cycle: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Optional: Health check scheduled every 30 minutes.
     */
    @Scheduled(cron = "0 */30 * * * ?")
    public void healthCheck() {
        log.debug("Health check: Portfolio value: ${}, Daily loss: ${}",
                currentPortfolioValue, riskManagementService.getCurrentDailyLoss());
    }
    
}
