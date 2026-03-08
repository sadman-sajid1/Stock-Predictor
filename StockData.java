package com.stockpredictor.model;

import java.time.LocalDate;

/**
 * Represents a single day's stock data (OHLCV format).
 */
public class StockData {
    private LocalDate date;
    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;

    public StockData(LocalDate date, double open, double high, double low, double close, long volume) {
        this.date   = date;
        this.open   = open;
        this.high   = high;
        this.low    = low;
        this.close  = close;
        this.volume = volume;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public LocalDate getDate()   { return date;   }
    public double    getOpen()   { return open;   }
    public double    getHigh()   { return high;   }
    public double    getLow()    { return low;    }
    public double    getClose()  { return close;  }
    public long      getVolume() { return volume; }

    /** Typical price: average of high, low, and close. */
    public double getTypicalPrice() {
        return (high + low + close) / 3.0;
    }

    @Override
    public String toString() {
        return String.format("StockData{date=%s, open=%.2f, high=%.2f, low=%.2f, close=%.2f, volume=%d}",
                date, open, high, low, close, volume);
    }
}
