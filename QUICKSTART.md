# Quick Start Guide

## Prerequisites

- Java 21 installed
- InfluxDB 2.7 running
- Maven 3.8+ (optional, if building from source)

## Step 1: Start InfluxDB

If using Docker:

```bash
docker run -d \
  -p 8086:8086 \
  -e INFLUXDB_DB=trading_data \
  influxdb:2.7
```

## Step 2: Configure InfluxDB

Access InfluxDB UI at `http://localhost:8086` and create:
- Organization: `admin`
- Bucket: `trading_data`
- API Token: (copy and save)

## Step 3: Update Configuration

Edit `src/main/resources/application.yml`:

```yaml
influxdb:
  url: http://localhost:8086
  token: YOUR_INFLUXDB_TOKEN
  org: admin
  bucket: trading_data
```

## Step 4: Build the Project

```bash
mvn clean package
```

## Step 5: Run the Application

```bash
java -jar target/trading-bot-1.0.0.jar
```

Or using Maven:

```bash
mvn spring-boot:run
```

## Step 6: Verify It's Running

Check the health endpoint:

```bash
curl http://localhost:8080/api/trading/health
```

Expected response:
```json
{
  "status": "UP",
  "timestamp": 1707561234567
}
```

## Step 7: Monitor Trading

Get current metrics:

```bash
curl http://localhost:8080/api/trading/metrics
```

View logs:

```bash
tail -f logs/trading-bot.log
```

## Configuration Options

### Paper Trading Mode (Recommended for Testing)

Edit `application.yml`:

```yaml
trading:
  trading:
    paper-trading: true  # Set to false for live trading
```

### Trading Pairs

Add or remove pairs in `application.yml`:

```yaml
trading:
  pairs:
    - BTC_USDT
    - ETH_USDT
    - BNB_USDT
```

### Risk Parameters

Adjust risk limits:

```yaml
trading:
  risk:
    daily-loss-limit: 500.0      # Max daily loss in USD
    max-position-size: 0.05       # Max 5% per trade
    max-open-positions: 5         # Max concurrent positions
    stop-loss-percentage: 2.0     # 2% stop loss
```

## Hourly Trading Cycle

The bot automatically executes every hour at the start of the hour:

1. **00:00 UTC** - Fetch market data, generate predictions, execute trades
2. **01:00 UTC** - Repeat cycle
3. **And so on...**

Check logs for execution details:

```
========== Starting hourly trading cycle at 2024-02-11T10:00:00Z ==========
Step 1: Fetching market data...
Step 2: Fetching fiat exchange rates...
Step 3: Generating predictions and executing trades...
========== Hourly trading cycle completed ==========
```

## Troubleshooting

### InfluxDB Connection Error

```
Error: Failed to connect to InfluxDB
```

**Solution:**
1. Verify InfluxDB is running: `curl http://localhost:8086/health`
2. Check credentials in `application.yml`
3. Ensure bucket exists in InfluxDB

### No Market Data

```
Warn: No market data found for symbol: BTC_USDT
```

**Solution:**
1. Verify Binance API is accessible
2. Check internet connection
3. Verify trading pairs are correctly configured

### Exchange API Error

```
Error: Exchange API error
```

**Solution:**
1. Check API rate limits
2. Verify API credentials (if using live trading)
3. Check exchange status page

## Next Steps

1. **Test with Paper Trading**: Run with `paper-trading: true` for 24 hours
2. **Monitor Performance**: Check daily P&L and trade statistics
3. **Tune Parameters**: Adjust risk limits and trading pairs based on results
4. **Add AI Models**: Integrate trained DJL models for better predictions
5. **Go Live**: Set `paper-trading: false` and `enabled: true` when confident

## Support

For issues:
1. Check logs in `logs/` directory
2. Review `application.yml` configuration
3. Verify InfluxDB connectivity
4. Check API rate limits and credentials

## Security Reminder

- **Never commit API keys** to version control
- **Use environment variables** for sensitive data
- **Start with paper trading** before using real capital
- **Monitor logs regularly** for unusual activity
