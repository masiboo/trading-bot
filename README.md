# AI-Powered Cryptocurrency Trading Bot

A Spring Boot 3.4 microservice built with Java 21 that automates cryptocurrency trading using AI-driven price predictions, integrated with InfluxDB 2.7 for time-series data storage.

## Features

- **Real-time Market Data Ingestion**: Fetches cryptocurrency data from Binance via XChange library
- **Fiat Currency Integration**: Retrieves forex exchange rates from Fixer.io API
- **AI Price Predictions**: Uses Deep Java Library (DJL) for hourly price movement predictions
- **Trading Strategy Engine**: Generates buy/sell/hold decisions based on AI signals and technical indicators
- **Risk Management**: Enforces daily loss limits, position sizing, and exposure monitoring
- **Order Execution**: Places market orders on exchanges with proper error handling
- **Hourly Automation**: Scheduled trading cycle execution every hour
- **Paper Trading Mode**: Simulate trades without real capital
- **Comprehensive Monitoring**: REST endpoints for health checks and metrics

## Architecture

```
Market Data Sources
        ↓
Data Ingestion Service (XChange, Fixer.io)
        ↓
InfluxDB 2.7 (Time-Series Storage)
        ↓
AI Prediction Service (DJL)
        ↓
Trading Strategy Service
        ↓
Risk Management Service
        ↓
Order Execution Service (XChange)
        ↓
Exchange APIs (Binance, etc.)
```

## Prerequisites

- **Java 21**: Required for Spring Boot 3.4
- **Maven 3.8+**: For dependency management and building
- **InfluxDB 2.7**: Running locally on `localhost:8086`
- **Docker** (optional): For running InfluxDB in a container

## Setup

### 1. Start InfluxDB

If using Docker:

```bash
docker run -p 8086:8086 influxdb:2.7
```

### 2. Configure InfluxDB Credentials

Update `src/main/resources/application.yml` with your InfluxDB credentials:

```yaml
influxdb:
  url: http://localhost:8086
  token: YOUR_INFLUXDB_TOKEN
  org: admin
  bucket: trading_data
  username: admin
```

### 3. Build the Project

```bash
mvn clean package
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

Or run the JAR directly:

```bash
java -jar target/trading-bot-1.0.0.jar
```

The application will start on `http://localhost:8080`

## Configuration

### Trading Parameters

Edit `src/main/resources/application.yml` to configure:

- **Risk Management**:
  - `daily-loss-limit`: Maximum daily loss in USD (default: $500)
  - `max-position-size`: Maximum position size as % of portfolio (default: 5%)
  - `max-open-positions`: Maximum concurrent open positions (default: 5)
  - `stop-loss-percentage`: Stop-loss threshold (default: 2%)

- **Trading Settings**:
  - `enabled`: Enable/disable trading (default: true)
  - `paper-trading`: Simulate trades without real capital (default: true)
  - `initial-portfolio-value`: Starting portfolio value (default: $10,000)

- **Trading Pairs**:
  ```yaml
  trading:
    pairs:
      - BTC_USDT
      - ETH_USDT
      - BNB_USDT
  ```

### API Keys

Set environment variables for exchange and API credentials:

```bash
export BINANCE_API_KEY=your_api_key
export BINANCE_SECRET_KEY=your_secret_key
export FIXER_API_KEY=your_fixer_api_key
```

## API Endpoints

### Health Check

```bash
GET /api/trading/health
```

Response:
```json
{
  "status": "UP",
  "timestamp": 1707561234567
}
```

### Trading Metrics

```bash
GET /api/trading/metrics
```

Response:
```json
{
  "dailyLoss": 0.0,
  "openPositions": 0,
  "executedOrders": 0,
  "timestamp": 1707561234567
}
```

### Trading Status

```bash
GET /api/trading/status
```

Response:
```json
{
  "running": true,
  "dailyLoss": 0.0,
  "openPositions": 0,
  "executedOrders": 0,
  "timestamp": 1707561234567
}
```

## Hourly Trading Cycle

The trading bot executes a complete trading cycle every hour at the start of the hour (00:00 UTC):

1. **Data Ingestion** (Step 1):
   - Fetches latest OHLCV data for all configured trading pairs from Binance
   - Stores data in InfluxDB for historical analysis

2. **Forex Data** (Step 2):
   - Retrieves current exchange rates for fiat currencies

3. **AI Prediction & Execution** (Step 3):
   - Generates price movement predictions using DJL models
   - Creates trading decisions based on predictions and technical indicators
   - Validates trades against risk management rules
   - Executes orders on the exchange

4. **Summary Logging** (Step 4):
   - Reports successful trades, blocked trades, and execution time
   - Updates portfolio value and daily loss tracking

