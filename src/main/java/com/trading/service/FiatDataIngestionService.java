package com.trading.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for ingesting fiat currency exchange rates from external APIs.
 * Supports Fixer.io and similar REST APIs for forex data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FiatDataIngestionService {
    
    @Value("${fixer.api-key:demo}")
    private String fixerApiKey;
    
    @Value("${fixer.base-url:http://data.fixer.io/api/latest}")
    private String fixerBaseUrl;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    /**
     * Fetches the latest exchange rates for a base currency.
     * Example: getLatestExchangeRates("EUR", "USD,GBP,JPY")
     */
    public Map<String, Double> getLatestExchangeRates(String baseCurrency, String targetCurrencies) {
        try {
            String url = String.format("%s?access_key=%s&base=%s&symbols=%s",
                    fixerBaseUrl, fixerApiKey, baseCurrency, targetCurrencies);
            
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            return parseFixerResponse(response);
            
        } catch (Exception e) {
            log.error("Error fetching exchange rates for {}: {}", baseCurrency, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch exchange rates", e);
        }
    }
    
    /**
     * Fetches historical exchange rates for a specific date.
     */
    public Map<String, Double> getHistoricalExchangeRates(String date, String baseCurrency, String targetCurrencies) {
        try {
            String url = String.format("%s?access_key=%s&base=%s&symbols=%s&date=%s",
                    fixerBaseUrl, fixerApiKey, baseCurrency, targetCurrencies, date);
            
            String response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            return parseFixerResponse(response);
            
        } catch (Exception e) {
            log.error("Error fetching historical exchange rates for date {}: {}", date, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch historical exchange rates", e);
        }
    }
    
    /**
     * Parses Fixer.io API response and extracts exchange rates.
     */
    private Map<String, Double> parseFixerResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            
            if (!root.has("success") || !root.get("success").asBoolean()) {
                log.warn("Fixer API returned unsuccessful response: {}", jsonResponse);
                return new HashMap<>();
            }
            
            Map<String, Double> rates = new HashMap<>();
            JsonNode ratesNode = root.get("rates");
            
            if (ratesNode != null && ratesNode.isObject()) {
                ratesNode.fields().forEachRemaining(entry ->
                        rates.put(entry.getKey(), entry.getValue().asDouble())
                );
            }
            
            log.debug("Parsed {} exchange rates from Fixer API", rates.size());
            return rates;
            
        } catch (Exception e) {
            log.error("Error parsing Fixer API response: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse exchange rates", e);
        }
    }
    
}
