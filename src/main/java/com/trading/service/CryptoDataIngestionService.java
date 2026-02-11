package com.trading.service;

import com.trading.model.MarketData;
import com.trading.repository.MarketDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.binance.BinanceExchange;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;

/**
 * Service for ingesting cryptocurrency market data from exchanges via XChange.
 * Fetches real-time OHLCV data from Binance and other supported exchanges.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CryptoDataIngestionService {
    
    private final MarketDataRepository marketDataRepository;
    private MarketDataService marketDataService;
    private Exchange binanceExchange;
    
    /**
     * Initializes the XChange exchange connection.
     */
    public void initialize() {
        try {
            binanceExchange = ExchangeFactory.INSTANCE.createExchange(BinanceExchange.class);
            marketDataService = binanceExchange.getMarketDataService();
            log.info("Binance exchange initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Binance exchange: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize exchange", e);
        }
    }
    
    /**
     * Fetches the latest ticker data for a cryptocurrency pair.
     */
    public Ticker getLatestTicker(String symbol) throws IOException {
        if (marketDataService == null) {
            initialize();
        }
        
        try {
            CurrencyPair pair = parseCurrencyPair(symbol);
            Ticker ticker = marketDataService.getTicker(pair);
            log.debug("Fetched ticker for {}: {}", symbol, ticker);
            return ticker;
        } catch (IOException e) {
            log.error("Error fetching ticker for {}: {}", symbol, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Fetches and stores current market data for a symbol.
     * This is a simplified version that uses ticker data.
     * For production, implement proper OHLCV candle fetching.
     */
    public MarketData fetchAndStoreMarketData(String symbol) {
        try {
            Ticker ticker = getLatestTicker(symbol);
            
            MarketData marketData = MarketData.builder()
                    .symbol(symbol)
                    .timestamp(Instant.now())
                    .open(ticker.getOpen() != null ? ticker.getOpen().doubleValue() : 0)
                    .high(ticker.getHigh() != null ? ticker.getHigh().doubleValue() : 0)
                    .low(ticker.getLow() != null ? ticker.getLow().doubleValue() : 0)
                    .close(ticker.getLast() != null ? ticker.getLast().doubleValue() : 0)
                    .volume(ticker.getVolume() != null ? ticker.getVolume().doubleValue() : 0)
                    .build();
            
            marketDataRepository.saveMarketData(marketData);
            log.info("Stored market data for {}", symbol);
            
            return marketData;
            
        } catch (Exception e) {
            log.error("Error fetching and storing market data for {}: {}", symbol, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch market data", e);
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
    
}
