package com.stockpredictor;

import com.stockpredictor.model.PredictionResult;
import com.stockpredictor.model.StockData;
import com.stockpredictor.predictor.*;
import com.stockpredictor.service.PredictionService;
import com.stockpredictor.service.StockDataService;
import com.stockpredictor.ui.ConsoleRenderer;

import java.util.*;

/**
 * Entry point for the Stock Price Predictor application.
 *
 * Usage:
 *   java -cp . com.stockpredictor.Main                         (interactive demo)
 *   java -cp . com.stockpredictor.Main <ticker> [daysAhead]   (predict specific ticker)
 *   java -cp . com.stockpredictor.Main --csv <ticker> <file>  (load real CSV data)
 */
public class Main {

    public static void main(String[] args) throws Exception {
        StockDataService   dataService       = new StockDataService();
        PredictionService  predictionService = new PredictionService(dataService);
        ConsoleRenderer    renderer          = new ConsoleRenderer();

        renderer.printBanner();

        // ── Parse arguments ───────────────────────────────────────────────────

        if (args.length >= 3 && args[0].equalsIgnoreCase("--csv")) {
            // Load real CSV: --csv AAPL data/AAPL.csv
            String ticker = args[1];
            String csvPath = args[2];
            dataService.loadFromCsv(ticker, csvPath);
            int days = args.length >= 4 ? Integer.parseInt(args[3]) : 1;
            runAnalysis(ticker, days, dataService, predictionService, renderer);

        } else if (args.length >= 1 && !args[0].startsWith("--")) {
            // Quick predict: AAPL [5]
            String ticker = args[0].toUpperCase();
            int days = args.length >= 2 ? Integer.parseInt(args[1]) : 1;
            dataService.generateSyntheticData(ticker, seedPrice(ticker), 365);
            runAnalysis(ticker, days, dataService, predictionService, renderer);

        } else {
            // Interactive demo with popular tickers
            runInteractiveDemo(dataService, predictionService, renderer);
        }
    }

    // ── Full analysis for one ticker ─────────────────────────────────────────

    private static void runAnalysis(String ticker, int daysAhead,
                                    StockDataService dataService,
                                    PredictionService predictionService,
                                    ConsoleRenderer renderer) {

        List<StockData> history = dataService.getHistory(ticker);
        if (history.isEmpty()) {
            System.err.println("No data for ticker: " + ticker);
            return;
        }

        renderer.printSeparator();
        renderer.printSection("Analysing: " + ticker + "  |  " + history.size() + " bars of history");

        // Technical indicators
        renderer.printIndicators(history);

        // Predictions
        List<PredictionResult> predictions = predictionService.predictAll(ticker, daysAhead);
        renderer.printPredictions(ticker, predictions);

        // Price chart (use ensemble or first prediction for the chart marker)
        double chartPrediction = predictions.isEmpty()
                ? history.get(history.size() - 1).getClose()
                : predictions.get(predictions.size() - 1).getPredictedPrice();
        renderer.printPriceChart(ticker, history, chartPrediction);

        // Back-testing
        renderer.printSection("Back-testing (30 bars walk-forward)");
        List<PredictionService.BacktestReport> reports = new ArrayList<>();
        for (Predictor p : predictionService.getAllPredictors()) {
            PredictionService.BacktestReport report = predictionService.backtest(ticker, p, 30);
            if (report != null) reports.add(report);
        }
        renderer.printBacktestResults(reports);
    }

    // ── Demo mode ─────────────────────────────────────────────────────────────

    private static void runInteractiveDemo(StockDataService dataService,
                                           PredictionService predictionService,
                                           ConsoleRenderer renderer) throws Exception {

        // Generate synthetic data for several popular tickers
        String[][] tickers = {
            {"AAPL",  "185.00"},
            {"MSFT",  "415.00"},
            {"GOOGL", "172.00"},
            {"TSLA",  "250.00"},
            {"AMZN",  "195.00"},
        };

        for (String[] t : tickers) {
            dataService.generateSyntheticData(t[0], Double.parseDouble(t[1]), 365);
        }

        System.out.println("\n  Available tickers: " + dataService.getAvailableTickers());
        System.out.println();

        Scanner sc = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.print("\n  Enter ticker (or 'quit'): ");
            String input = sc.nextLine().trim().toUpperCase();

            if (input.equalsIgnoreCase("QUIT") || input.equalsIgnoreCase("Q")) {
                running = false;
                continue;
            }

            if (!dataService.hasTicker(input)) {
                // Generate on the fly for unknown tickers
                System.out.printf("  Generating synthetic data for %s...%n", input);
                dataService.generateSyntheticData(input, 100 + new Random().nextDouble() * 400, 365);
            }

            System.out.print("  Days ahead to predict [1]: ");
            String daysInput = sc.nextLine().trim();
            int daysAhead = daysInput.isEmpty() ? 1 : Integer.parseInt(daysInput);
            daysAhead = Math.max(1, Math.min(daysAhead, 30));

            runAnalysis(input, daysAhead, dataService, predictionService, renderer);
        }

        System.out.println("\n  Goodbye! 📉📈\n");
        sc.close();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static double seedPrice(String ticker) {
        return switch (ticker) {
            case "AAPL"  -> 185.0;
            case "MSFT"  -> 415.0;
            case "GOOGL" -> 172.0;
            case "TSLA"  -> 250.0;
            case "AMZN"  -> 195.0;
            case "NVDA"  -> 875.0;
            case "META"  -> 520.0;
            default      -> 100.0 + Math.abs(ticker.hashCode() % 400);
        };
    }
}
