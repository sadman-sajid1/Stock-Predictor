package com.stockpredictor.predictor;

import com.stockpredictor.model.PredictionResult;
import com.stockpredictor.model.StockData;
import com.stockpredictor.util.TechnicalIndicators;

import java.time.LocalDate;
import java.util.List;

/**
 * Weighted Moving Average predictor.
 * Combines a short-term EMA and a long-term SMA with a momentum adjustment
 * derived from the RSI to generate a prediction.
 */
public class MovingAveragePredictor implements Predictor {

    private final int shortPeriod;
    private final int longPeriod;

    public MovingAveragePredictor(int shortPeriod, int longPeriod) {
        this.shortPeriod = shortPeriod;
        this.longPeriod  = longPeriod;
    }

    @Override
    public String getName() { return "Moving Average (EMA/SMA Blend)"; }

    @Override
    public int minDataPoints() { return longPeriod + 1; }

    @Override
    public PredictionResult predict(List<StockData> history, int daysAhead) {
        double shortEma = TechnicalIndicators.ema(history, shortPeriod);
        double longSma  = TechnicalIndicators.sma(history, longPeriod);
        double rsi      = TechnicalIndicators.rsi(history, 14);
        double atr      = TechnicalIndicators.atr(history, 14);

        double lastClose = history.get(history.size() - 1).getClose();

        // Blend EMA and SMA (60/40 weighting)
        double blendedMa = 0.6 * shortEma + 0.4 * longSma;

        // RSI momentum: scale from -1 (oversold) to +1 (overbought), centered at 50
        double rsiMomentum = Double.isNaN(rsi) ? 0 : (rsi - 50) / 50.0;

        // Momentum nudge: move predicted price slightly in RSI direction
        double momentum = rsiMomentum * atr * 0.3;

        double predicted = blendedMa + momentum * daysAhead;
        double upper     = predicted + atr * 1.5 * Math.sqrt(daysAhead);
        double lower     = predicted - atr * 1.5 * Math.sqrt(daysAhead);

        // Confidence: penalise if price is far from moving average
        double deviation = Math.abs(lastClose - blendedMa) / (blendedMa + 1e-9);
        double confidence = Math.max(0.2, 0.75 - deviation * 0.5);

        // Crossover bonus: short EMA above long SMA = bullish
        if (shortEma > longSma) confidence = Math.min(1.0, confidence + 0.1);

        LocalDate lastDate = history.get(history.size() - 1).getDate();
        LocalDate predDate = lastDate.plusDays(daysAhead);

        return new PredictionResult(predDate, predicted, upper, lower, confidence, getName());
    }
}
