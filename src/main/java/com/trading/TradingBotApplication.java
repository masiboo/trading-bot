package com.trading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application for AI-powered cryptocurrency trading bot.
 * Enables scheduled tasks for hourly trading cycle execution.
 */
@SpringBootApplication
@EnableScheduling
public class TradingBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradingBotApplication.class, args);
    }

}
