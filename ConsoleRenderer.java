package com.stockpredictor.ui;

import com.stockpredictor.model.PredictionResult;
import com.stockpredictor.model.StockData;

import java.util.List;

/**
 * Renders information to the terminal with ASCII charts and formatted tables.
 */
public class ConsoleRenderer {

    private static final int CHART_WIDTH  = 70;
    private static final int CHART_HEIGHT = 16;

    // ANSI colour codes
    private static final String RESET  = "\u001B[0m";
    private static final String BOLD   = "\u001B[1m";
    private static final String GREEN  = "\u001B[32m";
    private static final String RED    = "\u001B[31m";
    private static final String CYAN   = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE   = "\u001B[34m";
    private static final String MAGENTA= "\u001B[35m";
    private static final String WHITE  = "\u001B[37m";

    // ── Banner ────────────────────────────────────────────────────────────────

    public void printBanner() {
        System.out.println(CYAN + BOLD);
        System.out.println("╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║         📈  JAVA STOCK PRICE PREDICTOR  v1.0                        ║");
        System.out.println("║         Linear Regression · Moving Average · Momentum · Ensemble    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝");
        System.out.println(RESET);
    }

    // ── Price Chart ──────────────────────────────────────────────────────────

    /**
     * Renders an ASCII line chart of the last {@code maxBars} closing prices,
     * with the predicted price shown as a separate marker at the right edge.
     */
    public void printPriceChart(String ticker, List<StockData> history, double predictedPrice) {
        int n = Math.min(history.size(), CHART_WIDTH - 2);
        List<StockData> slice = history.subList(history.size() - n, history.size());

        double minPrice = slice.stream().mapToDouble(StockData::getLow).min().orElse(0);
        double maxPrice = slice.stream().mapToDouble(StockData::getHigh).max().orElse(1);
        minPrice = Math.min(minPrice, predictedPrice) * 0.995;
        maxPrice = Math.max(maxPrice, predictedPrice) * 1.005;
        double range = maxPrice - minPrice;

        char[][] canvas = new char[CHART_HEIGHT][CHART_WIDTH + 4];
        for (char[] row : canvas) java.util.Arrays.fill(row, ' ');

        // Y-axis labels + grid
        for (int row = 0; row < CHART_HEIGHT; row++) {
            double price = maxPrice - (range * row / (CHART_HEIGHT - 1));
            String label = String.format("$%7.2f │", price);
            for (int c = 0; c < label.length() && c < CHART_WIDTH + 4; c++) {
                canvas[row][c] = label.charAt(c);
            }
        }

        // Plot price bars as vertical lines
        int labelOffset = 10;
        for (int i = 0; i < slice.size(); i++) {
            StockData sd = slice.get(i);
            int col = labelOffset + i;
            if (col >= CHART_WIDTH + 4) break;

            int highRow  = (int) ((maxPrice - sd.getHigh())  / range * (CHART_HEIGHT - 1));
            int lowRow   = (int) ((maxPrice - sd.getLow())   / range * (CHART_HEIGHT - 1));
            int closeRow = (int) ((maxPrice - sd.getClose()) / range * (CHART_HEIGHT - 1));

            for (int row = Math.max(0, highRow); row <= Math.min(CHART_HEIGHT - 1, lowRow); row++) {
                canvas[row][col] = '│';
            }
            if (closeRow >= 0 && closeRow < CHART_HEIGHT) canvas[closeRow][col] = '●';
        }

        // Predicted price marker
        int predRow = (int) ((maxPrice - predictedPrice) / range * (CHART_HEIGHT - 1));
        predRow = Math.max(0, Math.min(CHART_HEIGHT - 1, predRow));
        int predCol = Math.min(labelOffset + slice.size() + 1, CHART_WIDTH + 3);
        if (predCol < CHART_WIDTH + 4) canvas[predRow][predCol] = '◆';

        // Print
        System.out.println(BOLD + CYAN + "\n  ── Price Chart: " + ticker + " ──" + RESET);
        for (char[] row : canvas) {
            System.out.println("  " + new String(row));
        }
        System.out.println("  " + " ".repeat(labelOffset) + "─".repeat(slice.size()) + "→  predicted ◆");
        System.out.println();
    }

    // ── Predictions Table ─────────────────────────────────────────────────────

