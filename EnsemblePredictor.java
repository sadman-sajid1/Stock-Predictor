package com.stockpredictor.predictor;

import com.stockpredictor.model.PredictionResult;
import com.stockpredictor.model.StockData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Ensemble predictor: combines predictions from multiple models
 * using confidence-weighted averaging.
 */
public class EnsemblePredictor implements Predictor {

    private final List<Predictor> models = new ArrayList<>();

    public EnsemblePredictor() {
        models.add(new LinearRegressionPredictor(30));
        models.add(new MovingAveragePredictor(10, 30));
        models.add(new MomentumPredictor());
    }

    @Override
    public String getName() { return "Ensemble (Weighted Average)"; }

    @Override
    public int minDataPoints() { return 30; }

    @Override
    public PredictionResult predict(List<StockData> history, int daysAhead) {
        List<PredictionResult> results = new ArrayList<>();

        for (Predictor model : models) {
            if (history.size() >= model.minDataPoints()) {
                results.add(model.predict(history, daysAhead));
            }
        }

        if (results.isEmpty()) {
            // Fallback: return last close
            double last = history.get(history.size() - 1).getClose();
            LocalDate date = history.get(history.size() - 1).getDate().plusDays(daysAhead);
            return new PredictionResult(date, last, last * 1.02, last * 0.98, 0.5, getName());
        }

        // Confidence-weighted average
        double totalWeight = 0;
        double weightedPrice = 0;
        double weightedUpper = 0;
        double weightedLower = 0;

        for (PredictionResult r : results) {
            double w = r.getConfidence();
            totalWeight    += w;
            weightedPrice  += r.getPredictedPrice() * w;
            weightedUpper  += r.getUpperBound()     * w;
            weightedLower  += r.getLowerBound()      * w;
        }

        double finalPrice = weightedPrice / totalWeight;
        double finalUpper = weightedUpper / totalWeight;
        double finalLower = weightedLower / totalWeight;

        // Ensemble confidence: average of sub-model confidences, boosted by agreement
        double avgConf = totalWeight / results.size();
        double priceRange = finalUpper - finalLower;
        double spread = priceRange / (finalPrice + 1e-9);
        double agreementBonus = Math.max(0, 0.1 - spread * 0.5);
        double finalConf = Math.min(0.95, avgConf + agreementBonus);

        LocalDate predDate = results.get(0).getDate();

        return new PredictionResult(predDate, finalPrice, finalUpper, finalLower, finalConf, getName());
    }

    public List<Predictor> getModels() { return models; }
}
