package com.example.ohlc.service;

import com.example.ohlc.dto.OhlcPeriod;
import com.example.ohlc.entity.Ohlc;

import java.util.List;

public interface OhlcStorage {
    void store(Ohlc ohlc);
    List<Ohlc> getHistorical(long instrumentId, OhlcPeriod period);
}
