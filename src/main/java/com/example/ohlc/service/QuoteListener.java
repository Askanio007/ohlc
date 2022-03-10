package com.example.ohlc.service;

import com.example.ohlc.dto.Quote;

public interface QuoteListener {
    void onQuote(Quote quote);
}
