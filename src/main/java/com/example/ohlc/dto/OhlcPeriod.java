package com.example.ohlc.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.temporal.ChronoUnit;

@Getter
@AllArgsConstructor
public enum OhlcPeriod {
    M1(ChronoUnit.MINUTES), // one minute, starts at 0 second of every minute
    H1(ChronoUnit.HOURS), // one hour, starts at 0:00 of every hour
    D1(ChronoUnit.DAYS); // one day, starts at 0:00:00 of every day

    private final ChronoUnit chronoUnit;
}
