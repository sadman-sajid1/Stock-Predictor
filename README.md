# 📈 Java Stock Price Predictor

A complete stock price prediction system built entirely in Java — no external libraries required.

---

## Features

- **4 Prediction Models**
  - Linear Regression (OLS on closing price history)
  - Moving Average Blend (EMA + SMA with RSI momentum adjustment)
  - Momentum (RSI + MACD + Bollinger Bands composite signal)
  - Ensemble (confidence-weighted average of all models)

- **Technical Indicators**
  - SMA, EMA, RSI, MACD, Bollinger Bands, ATR

- **Walk-forward Back-testing**
  - MAE, RMSE, and directional accuracy per model

- **ASCII Price Chart** rendered in the terminal

- **Real CSV Support** (Yahoo Finance format) or built-in synthetic data generator

---

## Project Structure

```
StockPredictor/
├── src/main/java/com/stockpredictor/
│   ├── Main.java                          ← entry point
│   ├── model/
│   │   ├── StockData.java                 ← OHLCV record
│   │   └── PredictionResult.java          ← prediction output
│   ├── predictor/
│   │   ├── Predictor.java                 ← interface
│   │   ├── LinearRegressionPredictor.java
│   │   ├── MovingAveragePredictor.java
│   │   ├── MomentumPredictor.java
│   │   └── EnsemblePredictor.java
│   ├── service/
│   │   ├── StockDataService.java          ← CSV loader / data store
│   │   └── PredictionService.java         ← orchestrator + backtester
│   ├── ui/
│   │   └── ConsoleRenderer.java           ← ASCII charts & tables
│   └── util/
│       └── TechnicalIndicators.java       ← SMA, EMA, RSI, MACD, ATR...
└── src/main/resources/
    └── AAPL_sample.csv                    ← 50-day sample data
```

---

## Quick Start

### 1. Compile & Run (bash helper)
```bash
chmod +x run.sh
./run.sh               # interactive demo (AAPL, MSFT, GOOGL, TSLA, AMZN)
./run.sh NVDA 3        # predict NVDA 3 days ahead (synthetic data)
```

### 2. Manual Compile & Run
```bash
# Compile
find src -name "*.java" | xargs javac -d out

# Interactive demo
java -cp out com.stockpredictor.Main

# Predict a ticker
java -cp out com.stockpredictor.Main AAPL 1

# Load a real Yahoo Finance CSV
java -cp out com.stockpredictor.Main --csv AAPL src/main/resources/AAPL_sample.csv 5
```

---

## Loading Real Data (Yahoo Finance)

1. Go to https://finance.yahoo.com → Search ticker → Historical Data → Download
2. Save as e.g. `data/AAPL.csv`
3. Run:
   ```bash
   ./run.sh --csv AAPL data/AAPL.csv
   ```

CSV must have headers: `Date,Open,High,Low,Close,Volume`

---

## Prediction Signals

| Signal | Confidence | Meaning |
|--------|-----------|---------|
| BUY    | ≥ 65%     | Model indicates upward momentum |
| HOLD   | 35–65%    | No clear directional edge |
| SELL   | ≤ 35%     | Model indicates downward pressure |

> ⚠️ **Disclaimer**: This is an educational project. Predictions are not financial advice.
> No model reliably predicts stock prices. Always do your own research.

---

## How the Models Work

### Linear Regression
Fits a straight line through the last 30 closing prices using OLS (Ordinary Least Squares).
Extrapolates the line to `daysAhead`. Confidence is derived from R².

### Moving Average Blend (EMA/SMA)
Combines a 10-day EMA (short-term trend) and a 30-day SMA (long-term trend) in a 60/40 blend.
Adds a momentum nudge proportional to RSI deviation from 50. Bullish when EMA > SMA.

### Momentum (RSI + MACD + Bollinger)
Generates a composite signal from three indicators:
- RSI: oversold (<30) → bullish, overbought (>70) → bearish
- MACD histogram: positive → bullish
- Bollinger Bands: price outside bands → mean-reversion expected

### Ensemble
Combines all three models using confidence-weighted averaging. Typically the most stable.

---

## Requirements

- Java 17+ (uses `switch` expressions and `List.of`)
- No external dependencies

---

## Extending the Project

To add a new prediction model:
1. Create a class in `predictor/` implementing `Predictor`
2. Register it in `PredictionService.allPredictors`

To add a new indicator:
1. Add a static method to `util/TechnicalIndicators.java`
