package com.trading.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configuration properties for trading parameters and risk management.
 */
@Component
@Data
@ConfigurationProperties(prefix = "trading")
public class TradingProperties {
    
    private RiskConfig risk = new RiskConfig();
    private TradingConfig trading = new TradingConfig();
    private List<String> pairs;
    
    @Data
    public static class RiskConfig {
        private double dailyLossLimit = 500.0;
        private double maxPositionSize = 0.05;  // 5% of portfolio
        private int maxOpenPositions = 5;
        private double stopLossPercentage = 2.0;
    }
    
    @Data
    public static class TradingConfig {
        private boolean enabled = true;
        private boolean paperTrading = true;
        private double initialPortfolioValue = 10000.0;
    }
    
}
