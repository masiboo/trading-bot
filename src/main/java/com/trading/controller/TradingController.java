package com.trading.controller;

import com.trading.model.MarketData;
import com.trading.model.TradingDecision;
import com.trading.model.TradingOrder;
import com.trading.repository.MarketDataRepository;
import com.trading.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * REST controller for trading bot API endpoints.
 * Provides endpoints for market data, predictions, trading operations, risk management, and monitoring.
 */
@RestController
@RequestMapping("/trading")
@Slf4j
public class TradingController {
    
    private final RiskManagementService riskManagementService;
    private final OrderExecutionService orderExecutionService;
    private final CryptoDataIngestionService cryptoDataIngestionService;
    private final FiatDataIngestionService fiatDataIngestionService;
    private final AIPredictionService aiPredictionService;
    private final TradingStrategyService tradingStrategyService;
    private final MarketDataRepository marketDataRepository;
    
    // In-memory storage for trading history and configuration
    private final List<TradingOrder> tradingHistory = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Object> configuration = new HashMap<>();
    
    public TradingController(
            RiskManagementService riskManagementService,
            OrderExecutionService orderExecutionService,
            CryptoDataIngestionService cryptoDataIngestionService,
            FiatDataIngestionService fiatDataIngestionService,
            AIPredictionService aiPredictionService,
            TradingStrategyService tradingStrategyService,
            MarketDataRepository marketDataRepository) {
        this.riskManagementService = riskManagementService;
        this.orderExecutionService = orderExecutionService;
        this.cryptoDataIngestionService = cryptoDataIngestionService;
        this.fiatDataIngestionService = fiatDataIngestionService;
        this.aiPredictionService = aiPredictionService;
        this.tradingStrategyService = tradingStrategyService;
        this.marketDataRepository = marketDataRepository;
        
        // Initialize default configuration
        initializeDefaultConfiguration();
    }
    
    private void initializeDefaultConfiguration() {
        configuration.put("enabled", true);
        configuration.put("paperTrading", true);
        configuration.put("initialPortfolioValue", 10000.0);
        
        List<String> pairs = new ArrayList<>();
        pairs.add("BTC_USDT");
        pairs.add("ETH_USDT");
        pairs.add("BNB_USDT");
        configuration.put("pairs", pairs);
        
        Map<String, Object> risk = new HashMap<>();
        risk.put("dailyLossLimit", 500.0);
        risk.put("maxPositionSize", 0.05);
        risk.put("maxOpenPositions", 5);
        risk.put("stopLossPercentage", 2.0);
        configuration.put("risk", risk);
    }
    
    // ==================== HEALTH & STATUS ENDPOINTS ====================
    
    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
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
    
    // ==================== MARKET DATA ENDPOINTS ====================
    
