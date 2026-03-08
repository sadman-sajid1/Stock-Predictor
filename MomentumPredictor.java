package com.stockpredictor.predictor;

import com.stockpredictor.model.PredictionResult;
import com.stockpredictor.model.StockData;
import com.stockpredictor.util.TechnicalIndicators;

import java.time.LocalDate;
import java.util.List;

/**
 * Momentum predictor that uses RSI, MACD, and Bollinger Bands
 * to estimate the probability of a reversal or continuation.
 */
public class MomentumPredictor implements Predictor {

    @Override
    public String getName() { return "Momentum (RSI + MACD + Bollinger)"; }

    @Override
    public int minDataPoints() { return 30; }

    @Override
    public PredictionResult predict(List<StockData> history, int daysAhead) {
        double rsi       = TechnicalIndicators.rsi(history, 14);
        double[] macd    = TechnicalIndicators.macd(history, 12, 26, 9);
        double[] bbands  = TechnicalIndicators.bollingerBands(history, 20, 2.0);
        double atr       = TechnicalIndicators.atr(history, 14);

        double lastClose = history.get(history.size() - 1).getClose();

        // ── RSI signal ────────────────────────────────────────────────────────
        // RSI < 30 → oversold → expect bounce up
        // RSI > 70 → overbought → expect pullback
        double rsiSignal = 0;
        if (!Double.isNaN(rsi)) {
            if (rsi < 30) rsiSignal = 1.0;
            else if (rsi < 45) rsiSignal = 0.5;
            else if (rsi > 70) rsiSignal = -1.0;
            else if (rsi > 55) rsiSignal = -0.5;
        }

        // ── MACD signal ───────────────────────────────────────────────────────
        double macdSignal = 0;
        if (!Double.isNaN(macd[0]) && !Double.isNaN(macd[2])) {
            macdSignal = macd[2] > 0 ? 0.5 : -0.5;  // histogram above 0 = bullish
        }

        // ── Bollinger Band position ───────────────────────────────────────────
        double bbSignal = 0;
        if (!Double.isNaN(bbands[0])) {
            if (lastClose < bbands[0]) bbSignal =  1.0;   // below lower → mean-revert up
            else if (lastClose > bbands[2]) bbSignal = -1.0;  // above upper → mean-revert down
        }

        // ── Composite signal (weighted average) ───────────────────────────────
        double composite = (rsiSignal * 0.4) + (macdSignal * 0.3) + (bbSignal * 0.3);

        // Price move: composite * ATR * days
        double priceDelta = composite * atr * daysAhead * 0.8;
        double predicted  = lastClose + priceDelta;

        double uncertainty = atr * 1.5 * Math.sqrt(daysAhead);
        double upper = predicted + uncertainty;
        double lower = predicted - uncertainty;

        // Confidence: higher when multiple indicators agree
        double absComposite = Math.abs(composite);
        double confidence   = 0.35 + absComposite * 0.55;

        LocalDate lastDate = history.get(history.size() - 1).getDate();
        LocalDate predDate = lastDate.plusDays(daysAhead);

        return new PredictionResult(predDate, predicted, upper, lower, confidence, getName());
    }
}