    public void printPredictions(String ticker, List<PredictionResult> predictions) {
        if (predictions.isEmpty()) {
            System.out.println(RED + "  No predictions available." + RESET);
            return;
        }

        System.out.println(BOLD + CYAN + "\n  ── Predictions for " + ticker + " ──" + RESET);
        System.out.println("  " + "─".repeat(82));
        System.out.printf("  %-40s │ %8s │ %12s │ %8s │ %6s%n",
                "Model", "Predicted", "95% CI", "Conf.", "Signal");
        System.out.println("  " + "─".repeat(82));

        for (PredictionResult r : predictions) {
            String signalColor = switch (r.getSignal()) {
                case "BUY"  -> GREEN;
                case "SELL" -> RED;
                default     -> YELLOW;
            };
            System.out.printf("  %-40s │ " + BOLD + "%8.2f" + RESET + " │ %6.2f–%-6.2f │ %6.1f%% │ "
                            + signalColor + BOLD + "%6s" + RESET + "%n",
                    r.getModelName(),
                    r.getPredictedPrice(),
                    r.getLowerBound(), r.getUpperBound(),
                    r.getConfidence() * 100,
                    r.getSignal());
        }
        System.out.println("  " + "─".repeat(82));
    }

    // ── Back-test Results ─────────────────────────────────────────────────────

    public void printBacktestResults(List<com.stockpredictor.service.PredictionService.BacktestReport> reports) {
        System.out.println(BOLD + CYAN + "\n  ── Back-test Results ──" + RESET);
        System.out.println("  " + "─".repeat(75));
        for (var r : reports) System.out.println("  " + r);
        System.out.println("  " + "─".repeat(75));
    }

    // ── Technical Indicators ─────────────────────────────────────────────────

    public void printIndicators(List<StockData> history) {
        com.stockpredictor.util.TechnicalIndicators ti = null; // static methods only
        double sma20  = com.stockpredictor.util.TechnicalIndicators.sma(history, 20);
        double sma50  = com.stockpredictor.util.TechnicalIndicators.sma(history, 50);
        double ema12  = com.stockpredictor.util.TechnicalIndicators.ema(history, 12);
        double rsi14  = com.stockpredictor.util.TechnicalIndicators.rsi(history, 14);
        double[] bb   = com.stockpredictor.util.TechnicalIndicators.bollingerBands(history, 20, 2.0);
        double[] macd = com.stockpredictor.util.TechnicalIndicators.macd(history, 12, 26, 9);
        double atr    = com.stockpredictor.util.TechnicalIndicators.atr(history, 14);

        double lastClose = history.get(history.size() - 1).getClose();

        System.out.println(BOLD + CYAN + "\n  ── Technical Indicators ──" + RESET);
        System.out.println("  " + "─".repeat(45));
        printIndicatorRow("Last Close",  String.format("$%.2f", lastClose));
        printIndicatorRow("SMA 20",      fmt(sma20));
        printIndicatorRow("SMA 50",      fmt(sma50));
        printIndicatorRow("EMA 12",      fmt(ema12));
        printIndicatorRow("RSI 14",      Double.isNaN(rsi14) ? "N/A" : String.format("%.1f", rsi14)
                + "  " + rsiLabel(rsi14));
        printIndicatorRow("BB Upper",    fmt(bb[2]));
        printIndicatorRow("BB Middle",   fmt(bb[1]));
        printIndicatorRow("BB Lower",    fmt(bb[0]));
        printIndicatorRow("MACD Line",   Double.isNaN(macd[0]) ? "N/A" : String.format("%.4f", macd[0]));
        printIndicatorRow("MACD Signal", Double.isNaN(macd[1]) ? "N/A" : String.format("%.4f", macd[1]));
        printIndicatorRow("ATR 14",      fmt(atr));
        System.out.println("  " + "─".repeat(45));
    }

    private void printIndicatorRow(String label, String value) {
        System.out.printf("  %-16s │ %s%n", label, value);
    }

    private String fmt(double v) {
        return Double.isNaN(v) ? "N/A" : String.format("$%.2f", v);
    }

    private String rsiLabel(double rsi) {
        if (Double.isNaN(rsi)) return "";
        if (rsi < 30) return RED + "(Oversold)" + RESET;
        if (rsi > 70) return GREEN + "(Overbought)" + RESET;
        return YELLOW + "(Neutral)" + RESET;
    }

    // ── Separator ─────────────────────────────────────────────────────────────

    public void printSeparator() {
        System.out.println(BLUE + "\n  " + "═".repeat(80) + RESET);
    }

    public void printSection(String title) {
        System.out.println("\n" + BOLD + MAGENTA + "  ▶ " + title + RESET);
    }
}
