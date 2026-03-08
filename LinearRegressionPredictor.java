package com.stockpredictor.predictor;

import com.stockpredictor.model.PredictionResult;
import com.stockpredictor.model.StockData;

import java.time.LocalDate;
import java.util.List;

/**
 * Ordinary Least Squares linear regression on closing prices.
 * Uses the last {@code windowSize} bars as training data.
 */
public class LinearRegressionPredictor implements Predictor {

    private final int windowSize;

    public LinearRegressionPredictor(int windowSize) {
        this.windowSize = windowSize;
    }

    @Override
    public String getName() { return "Linear Regression"; }

    @Override
    public int minDataPoints() { return windowSize; }

    @Override
    public PredictionResult predict(List<StockData> history, int daysAhead) {
        int n = Math.min(windowSize, history.size());
        int start = history.size() - n;

        // Build x (time index) and y (close price) arrays
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = i;
            y[i] = history.get(start + i).getClose();
        }

        // OLS: beta = (sum_xy - n*xbar*ybar) / (sum_xx - n*xbar^2)
        double xBar = mean(x);
        double yBar = mean(y);

        double sumXY = 0, sumXX = 0;
        for (int i = 0; i < n; i++) {
            sumXY += x[i] * y[i];
            sumXX += x[i] * x[i];
        }
        double beta  = (sumXY - n * xBar * yBar) / (sumXX - n * xBar * xBar);
        double alpha = yBar - beta * xBar;

        // Predict n + daysAhead - 1
        double xPred = n - 1 + daysAhead;
        double predicted = alpha + beta * xPred;

        // Residual std dev → confidence interval
        double residualSSE = 0;
        for (int i = 0; i < n; i++) {
            double err = y[i] - (alpha + beta * x[i]);
            residualSSE += err * err;
        }
        double rmse = Math.sqrt(residualSSE / n);

        double upper = predicted + 1.96 * rmse;
        double lower = predicted - 1.96 * rmse;

        // R² → confidence
        double ssTot = 0;
        for (double yi : y) ssTot += (yi - yBar) * (yi - yBar);
        double r2 = (ssTot == 0) ? 0.5 : Math.max(0, 1 - residualSSE / ssTot);
        double confidence = 0.3 + r2 * 0.7; // clamp to [0.3, 1.0]

        LocalDate lastDate = history.get(history.size() - 1).getDate();
        LocalDate predDate = lastDate.plusDays(daysAhead);

        return new PredictionResult(predDate, predicted, upper, lower, confidence, getName());
    }

    private double mean(double[] arr) {
        double sum = 0;
        for (double v : arr) sum += v;
        return sum / arr.length;
    }
}
