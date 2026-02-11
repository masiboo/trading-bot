package com.trading.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for InfluxDB client and connection settings.
 */
@Configuration
@Data
@ConfigurationProperties(prefix = "influxdb")
public class InfluxDBConfig {
    
    private String url;
    private String token;
    private String org;
    private String bucket;
    private String username;
    
    /**
     * Creates and configures the InfluxDB client bean.
     */
    @Bean
    public InfluxDBClient influxDBClient() {
        return InfluxDBClientFactory.create(url, token.toCharArray(), org);
    }
    
}
