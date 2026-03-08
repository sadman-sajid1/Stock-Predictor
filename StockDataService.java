package com.stockpredictor.service;

import com.stockpredictor.model.StockData;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Loads and manages stock data from CSV files.
 *
 * Expected CSV format (Yahoo Finance style):
 *   Date,Open,High,Low,Close,Volume
 *   2024-01-02,185.23,188.44,184.35,187.23,52345678
 */
public class StockDataService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // In-memory cache: ticker → sorted list of StockData (oldest first)
    private final Map<String, List<StockData>> cache = new LinkedHashMap<>();

    // ── CSV Loading ───────────────────────────────────────────────────────────

    /**
     * Loads a CSV file into the cache under the given ticker symbol.
     * Skips the header row and any malformed lines.
     */
    public void loadFromCsv(String ticker, String csvPath) throws IOException {
        List<StockData> records = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String line;
            boolean isHeader = true;
            while ((line = br.readLine()) != null) {
                if (isHeader) { isHeader = false; continue; }
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length < 6) continue;

                try {
                    LocalDate date   = LocalDate.parse(parts[0].trim(), DATE_FMT);
                    double open      = Double.parseDouble(parts[1].trim());
                    double high      = Double.parseDouble(parts[2].trim());
                    double low       = Double.parseDouble(parts[3].trim());
                    double close     = Double.parseDouble(parts[4].trim());
                    long   volume    = Long.parseLong(parts[5].trim());
                    records.add(new StockData(date, open, high, low, close, volume));
                } catch (Exception ignored) { /* skip bad row */ }
            }
        }

        records.sort(Comparator.comparing(StockData::getDate));
        cache.put(ticker.toUpperCase(), records);
        System.out.printf("[DataService] Loaded %d records for %s%n", records.size(), ticker.toUpperCase());
    }

    /**
     * Generates realistic synthetic data for a ticker so the app works
     * without any real CSV file (useful for demos and testing).
     */
    public void generateSyntheticData(String ticker, double startPrice, int days) {
        List<StockData> records = new ArrayList<>();
        Random rng = new Random(ticker.hashCode()); // deterministic per ticker

        LocalDate date  = LocalDate.now().minusDays(days);
        double price    = startPrice;
        double trend    = 0.0002;   // slight upward drift

        for (int i = 0; i < days; i++) {
            // Skip weekends
            while (date.getDayOfWeek().getValue() > 5) date = date.plusDays(1);

            double dailyReturn = trend + (rng.nextGaussian() * 0.015);
            price = price * (1 + dailyReturn);

            double high    = price * (1 + Math.abs(rng.nextGaussian() * 0.008));
            double low     = price * (1 - Math.abs(rng.nextGaussian() * 0.008));
            double open    = low + rng.nextDouble() * (high - low);
            long   volume  = (long) (1_000_000 + rng.nextGaussian() * 300_000);
            volume         = Math.max(100_000, volume);

            records.add(new StockData(date, open, high, low, price, volume));
            date = date.plusDays(1);
        }

        records.sort(Comparator.comparing(StockData::getDate));
        cache.put(ticker.toUpperCase(), records);
        System.out.printf("[DataService] Generated %d synthetic records for %s (seed price $%.2f)%n",
                records.size(), ticker.toUpperCase(), startPrice);
    }

    // ── Data Access ───────────────────────────────────────────────────────────

    public List<StockData> getHistory(String ticker) {
        return cache.getOrDefault(ticker.toUpperCase(), Collections.emptyList());
    }

    public List<StockData> getHistory(String ticker, int lastN) {
        List<StockData> all = getHistory(ticker);
        if (all.size() <= lastN) return all;
        return all.subList(all.size() - lastN, all.size());
    }

    public Optional<StockData> getLatest(String ticker) {
        List<StockData> data = getHistory(ticker);
        if (data.isEmpty()) return Optional.empty();
        return Optional.of(data.get(data.size() - 1));
    }

    public Set<String> getAvailableTickers() {
        return Collections.unmodifiableSet(cache.keySet());
    }

    public boolean hasTicker(String ticker) {
        List<StockData> d = cache.get(ticker.toUpperCase());
        return d != null && !d.isEmpty();
    }

    // ── CSV Export ────────────────────────────────────────────────────────────

    public void exportToCsv(String ticker, String outputPath) throws IOException {
        List<StockData> data = getHistory(ticker);
        try (PrintWriter pw = new PrintWriter(new FileWriter(outputPath))) {
            pw.println("Date,Open,High,Low,Close,Volume");
            for (StockData sd : data) {
                pw.printf("%s,%.4f,%.4f,%.4f,%.4f,%d%n",
                        sd.getDate(), sd.getOpen(), sd.getHigh(),
                        sd.getLow(), sd.getClose(), sd.getVolume());
            }
        }
        System.out.printf("[DataService] Exported %d records to %s%n", data.size(), outputPath);
    }
}
