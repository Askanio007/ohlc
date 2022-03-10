package com.example.ohlc.entity;

import com.example.ohlc.dto.OhlcPeriod;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RawOhlcDataKey {
    private long instrumentId;
    private OhlcPeriod period;

    public static RawOhlcDataKey of(long instrumentId, OhlcPeriod period) {
        return new RawOhlcDataKey(instrumentId, period);
    }
}