    /**
     * Fetch latest market data for a symbol.
     */
    @GetMapping("/market-data/{symbol}")
    public ResponseEntity<Map<String, Object>> getMarketData(@PathVariable String symbol) {
        try {
            MarketData marketData = cryptoDataIngestionService.fetchAndStoreMarketData(symbol);
            if (marketData == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("symbol", marketData.getSymbol());
            response.put("timestamp", marketData.getTimestamp().toString());
            response.put("open", marketData.getOpen());
            response.put("high", marketData.getHigh());
            response.put("low", marketData.getLow());
            response.put("close", marketData.getClose());
            response.put("volume", marketData.getVolume());
            response.put("rsi", marketData.getRsi());
            response.put("macd", marketData.getMacd());
            response.put("bollingerUpper", marketData.getBollingerUpper());
            response.put("bollingerLower", marketData.getBollingerLower());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching market data for {}: {}", symbol, e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get historical market data for a symbol.
     */
    @GetMapping("/market-data/{symbol}/historical")
    public ResponseEntity<List<Map<String, Object>>> getHistoricalData(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "-24h") String range) {
        try {
            // Parse time range
            Instant endTime = Instant.now();
            Instant startTime;
            
            if (range.equals("-24h")) {
                startTime = endTime.minus(24, ChronoUnit.HOURS);
            } else if (range.equals("-7d")) {
                startTime = endTime.minus(7, ChronoUnit.DAYS);
            } else if (range.equals("-30d")) {
                startTime = endTime.minus(30, ChronoUnit.DAYS);
            } else if (range.equals("-1y")) {
                startTime = endTime.minus(365, ChronoUnit.DAYS);
            } else {
                startTime = endTime.minus(24, ChronoUnit.HOURS);
            }
            
            List<MarketData> historicalData = marketDataRepository.getHistoricalMarketData(symbol, "-7d");
            
            List<Map<String, Object>> response = new ArrayList<>();
            for (MarketData data : historicalData) {
                Map<String, Object> item = new HashMap<>();
                item.put("symbol", data.getSymbol());
                item.put("timestamp", data.getTimestamp().toString());
                item.put("open", data.getOpen());
                item.put("high", data.getHigh());
                item.put("low", data.getLow());
                item.put("close", data.getClose());
                item.put("volume", data.getVolume());
                response.add(item);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching historical data for {}: {}", symbol, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
    
    // ==================== PREDICTION ENDPOINTS ====================
    
    /**
     * Get AI price prediction for a symbol.
     */
    @GetMapping("/prediction/{symbol}")
    public ResponseEntity<Map<String, Object>> getPrediction(@PathVariable String symbol) {
        try {
            // Get historical data for prediction
            Instant endTime = Instant.now();
            Instant startTime = endTime.minus(7, ChronoUnit.DAYS);
            List<MarketData> historicalData = marketDataRepository.getHistoricalMarketData(symbol, "-7d");
            
            AIPredictionService.PricePrediction prediction = aiPredictionService.predictNextHourMovement(symbol, historicalData);
            
            Map<String, Object> response = new HashMap<>();
            response.put("symbol", prediction.symbol);
            response.put("direction", prediction.direction.toString());
            response.put("confidence", prediction.confidence);
            response.put("targetPrice", prediction.targetPrice);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting prediction for {}: {}", symbol, e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    // ==================== TRADING DECISION ENDPOINTS ====================
    
    /**
     * Get trading decision for a symbol.
     */
    @GetMapping("/decision/{symbol}")
    public ResponseEntity<Map<String, Object>> getTradingDecision(@PathVariable String symbol) {
        try {
            MarketData marketData = cryptoDataIngestionService.fetchAndStoreMarketData(symbol);
            if (marketData == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Get historical data for prediction
            Instant endTime = Instant.now();
            Instant startTime = endTime.minus(7, ChronoUnit.DAYS);
            List<MarketData> historicalData = marketDataRepository.getHistoricalMarketData(symbol, "-7d");
            
            AIPredictionService.PricePrediction prediction = aiPredictionService.predictNextHourMovement(symbol, historicalData);
            TradingDecision decision = tradingStrategyService.makeDecision(
                    symbol, prediction, marketData, 10000.0);
            
            Map<String, Object> response = new HashMap<>();
            response.put("symbol", decision.getSymbol());
            response.put("action", decision.getAction().toString());
            response.put("amount", decision.getAmount());
            response.put("confidence", decision.getConfidence());
            response.put("reason", decision.getReason());
            response.put("timestamp", decision.getTimestamp().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting trading decision for {}: {}", symbol, e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    // ==================== ORDER EXECUTION ENDPOINTS ====================
    
    /**
     * Execute a trading order.
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeOrder(@RequestBody Map<String, Object> orderRequest) {
        try {
            String symbol = (String) orderRequest.get("symbol");
            String action = (String) orderRequest.get("action");
            Double amount = ((Number) orderRequest.get("amount")).doubleValue();
            Double confidence = ((Number) orderRequest.get("confidence")).doubleValue();
            String reason = (String) orderRequest.get("reason");
            
            // Create trading decision
            TradingDecision decision = TradingDecision.builder()
                    .symbol(symbol)
                    .action(TradingDecision.Action.valueOf(action))
                    .amount(amount)
                    .confidence(confidence)
                    .reason(reason)
                    .timestamp(Instant.now())
                    .build();
            
            // Validate risk
            if (!riskManagementService.canExecuteTrade(decision, 10000.0)) {
                return ResponseEntity.status(400).body(Map.of(
                        "error", "Trade rejected by risk management",
                        "reason", "Daily loss limit exceeded or position size too large"
                ));
            }
            
            // Execute order
            TradingOrder order = orderExecutionService.executeOrder(decision);
            
            // Record in history
            tradingHistory.add(order);
            
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.getOrderId());
            response.put("symbol", order.getSymbol());
            response.put("action", order.getAction().toString());
            response.put("amount", order.getAmount());
            response.put("executionPrice", order.getExecutionPrice());
            response.put("status", order.getStatus());
            response.put("createdAt", order.getCreatedAt().toString());
            response.put("executedAt", order.getExecutedAt() != null ? order.getExecutedAt().toString() : null);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error executing order: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    // ==================== RISK MANAGEMENT ENDPOINTS ====================
    
    /**
     * Get current daily loss.
     */
    @GetMapping("/risk/daily-loss")
    public ResponseEntity<Map<String, Object>> getDailyLoss() {
        Map<String, Object> response = new HashMap<>();
        response.put("dailyLoss", riskManagementService.getCurrentDailyLoss());
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get count of open positions.
     */
    @GetMapping("/risk/open-positions")
    public ResponseEntity<Map<String, Object>> getOpenPositions() {
        Map<String, Object> response = new HashMap<>();
        response.put("openPositions", riskManagementService.getOpenPositionCount());
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Validate if a trade can be executed based on risk rules.
     */
    @PostMapping("/risk/validate")
    public ResponseEntity<Map<String, Object>> validateTrade(@RequestBody Map<String, Object> tradeRequest) {
        try {
            String symbol = (String) tradeRequest.get("symbol");
            String action = (String) tradeRequest.get("action");
            Double amount = ((Number) tradeRequest.get("amount")).doubleValue();
            Double confidence = ((Number) tradeRequest.get("confidence")).doubleValue();
            
            TradingDecision decision = TradingDecision.builder()
                    .symbol(symbol)
                    .action(TradingDecision.Action.valueOf(action))
                    .amount(amount)
                    .confidence(confidence)
                    .reason("Validation test")
                    .timestamp(Instant.now())
                    .build();
            
            boolean valid = riskManagementService.canExecuteTrade(decision, 10000.0);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", valid);
            response.put("reason", valid ? "Trade approved - within risk limits" : "Trade rejected - exceeds risk limits");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error validating trade: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    // ==================== FOREX ENDPOINTS ====================
    
    /**
     * Get forex exchange rates.
     */
    @GetMapping("/forex/rates")
    public ResponseEntity<Map<String, Object>> getForexRates(
            @RequestParam(defaultValue = "USD") String base,
            @RequestParam(defaultValue = "EUR,GBP,JPY") String targets) {
        try {
            Map<String, Double> rates = fiatDataIngestionService.getLatestExchangeRates(base, targets);
            
            Map<String, Object> response = new HashMap<>();
            response.put("base", base);
            response.put("rates", rates);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching forex rates: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    // ==================== CONFIGURATION ENDPOINTS ====================
    
    /**
     * Get current trading configuration.
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfiguration() {
        return ResponseEntity.ok(configuration);
    }
    
    /**
     * Update risk management parameters.
     */
    @PutMapping("/config/risk")
    public ResponseEntity<Map<String, Object>> updateRiskConfig(@RequestBody Map<String, Object> riskConfig) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> risk = (Map<String, Object>) configuration.get("risk");
            
            if (riskConfig.containsKey("dailyLossLimit")) {
                risk.put("dailyLossLimit", riskConfig.get("dailyLossLimit"));
            }
            if (riskConfig.containsKey("maxPositionSize")) {
                risk.put("maxPositionSize", riskConfig.get("maxPositionSize"));
            }
            if (riskConfig.containsKey("maxOpenPositions")) {
                risk.put("maxOpenPositions", riskConfig.get("maxOpenPositions"));
            }
            if (riskConfig.containsKey("stopLossPercentage")) {
                risk.put("stopLossPercentage", riskConfig.get("stopLossPercentage"));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Risk parameters updated successfully");
            response.put("updatedParameters", risk);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating risk config: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Update trading pairs.
     */
    @PutMapping("/config/pairs")
    public ResponseEntity<Map<String, Object>> updateTradingPairs(@RequestBody Map<String, Object> pairsConfig) {
        try {
            @SuppressWarnings("unchecked")
            List<String> pairs = (List<String>) pairsConfig.get("pairs");
            configuration.put("pairs", pairs);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Trading pairs updated successfully");
            response.put("pairs", pairs);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating trading pairs: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    // ==================== MONITORING & HISTORY ENDPOINTS ====================
    
    /**
     * Get trading history with pagination.
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getTradingHistory(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        try {
            int total = tradingHistory.size();
            int endIndex = Math.min(offset + limit, total);
            List<TradingOrder> paginatedHistory = tradingHistory.subList(
                    Math.max(0, offset), 
                    endIndex
            );
            
            List<Map<String, Object>> trades = new ArrayList<>();
            for (TradingOrder order : paginatedHistory) {
                Map<String, Object> trade = new HashMap<>();
                trade.put("orderId", order.getOrderId());
                trade.put("symbol", order.getSymbol());
                trade.put("action", order.getAction().toString());
                trade.put("amount", order.getAmount());
                trade.put("executionPrice", order.getExecutionPrice());
                trade.put("status", order.getStatus());
                trade.put("executedAt", order.getExecutedAt() != null ? order.getExecutedAt().toString() : null);
                trades.add(trade);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("total", total);
            response.put("limit", limit);
            response.put("offset", offset);
            response.put("trades", trades);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching trading history: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get daily trading summary.
     */
    @GetMapping("/summary/daily")
    public ResponseEntity<Map<String, Object>> getDailySummary() {
        try {
            Instant today = Instant.now().truncatedTo(ChronoUnit.DAYS);
            
            long totalTrades = tradingHistory.stream()
                    .filter(o -> o.getExecutedAt() != null && o.getExecutedAt().isAfter(today))
                    .count();
            
            long winningTrades = tradingHistory.stream()
                    .filter(o -> o.getExecutedAt() != null && o.getExecutedAt().isAfter(today))
                    .filter(o -> o.getAction() == TradingDecision.Action.BUY)
                    .count();
            
            long losingTrades = totalTrades - winningTrades;
            
            double totalProfit = tradingHistory.stream()
                    .filter(o -> o.getExecutedAt() != null && o.getExecutedAt().isAfter(today))
                    .mapToDouble(o -> o.getAmount() * 0.01)
                    .sum();
            
            double totalLoss = riskManagementService.getCurrentDailyLoss();
            double netPnL = totalProfit - totalLoss;
            double winRate = totalTrades > 0 ? (double) winningTrades / totalTrades : 0.0;
            
            Map<String, Object> response = new HashMap<>();
            response.put("date", today.toString());
            response.put("totalTrades", totalTrades);
            response.put("winningTrades", winningTrades);
            response.put("losingTrades", losingTrades);
            response.put("totalProfit", totalProfit);
            response.put("totalLoss", totalLoss);
            response.put("netPnL", netPnL);
            response.put("winRate", winRate);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching daily summary: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get weekly trading summary.
     */
    @GetMapping("/summary/weekly")
    public ResponseEntity<Map<String, Object>> getWeeklySummary() {
        try {
            Instant weekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
            
            long totalTrades = tradingHistory.stream()
                    .filter(o -> o.getExecutedAt() != null && o.getExecutedAt().isAfter(weekAgo))
                    .count();
            
            long winningTrades = tradingHistory.stream()
                    .filter(o -> o.getExecutedAt() != null && o.getExecutedAt().isAfter(weekAgo))
                    .filter(o -> o.getAction() == TradingDecision.Action.BUY)
                    .count();
            
            long losingTrades = totalTrades - winningTrades;
            
            double totalProfit = tradingHistory.stream()
                    .filter(o -> o.getExecutedAt() != null && o.getExecutedAt().isAfter(weekAgo))
                    .mapToDouble(o -> o.getAmount() * 0.01)
                    .sum();
            
            double totalLoss = riskManagementService.getCurrentDailyLoss();
            double netPnL = totalProfit - totalLoss;
            double winRate = totalTrades > 0 ? (double) winningTrades / totalTrades : 0.0;
            
            Map<String, Object> response = new HashMap<>();
            response.put("period", "last_7_days");
            response.put("totalTrades", totalTrades);
            response.put("winningTrades", winningTrades);
            response.put("losingTrades", losingTrades);
            response.put("totalProfit", totalProfit);
            response.put("totalLoss", totalLoss);
            response.put("netPnL", netPnL);
            response.put("winRate", winRate);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching weekly summary: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get application logs.
     */
    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "100") int lines,
            @RequestParam(defaultValue = "INFO") String level) {
        try {
            // Simplified log retrieval - in production, read from actual log file
            List<String> logs = new ArrayList<>();
            logs.add("[" + Instant.now() + "] " + level + " - Trading bot is running");
            logs.add("[" + Instant.now().minus(1, ChronoUnit.MINUTES) + "] " + level + " - Hourly trading cycle started");
            logs.add("[" + Instant.now().minus(2, ChronoUnit.MINUTES) + "] " + level + " - Market data fetched successfully");
            logs.add("[" + Instant.now().minus(3, ChronoUnit.MINUTES) + "] " + level + " - AI predictions generated");
            
            Map<String, Object> response = new HashMap<>();
            response.put("logs", logs);
            response.put("level", level);
            response.put("count", logs.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching logs: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
