package com.trading.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.trading.config.InfluxDBConfig;
import com.trading.model.MarketData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for storing and retrieving market data from InfluxDB.
 * Handles time-series data operations for OHLCV and technical indicators.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class MarketDataRepository {
    
    private final InfluxDBClient influxDBClient;
    private final InfluxDBConfig influxDBConfig;
    
    /**
     * Saves market data to InfluxDB.
     */
    public void saveMarketData(MarketData marketData) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            
            Point point = Point.measurement("market_data")
                    .addTag("symbol", marketData.getSymbol())
                    .addField("open", marketData.getOpen())
                    .addField("high", marketData.getHigh())
                    .addField("low", marketData.getLow())
                    .addField("close", marketData.getClose())
                    .addField("volume", marketData.getVolume());
            
            // Add technical indicators if available
            if (marketData.getRsi() != null) {
                point.addField("rsi", marketData.getRsi());
            }
            if (marketData.getMacd() != null) {
                point.addField("macd", marketData.getMacd());
            }
            if (marketData.getBollingerUpper() != null) {
                point.addField("bollinger_upper", marketData.getBollingerUpper());
            }
            if (marketData.getBollingerLower() != null) {
                point.addField("bollinger_lower", marketData.getBollingerLower());
            }
            
            point.time(marketData.getTimestamp().toEpochMilli(), WritePrecision.MS);
            
            writeApi.writePoint(influxDBConfig.getBucket(), influxDBConfig.getOrg(), point);
            log.debug("Saved market data for {} at {}", marketData.getSymbol(), marketData.getTimestamp());
            
        } catch (Exception e) {
            log.error("Error saving market data for {}: {}", marketData.getSymbol(), e.getMessage(), e);
            throw new RuntimeException("Failed to save market data", e);
        }
    }
    
    /**
     * Retrieves the latest market data for a symbol.
     */
    public MarketData getLatestMarketData(String symbol) {
        try {
            String query = String.format(
                    "from(bucket:\"%s\") " +
                    "|> range(start: -1h) " +
                    "|> filter(fn: (r) => r[\"_measurement\"] == \"market_data\" and r[\"symbol\"] == \"%s\") " +
                    "|> sort(columns: [\"_time\"], desc: true) " +
                    "|> limit(n: 1)",
                    influxDBConfig.getBucket(), symbol
            );
            
            List<FluxTable> tables = influxDBClient.getQueryApi().query(query, influxDBConfig.getOrg());
            
            if (tables.isEmpty() || tables.get(0).getRecords().isEmpty()) {
                log.warn("No market data found for symbol: {}", symbol);
                return null;
            }
            
            return parseFluxRecordsToMarketData(tables.get(0).getRecords(), symbol).stream()
                    .findFirst()
                    .orElse(null);
            
        } catch (Exception e) {
            log.error("Error retrieving latest market data for {}: {}", symbol, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve market data", e);
        }
    }
    
    /**
     * Retrieves historical market data for a symbol within a time range.
     */
    public List<MarketData> getHistoricalMarketData(String symbol, String timeRange) {
        try {
            String query = String.format(
                    "from(bucket:\"%s\") " +
                    "|> range(start: %s) " +
                    "|> filter(fn: (r) => r[\"_measurement\"] == \"market_data\" and r[\"symbol\"] == \"%s\") " +
                    "|> sort(columns: [\"_time\"])",
                    influxDBConfig.getBucket(), timeRange, symbol
            );
            
            List<FluxTable> tables = influxDBClient.getQueryApi().query(query, influxDBConfig.getOrg());
            
            List<MarketData> result = new ArrayList<>();
            for (FluxTable table : tables) {
                result.addAll(parseFluxRecordsToMarketData(table.getRecords(), symbol));
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Error retrieving historical market data for {}: {}", symbol, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve historical market data", e);
        }
    }
    
    /**
     * Parses Flux query records into MarketData objects.
     */
    private List<MarketData> parseFluxRecordsToMarketData(List<FluxRecord> records, String symbol) {
        List<MarketData> dataList = new ArrayList<>();
        MarketData.MarketDataBuilder builder = null;
        
        for (FluxRecord record : records) {
            String field = record.getField();
            Object value = record.getValue();
            Instant timestamp = record.getTime();
            
            if (builder == null || !builder.build().getTimestamp().equals(timestamp)) {
                if (builder != null) {
                    dataList.add(builder.build());
                }
                builder = MarketData.builder()
                        .symbol(symbol)
                        .timestamp(timestamp);
            }
            
            if (builder != null) {
                switch (field) {
                    case "open" -> builder.open((Double) value);
                    case "high" -> builder.high((Double) value);
                    case "low" -> builder.low((Double) value);
                    case "close" -> builder.close((Double) value);
                    case "volume" -> builder.volume((Double) value);
                    case "rsi" -> builder.rsi((Double) value);
                    case "macd" -> builder.macd((Double) value);
                    case "bollinger_upper" -> builder.bollingerUpper((Double) value);
                    case "bollinger_lower" -> builder.bollingerLower((Double) value);
                }
            }
        }
        
        if (builder != null) {
            dataList.add(builder.build());
        }
        
        return dataList;
    }
    
}
