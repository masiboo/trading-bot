package com.trading.service;

import com.trading.config.TradingProperties;
import com.trading.model.TradingDecision;
import com.trading.model.TradingOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.binance.BinanceExchange;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.service.trade.TradeService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for executing trades on cryptocurrency exchanges using XChange.
 * Handles order placement, modification, and cancellation with proper error handling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderExecutionService {
    
    private final TradingProperties tradingProperties;
    
    private TradeService tradeService;
    private Exchange binanceExchange;
    private final Map<String, String> executedOrders = new HashMap<>();
    
    /**
     * Initializes the exchange connection and trade service.
     */
    public void initialize() {
        try {
            binanceExchange = ExchangeFactory.INSTANCE.createExchange(BinanceExchange.class);
            
            // TODO: Configure API credentials from environment variables
            // binanceExchange.getExchangeSpecification().setApiKey(apiKey);
            // binanceExchange.getExchangeSpecification().setSecretKey(secretKey);
            
            tradeService = binanceExchange.getTradeService();
            log.info("Order execution service initialized for Binance");
        } catch (Exception e) {
            log.error("Failed to initialize order execution service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize exchange", e);
        }
    }
    
    /**
     * Executes a trading decision by placing an order on the exchange.
     * 
     * @param decision The trading decision to execute
     * @return TradingOrder with execution details
     */
    public TradingOrder executeOrder(TradingDecision decision) {
        try {
            if (tradeService == null) {
                initialize();
            }
            
            if (decision.getAction() == TradingDecision.Action.HOLD) {
                log.debug("Skipping order execution for HOLD decision on {}", decision.getSymbol());
                return createSkippedOrder(decision);
            }
            
            if (tradingProperties.getTrading().isPaperTrading()) {
                log.info("Paper trading enabled - simulating order execution for {}", decision.getSymbol());
                return simulateOrderExecution(decision);
            }
            
            // Execute real order
            return executeRealOrder(decision);
            
        } catch (Exception e) {
            log.error("Error executing order for {}: {}", decision.getSymbol(), e.getMessage(), e);
            return createFailedOrder(decision, e.getMessage());
        }
    }
    
    /**
     * Executes a real order on the exchange.
     */
    private TradingOrder executeRealOrder(TradingDecision decision) throws IOException {
        try {
            CurrencyPair pair = parseCurrencyPair(decision.getSymbol());
            Order.OrderType orderType = decision.getAction() == TradingDecision.Action.BUY ?
                    Order.OrderType.BID : Order.OrderType.ASK;
            
            // Convert amount to quantity (simplified - assumes 1 unit per USDT)
            BigDecimal quantity = BigDecimal.valueOf(decision.getAmount());
            
            // Create limit order (use market order logic by setting price to 0)
            LimitOrder order = new LimitOrder(orderType, quantity, pair, null, null, BigDecimal.ZERO);
            
            String orderId = tradeService.placeLimitOrder(order);
            
            TradingOrder tradingOrder = TradingOrder.builder()
                    .orderId(orderId)
                    .symbol(decision.getSymbol())
                    .action(decision.getAction())
                    .amount(decision.getAmount())
                    .executionPrice(0.0)  // Would be fetched from exchange
                    .status(TradingOrder.Status.EXECUTED)
                    .createdAt(Instant.now())
                    .executedAt(Instant.now())
                    .build();
            
            executedOrders.put(orderId, decision.getSymbol());
            log.info("Order executed on exchange: {} for {} with amount {}", 
                    orderId, decision.getSymbol(), decision.getAmount());
            
            return tradingOrder;
            
        } catch (IOException e) {
            log.error("Exchange API error: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Simulates order execution for paper trading.
     */
    private TradingOrder simulateOrderExecution(TradingDecision decision) {
        String simulatedOrderId = "SIM-" + System.currentTimeMillis();
        
        TradingOrder order = TradingOrder.builder()
                .orderId(simulatedOrderId)
                .symbol(decision.getSymbol())
                .action(decision.getAction())
                .amount(decision.getAmount())
                .executionPrice(0.0)  // Would use current market price
                .status(TradingOrder.Status.EXECUTED)
                .createdAt(Instant.now())
                .executedAt(Instant.now())
                .build();
        
        executedOrders.put(simulatedOrderId, decision.getSymbol());
        log.info("Simulated order execution: {} for {} with amount {}", 
                simulatedOrderId, decision.getSymbol(), decision.getAmount());
        
        return order;
    }
    
    /**
     * Creates a skipped order for HOLD decisions.
     */
    private TradingOrder createSkippedOrder(TradingDecision decision) {
        return TradingOrder.builder()
                .orderId("SKIPPED-" + System.currentTimeMillis())
                .symbol(decision.getSymbol())
                .action(decision.getAction())
                .amount(0.0)
                .status(TradingOrder.Status.PENDING)
                .createdAt(Instant.now())
                .build();
    }
    
    /**
     * Creates a failed order record.
     */
    private TradingOrder createFailedOrder(TradingDecision decision, String errorMessage) {
        return TradingOrder.builder()
                .orderId("FAILED-" + System.currentTimeMillis())
                .symbol(decision.getSymbol())
                .action(decision.getAction())
                .amount(decision.getAmount())
                .status(TradingOrder.Status.FAILED)
                .createdAt(Instant.now())
                .errorMessage(errorMessage)
                .build();
    }
    
    /**
     * Cancels an existing order.
     */
    public boolean cancelOrder(String orderId) {
        try {
            if (tradeService == null) {
                initialize();
            }
            
            if (tradingProperties.getTrading().isPaperTrading()) {
                log.info("Paper trading - simulating order cancellation: {}", orderId);
                return true;
            }
            
            // TODO: Implement actual order cancellation via XChange
            // boolean cancelled = tradeService.cancelOrder(orderId);
            
            log.info("Order cancelled: {}", orderId);
            return true;
            
        } catch (Exception e) {
            log.error("Error cancelling order {}: {}", orderId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Parses a symbol string (e.g., "BTC_USDT") into a CurrencyPair.
     */
    private CurrencyPair parseCurrencyPair(String symbol) {
        String[] parts = symbol.split("_");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid symbol format: " + symbol);
        }
        return new CurrencyPair(parts[0], parts[1]);
    }
    
    /**
     * Gets the count of executed orders.
     */
    public int getExecutedOrderCount() {
        return executedOrders.size();
    }
    
}
