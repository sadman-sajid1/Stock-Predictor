package com.stockpredictor.model;

import java.time.LocalDate;

/**
 * Holds a single price prediction with confidence metrics.
 */
public class PredictionResult {
    private final LocalDate date;
    private final double predictedPrice;
    private final double upperBound;
    private final double lowerBound;
    private final double confidence;      // 0.0 – 1.0
    private final String modelName;
    private final String signal;          // BUY / SELL / HOLD

    public PredictionResult(LocalDate date, double predictedPrice,
                            double upperBound, double lowerBound,
                            double confidence, String modelName) {
        this.date           = date;
        this.predictedPrice = predictedPrice;
        this.upperBound     = upperBound;
        this.lowerBound     = lowerBound;
        this.confidence     = confidence;
        this.modelName      = modelName;
        this.signal         = deriveSignal(confidence);
    }

    private String deriveSignal(double conf) {
        if (conf >= 0.65) return "BUY";
        if (conf <= 0.35) return "SELL";
        return "HOLD";
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public LocalDate getDate()            { return date;           }
    public double    getPredictedPrice()  { return predictedPrice; }
    public double    getUpperBound()      { return upperBound;     }
    public double    getLowerBound()      { return lowerBound;     }
    public double    getConfidence()      { return confidence;     }
    public String    getModelName()       { return modelName;      }
    public String    getSignal()          { return signal;         }

    @Override
    public String toString() {
        return String.format(
            "[%s] %s  →  $%.2f  (%.2f–%.2f)  conf=%.1f%%  signal=%s",
            modelName, date, predictedPrice, lowerBound, upperBound,
            confidence * 100, signal);
    }
}
