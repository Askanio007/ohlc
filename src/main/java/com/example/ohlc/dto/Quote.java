package com.example.ohlc.dto;

public interface Quote {
    double getPrice();
    long getInstrumentId();
    long getUtcTimestamp();
}
