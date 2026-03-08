package com.stockpredictor.predictor;

import com.stockpredictor.model.PredictionResult;
import com.stockpredictor.model.StockData;

import java.util.List;

/**
 * Contract that every prediction algorithm must implement.
 */
public interface Predictor {

    /** Human-readable name of the algorithm. */
    String getName();

    /**
     * Predicts the closing price for the next trading day.
     *
     * @param history  historical data sorted oldest → newest (min size varies per impl)
     * @param daysAhead  how many days ahead to project (1 = next day)
     * @return PredictionResult containing price estimate and confidence metrics
     */
    PredictionResult predict(List<StockData> history, int daysAhead);

    /** Minimum number of data points required by this model. */
    int minDataPoints();
}
