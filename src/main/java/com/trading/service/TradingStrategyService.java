package com.trading.service;

import com.trading.model.MarketData;
import com.trading.model.TradingDecision;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for generating trading decisions based on AI predictions and trading rules.
 * Interprets AI signals and combines them with technical analysis to create buy/sell/hold decisions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TradingStrategyService {
    
    private static final double CONFIDENCE_THRESHOLD = 0.65;  // Minimum confidence to trade
    private static final double TRADE_SIZE_PERCENTAGE = 0.02;  // 2% of portfolio per trade
    
    /**
     * Generates a trading decision based on AI prediction and current market data.
     * 
     * @param symbol The trading pair
     * @param prediction AI price prediction
     * @param currentMarketData Current market data with technical indicators
     * @param portfolioValue Current portfolio value for position sizing
     * @return Trading decision (BUY, SELL, or HOLD)
     */
    public TradingDecision makeDecision(String symbol, 
                                       AIPredictionService.PricePrediction prediction,
                                       MarketData currentMarketData,
                                       double portfolioValue) {
        
        try {
            // If confidence is too low, hold
            if (prediction.confidence < CONFIDENCE_THRESHOLD) {
                log.debug("Confidence too low ({}) for {}, holding", prediction.confidence, symbol);
                return createHoldDecision(symbol, "Low confidence prediction");
            }
            
            // Apply additional technical analysis filters
            TradingDecision.Action action = determineAction(prediction, currentMarketData);
            
            if (action == TradingDecision.Action.HOLD) {
                return createHoldDecision(symbol, "Technical analysis filters not met");
            }
            
            // Calculate position size
            double tradeAmount = portfolioValue * TRADE_SIZE_PERCENTAGE;
            
            return TradingDecision.builder()
                    .symbol(symbol)
                    .action(action)
                    .amount(tradeAmount)
                    .confidence(prediction.confidence)
                    .reason(String.format("AI prediction: %s with %.2f%% confidence", 
                            prediction.direction, prediction.confidence * 100))
                    .timestamp(Instant.now())
                    .build();
            
        } catch (Exception e) {
            log.error("Error making trading decision for {}: {}", symbol, e.getMessage(), e);
            return createHoldDecision(symbol, "Error in decision making");
        }
    }
    
    /**
     * Determines the trading action based on AI prediction and technical indicators.
     */
    private TradingDecision.Action determineAction(AIPredictionService.PricePrediction prediction,
                                                   MarketData marketData) {
        
        // Strong UP signal with good RSI
        if (prediction.direction == AIPredictionService.Direction.UP && 
            prediction.confidence > 0.7) {
            
            // Check RSI if available (RSI < 70 means not overbought)
            if (marketData.getRsi() == null || marketData.getRsi() < 70) {
                return TradingDecision.Action.BUY;
            }
        }
        
        // Strong DOWN signal with good RSI
        if (prediction.direction == AIPredictionService.Direction.DOWN && 
            prediction.confidence > 0.7) {
            
            // Check RSI if available (RSI > 30 means not oversold)
            if (marketData.getRsi() == null || marketData.getRsi() > 30) {
                return TradingDecision.Action.SELL;
            }
        }
        
        // Moderate signals with additional confirmation
        if (prediction.direction == AIPredictionService.Direction.UP && 
            prediction.confidence > 0.6) {
            
            // Check if price is above Bollinger lower band
            if (marketData.getBollingerLower() == null || 
                marketData.getClose() > marketData.getBollingerLower()) {
                return TradingDecision.Action.BUY;
            }
        }
        
        if (prediction.direction == AIPredictionService.Direction.DOWN && 
            prediction.confidence > 0.6) {
            
            // Check if price is below Bollinger upper band
            if (marketData.getBollingerUpper() == null || 
                marketData.getClose() < marketData.getBollingerUpper()) {
                return TradingDecision.Action.SELL;
            }
        }
        
        return TradingDecision.Action.HOLD;
    }
    
    /**
     * Creates a HOLD decision with a reason.
     */
    private TradingDecision createHoldDecision(String symbol, String reason) {
        return TradingDecision.builder()
                .symbol(symbol)
                .action(TradingDecision.Action.HOLD)
                .amount(0.0)
                .confidence(0.5)
                .reason(reason)
                .timestamp(Instant.now())
                .build();
    }
    
}
