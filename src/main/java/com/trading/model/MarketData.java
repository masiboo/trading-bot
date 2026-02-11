package com.trading.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents OHLCV (Open, High, Low, Close, Volume) market data for a trading pair.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketData {
    
    private String symbol;           // e.g., BTC_USDT
    private Instant timestamp;
    private double open;
    private double high;
    private double low;
    private double close;
    private double volume;
    
    // Technical indicators
    private Double rsi;              // Relative Strength Index
    private Double macd;             // MACD value
    private Double bollingerUpper;   // Bollinger Bands upper
    private Double bollingerLower;   // Bollinger Bands lower
    
}
