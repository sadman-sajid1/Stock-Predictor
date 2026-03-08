package com.stockpredictor.service;

import com.stockpredictor.model.PredictionResult;
import com.stockpredictor.model.StockData;
import com.stockpredictor.predictor.*;

import java.util.*;

/**
 * Orchestrates all predictors and provides back-testing utilities.
 */
public class PredictionService {

    private final StockDataService dataService;

    private final List<Predictor> allPredictors = List.of(
        new LinearRegressionPredictor(30),
        new MovingAveragePredictor(10, 30),
        new MomentumPredictor(),
        new EnsemblePredictor()
    );

    public PredictionService(StockDataService dataService) {
        this.dataService = dataService;
    }

    // ── Prediction ────────────────────────────────────────────────────────────

    /**
     * Run all models against the given ticker's history.
     * @param ticker   stock symbol
     * @param daysAhead 1 = next trading day
     */
    public List<PredictionResult> predictAll(String ticker, int daysAhead) {
        List<StockData> history = dataService.getHistory(ticker);
        if (history.isEmpty()) {
            System.err.println("[PredictionService] No data for ticker: " + ticker);
            return Collections.emptyList();
        }

        List<PredictionResult> results = new ArrayList<>();
        for (Predictor p : allPredictors) {
            if (history.size() >= p.minDataPoints()) {
                results.add(p.predict(history, daysAhead));
            } else {
                System.out.printf("[PredictionService] Skipping %s — need %d bars, have %d%n",
                        p.getName(), p.minDataPoints(), history.size());
            }
        }
        return results;
    }

    /** Run a single named predictor. */
    public Optional<PredictionResult> predictWith(String ticker, String modelName, int daysAhead) {
        List<StockData> history = dataService.getHistory(ticker);
        return allPredictors.stream()
                .filter(p -> p.getName().equalsIgnoreCase(modelName))
                .filter(p -> history.size() >= p.minDataPoints())
                .map(p -> p.predict(history, daysAhead))
                .findFirst();
    }

    // ── Back-testing ──────────────────────────────────────────────────────────

    /**
     * Simple walk-forward back-test.
     * For each bar in the test window, trains on all prior data and predicts 1 day ahead.
     * Returns MAE, RMSE, directional accuracy, and per-step details.
     */
    public BacktestReport backtest(String ticker, Predictor predictor, int testBars) {
        List<StockData> all = dataService.getHistory(ticker);
        int trainSize = all.size() - testBars;
        if (trainSize < predictor.minDataPoints()) {
            System.err.printf("[Backtest] Not enough data. Need %d train + %d test, have %d%n",
                    predictor.minDataPoints(), testBars, all.size());
            return null;
        }

        List<double[]> steps = new ArrayList<>(); // [actual, predicted]
        int correctDirection = 0;

        for (int t = 0; t < testBars - 1; t++) {
            List<StockData> train = all.subList(0, trainSize + t);
            double actual = all.get(trainSize + t + 1).getClose();
            double prev   = all.get(trainSize + t).getClose();

            PredictionResult pred = predictor.predict(train, 1);
            double predicted = pred.getPredictedPrice();

            steps.add(new double[]{actual, predicted});

            boolean actualUp    = actual    > prev;
            boolean predictedUp = predicted > prev;
            if (actualUp == predictedUp) correctDirection++;
        }

        // Compute MAE, RMSE
        double mae = 0, mse = 0;
        for (double[] step : steps) {
            double err = Math.abs(step[0] - step[1]);
            mae += err;
            mse += err * err;
        }
        mae /= steps.size();
        double rmse = Math.sqrt(mse / steps.size());
        double dirAccuracy = (double) correctDirection / steps.size();

        return new BacktestReport(predictor.getName(), mae, rmse, dirAccuracy, steps.size());
    }

    public List<Predictor> getAllPredictors() { return allPredictors; }

    // ── Inner report class ────────────────────────────────────────────────────

    public static class BacktestReport {
        public final String modelName;
        public final double mae;
        public final double rmse;
        public final double directionalAccuracy;
        public final int    testBars;

        BacktestReport(String modelName, double mae, double rmse, double dirAcc, int testBars) {
            this.modelName           = modelName;
            this.mae                 = mae;
            this.rmse                = rmse;
            this.directionalAccuracy = dirAcc;
            this.testBars            = testBars;
        }

        @Override
        public String toString() {
            return String.format(
                "%-40s | MAE=$%6.2f | RMSE=$%6.2f | Dir.Acc=%.1f%%  (%d bars)",
                modelName, mae, rmse, directionalAccuracy * 100, testBars);
        }
    }
}
