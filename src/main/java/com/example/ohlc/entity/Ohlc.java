package com.example.ohlc.entity;

import com.example.ohlc.dto.OhlcPeriod;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Ohlc {
    private long instrumentId;
    private double openPrice;
    private double highPrice;
    private double lowPrice;
    private double closePrice;
    private OhlcPeriod period;
    private long startPeriod;

    public static Ohlc first(long instrumentId, OhlcPeriod period, long startPeriod, double price) {
        return Ohlc.builder()
                .instrumentId(instrumentId)
                .period(period)
                .startPeriod(startPeriod)
                .openPrice(price)
                .closePrice(price)
                .lowPrice(price)
                .highPrice(price)
                .build();
    }

    public void updatePrices(double price) {
        this.highPrice = Math.max(this.highPrice, price);
        this.lowPrice = Math.min(this.lowPrice, price);
        this.closePrice = price;
    }
}