## Project Structure

```
trading-bot/
├── src/
│   ├── main/
│   │   ├── java/com/trading/
│   │   │   ├── TradingBotApplication.java
│   │   │   ├── config/
│   │   │   │   ├── InfluxDBConfig.java
│   │   │   │   ├── TradingProperties.java
│   │   │   │   └── WebClientConfig.java
│   │   │   ├── controller/
│   │   │   │   └── TradingController.java
│   │   │   ├── model/
│   │   │   │   ├── MarketData.java
│   │   │   │   ├── TradingDecision.java
│   │   │   │   └── TradingOrder.java
│   │   │   ├── repository/
│   │   │   │   └── MarketDataRepository.java
│   │   │   ├── scheduler/
│   │   │   │   └── HourlyTradingScheduler.java
│   │   │   └── service/
│   │   │       ├── AIPredictionService.java
│   │   │       ├── CryptoDataIngestionService.java
│   │   │       ├── FiatDataIngestionService.java
│   │   │       ├── OrderExecutionService.java
│   │   │       ├── RiskManagementService.java
│   │   │       └── TradingStrategyService.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/trading/service/
│           ├── RiskManagementServiceTest.java
│           └── TradingStrategyServiceTest.java
├── pom.xml
├── README.md
└── .gitignore
```

## Key Services

### CryptoDataIngestionService
Fetches real-time cryptocurrency market data from Binance using XChange library.

### FiatDataIngestionService
Retrieves forex exchange rates from Fixer.io REST API.

### MarketDataRepository
Stores and queries time-series market data in InfluxDB.

### AIPredictionService
Generates hourly price movement predictions using Deep Java Library (DJL).
- Supports LSTM, CNN-LSTM, and Transformer models
- Provides mock predictions for testing

### TradingStrategyService
Interprets AI predictions and generates trading decisions.
- Applies confidence thresholds
- Uses technical indicators (RSI, Bollinger Bands, MACD)
- Implements position sizing

### RiskManagementService
Enforces safety parameters before trade execution.
- Daily loss limit tracking
- Position size validation
- Open position monitoring
- Volatility checks

### OrderExecutionService
Executes trades on exchanges via XChange.
- Supports paper trading mode
- Handles order placement and cancellation
- Implements error handling and retries

## Testing

Run the unit tests:

```bash
mvn test
```

Test coverage includes:
- Risk management validation
- Trading strategy decision making
- Order execution logic

## Monitoring

The application logs detailed information about each trading cycle:

```
========== Starting hourly trading cycle at 2024-02-11T10:00:00Z ==========
Step 1: Fetching market data...
Fetched market data for BTC_USDT: Close=$50000.00
Step 2: Fetching fiat exchange rates...
Fetched exchange rates: {EUR=0.92, GBP=0.79}
Step 3: Generating predictions and executing trades...
AI Prediction for BTC_USDT: UP with confidence 0.75
Trading decision for BTC_USDT: BUY (amount: $200.00)
Order executed: SIM-1707561234567 for BTC_USDT (P&L: $1.00)
========== Hourly trading cycle completed ==========
Summary: 1 successful trades, 0 blocked trades, duration: 1234ms
Current portfolio value: $10001.00
Daily loss: $0.00
Open positions: 1
```

## Security Considerations

1. **API Keys**: Never commit API keys to version control. Use environment variables.
2. **Paper Trading**: Always start with paper trading mode enabled.
3. **Risk Limits**: Set conservative daily loss limits and position sizes.
4. **Monitoring**: Monitor logs and metrics regularly for unusual activity.
5. **Rate Limiting**: Be aware of exchange API rate limits.

## Future Enhancements

- [ ] Integrate actual DJL models for production predictions
- [ ] Add support for more exchanges (Kraken, Coinbase, etc.)
- [ ] Implement advanced technical indicators
- [ ] Add database persistence for orders and trades
- [ ] Create web dashboard for monitoring
- [ ] Implement backtesting framework
- [ ] Add WebSocket support for real-time data
- [ ] Implement machine learning model retraining pipeline

## Troubleshooting

### InfluxDB Connection Error
Ensure InfluxDB is running and accessible:
```bash
curl http://localhost:8086/health
```

### XChange Exchange Error
Verify API credentials are set correctly and exchange is accessible.

### No Market Data
Check that trading pairs are correctly configured and Binance API is accessible.

## License

MIT License - See LICENSE file for details

## Support

For issues and questions, please refer to the documentation or create an issue in the repository.

## References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [XChange Library](https://github.com/knowm/XChange)
- [Deep Java Library (DJL)](https://djl.ai/)
- [InfluxDB Java Client](https://github.com/influxdata/influxdb-client-java)
- [Fixer.io API](https://fixer.io/)
