package com.example.ohlc.dao;

import com.example.ohlc.dto.OhlcPeriod;
import com.example.ohlc.entity.Ohlc;

import java.util.List;

public interface OhlcDao {
    void store(Ohlc ohlc);
    /** loads OHLCs from DB selected by parameters and sorted by
     periodStartUtcTimestamp in descending order */
    List<Ohlc> getHistorical (long instrumentId, OhlcPeriod period);
}
