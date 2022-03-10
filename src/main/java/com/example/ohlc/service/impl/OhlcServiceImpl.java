package com.example.ohlc.service.impl;

import com.example.ohlc.dto.OhlcPeriod;
import com.example.ohlc.dto.Quote;
import com.example.ohlc.entity.Ohlc;
import com.example.ohlc.entity.RawOhlcDataKey;
import com.example.ohlc.service.OhlcService;
import com.example.ohlc.service.OhlcStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class OhlcServiceImpl implements OhlcService {
    private final static ZoneOffset CURRENT_ZONE = ZoneOffset.UTC;

    private final Map<RawOhlcDataKey, Ohlc> rawOhlcData = new ConcurrentHashMap<>();
    private final OhlcStorage ohlcStorage;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Override
    public Ohlc getCurrent(long instrumentId, OhlcPeriod period) {
        return getCurrent(RawOhlcDataKey.of(instrumentId, period));
    }

    @Override
    public List<Ohlc> getHistorical(long instrumentId, OhlcPeriod period) {
        return ohlcStorage.getHistorical(instrumentId, period);
    }

    @Override
    public List<Ohlc> getHistoricalAndCurrent(long instrumentId, OhlcPeriod period) {
        var result = ohlcStorage.getHistorical(instrumentId, period);
        var current = getCurrent(instrumentId, period);
        if (current != null) {
            result.add(current);
        }
        return result;
    }

    @Override
    public void onQuote(Quote quote) {
        addTask(() -> saveQuoteRawData(quote));
    }

    @Scheduled(cron = "${close.period.cron}")
    protected void closeQuotePeriod() {
        addTask(this::closePeriod);
    }

    private void closePeriod() {
        var now = LocalDateTime.now(CURRENT_ZONE);
        log.debug("Start close period at {}", now);
        List<Ohlc> removed = new ArrayList<>();
        rawOhlcData.entrySet().removeIf(e -> {
            var period = e.getKey().getPeriod();
            long periodMillis = getQuotePeriodMillis(period, now);
            if (e.getValue().getStartPeriod() != periodMillis) {
                ohlcStorage.store(e.getValue());
                removed.add(e.getValue());
                return true;
            }
            return false;
        });
        log.debug("End close period at " + LocalDateTime.now(ZoneId.of("UTC")) + " count=" + removed.size());
    }

    private void saveQuoteRawData(Quote quote) {
        LocalDateTime quoteDataTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(quote.getUtcTimestamp()), CURRENT_ZONE);
        for (OhlcPeriod p : OhlcPeriod.values()) {
            var key = RawOhlcDataKey.of(quote.getInstrumentId(), p);
            var ohlc = getCurrent(key);
            long quotePeriodMillis = getQuotePeriodMillis(p, quoteDataTime);
            if (ohlc != null && ohlc.getStartPeriod() == quotePeriodMillis) {
                ohlc.updatePrices(quote.getPrice());
                continue;
            }
            if (ohlc != null) {
                ohlcStorage.store(ohlc);
            }
            ohlc = Ohlc.first(quote.getInstrumentId(), p, quotePeriodMillis, quote.getPrice());
            rawOhlcData.put(key, ohlc);
        }
    }

    private long getQuotePeriodMillis(OhlcPeriod period, LocalDateTime quoteDataTime) {
        LocalDateTime truncatedQuoteDateTime = quoteDataTime.truncatedTo(period.getChronoUnit());
        return truncatedQuoteDateTime.toInstant(CURRENT_ZONE).toEpochMilli();
    }

    private Ohlc getCurrent(RawOhlcDataKey key) {
        return rawOhlcData.get(key);
    }

    private void addTask(Runnable task) {
        threadPoolTaskExecutor.execute(task);
    }
}