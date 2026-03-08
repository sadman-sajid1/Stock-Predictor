package com.stockpredictor.util;

import com.stockpredictor.model.StockData;

import java.util.ArrayList;
import java.util.List;

/**
 * Stateless utility class for common technical indicators.
 * All methods accept a list of StockData sorted oldest → newest.
 */
public final class TechnicalIndicators {

    private TechnicalIndicators() { /* utility class */ }

    // ── Simple Moving Average ─────────────────────────────────────────────────

    /**
     * Calculates Simple Moving Average (SMA) for the last {@code period} closing prices.
     * Returns {@code Double.NaN} when not enough data is available.
     */
    public static double sma(List<StockData> data, int period) {
        if (data.size() < period) return Double.NaN;
        double sum = 0;
        int start = data.size() - period;
        for (int i = start; i < data.size(); i++) {
            sum += data.get(i).getClose();
        }
        return sum / period;
    }

    /** Returns the full SMA series aligned to the end of the input list. */
    public static List<Double> smaList(List<StockData> data, int period) {
        List<Double> result = new ArrayList<>();
        for (int i = period - 1; i < data.size(); i++) {
            double sum = 0;
            for (int j = i - period + 1; j <= i; j++) sum += data.get(j).getClose();
            result.add(sum / period);
        }
        return result;
    }

    // ── Exponential Moving Average ────────────────────────────────────────────

    public static List<Double> emaList(List<StockData> data, int period) {
        List<Double> ema = new ArrayList<>();
        if (data.size() < period) return ema;

        double k = 2.0 / (period + 1);
        // Seed with SMA
        double seed = 0;
        for (int i = 0; i < period; i++) seed += data.get(i).getClose();
        seed /= period;
        ema.add(seed);

        for (int i = period; i < data.size(); i++) {
            double prev = ema.get(ema.size() - 1);
            ema.add(data.get(i).getClose() * k + prev * (1 - k));
        }
        return ema;
    }

    public static double ema(List<StockData> data, int period) {
        List<Double> list = emaList(data, period);
        return list.isEmpty() ? Double.NaN : list.get(list.size() - 1);
    }

    // ── RSI ───────────────────────────────────────────────────────────────────

    /**
     * Wilder's Relative Strength Index over {@code period} bars.
     * Typically period = 14.
     */
    public static double rsi(List<StockData> data, int period) {
        if (data.size() <= period) return Double.NaN;

        double avgGain = 0, avgLoss = 0;
        for (int i = 1; i <= period; i++) {
            double change = data.get(i).getClose() - data.get(i - 1).getClose();
            if (change > 0) avgGain += change; else avgLoss -= change;
        }
        avgGain /= period;
        avgLoss /= period;

        for (int i = period + 1; i < data.size(); i++) {
            double change = data.get(i).getClose() - data.get(i - 1).getClose();
            double gain = Math.max(change, 0);
            double loss = Math.max(-change, 0);
            avgGain = (avgGain * (period - 1) + gain) / period;
            avgLoss = (avgLoss * (period - 1) + loss) / period;
        }

        if (avgLoss == 0) return 100;
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    // ── Bollinger Bands ───────────────────────────────────────────────────────

    public static double[] bollingerBands(List<StockData> data, int period, double stdMultiplier) {
        if (data.size() < period) return new double[]{Double.NaN, Double.NaN, Double.NaN};

        double mean = sma(data, period);
        double variance = 0;
        int start = data.size() - period;
        for (int i = start; i < data.size(); i++) {
            double diff = data.get(i).getClose() - mean;
            variance += diff * diff;
        }
        double std = Math.sqrt(variance / period);

        return new double[]{
            mean - stdMultiplier * std,  // lower band
            mean,                         // middle (SMA)
            mean + stdMultiplier * std   // upper band
        };
    }

    // ── MACD ─────────────────────────────────────────────────────────────────

    /**
     * Returns [macdLine, signalLine, histogram].
     * Standard parameters: fast=12, slow=26, signal=9.
     */
    public static double[] macd(List<StockData> data, int fast, int slow, int signalPeriod) {
        double emaFast = ema(data, fast);
        double emaSlow = ema(data, slow);
        if (Double.isNaN(emaFast) || Double.isNaN(emaSlow)) {
            return new double[]{Double.NaN, Double.NaN, Double.NaN};
        }

        double macdLine = emaFast - emaSlow;

        // Signal line: EMA of macd series — approximate with last value for simplicity
        double signalLine = macdLine * (2.0 / (signalPeriod + 1));
        double histogram  = macdLine - signalLine;

        return new double[]{macdLine, signalLine, histogram};
    }

    // ── Average True Range ────────────────────────────────────────────────────

    public static double atr(List<StockData> data, int period) {
        if (data.size() <= period) return Double.NaN;

        double atr = 0;
        for (int i = 1; i <= period; i++) {
            StockData cur  = data.get(i);
            StockData prev = data.get(i - 1);
            double tr = Math.max(cur.getHigh() - cur.getLow(),
                        Math.max(Math.abs(cur.getHigh() - prev.getClose()),
                                 Math.abs(cur.getLow()  - prev.getClose())));
            atr += tr;
        }
        atr /= period;

        for (int i = period + 1; i < data.size(); i++) {
            StockData cur  = data.get(i);
            StockData prev = data.get(i - 1);
            double tr = Math.max(cur.getHigh() - cur.getLow(),
                        Math.max(Math.abs(cur.getHigh() - prev.getClose()),
                                 Math.abs(cur.getLow()  - prev.getClose())));
            atr = (atr * (period - 1) + tr) / period;
        }
        return atr;
    }
}
