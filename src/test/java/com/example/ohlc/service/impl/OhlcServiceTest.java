package com.example.ohlc.service.impl;

import com.example.ohlc.dto.OhlcPeriod;
import com.example.ohlc.dto.Quote;
import com.example.ohlc.entity.Ohlc;
import com.example.ohlc.service.OhlcStorage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OhlcServiceTest {
    @Mock
    private OhlcStorage ohlcStorage;
    @Mock
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @InjectMocks
    OhlcServiceImpl ohlcService;
    @Captor
    ArgumentCaptor<Ohlc> ohlcCaptor;
    PodamFactory factory = new PodamFactoryImpl();
    LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));

    @BeforeEach
    public void init() {
        doAnswer((Answer<Void>) invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(threadPoolTaskExecutor).execute(any());
    }

    @Nested
    @DisplayName("GetCurrentOhlc")
    class GetCurrentOhlcTest {

        @BeforeEach
        public void init() {
            long nowMilli = now.toInstant(ZoneOffset.UTC).toEpochMilli();
            getQuoteList(nowMilli).forEach(q -> ohlcService.onQuote(q));
        }

        @Test
        public void shouldGetCurrentH1() {
            long nextPeriodH1Milli = now.toInstant(ZoneOffset.UTC).plus(1, ChronoUnit.HOURS).toEpochMilli();
            shouldGetCurrent(OhlcPeriod.H1, nextPeriodH1Milli,2);
        }

        @Test
        public void shouldGetCurrentD1() {
            long nextPeriodD1Milli = now.toInstant(ZoneOffset.UTC).plus(1, ChronoUnit.DAYS).toEpochMilli();
            shouldGetCurrent(OhlcPeriod.D1, nextPeriodD1Milli,3);
        }

        @Test
        public void shouldGetCurrentM1() {
            long nextPeriodM1Milli = now.toInstant(ZoneOffset.UTC).plus(1, ChronoUnit.MINUTES).toEpochMilli();
            shouldGetCurrent(OhlcPeriod.M1, nextPeriodM1Milli,1);
        }

        private void shouldGetCurrent(OhlcPeriod period, long nextPeriodMilli, int countOfInvokeStore) {
            var quoteNextPeriod = new QuoteTest(1, 11.11, nextPeriodMilli);
            var expectedOhlc = Ohlc.builder()
                    .instrumentId(1)
                    .openPrice(10.32)
                    .highPrice(13.75)
                    .lowPrice(8)
                    .closePrice(9.87)
                    .build();
            var expectedOhlcNextPeriod = Ohlc.first(1, period, 0, quoteNextPeriod.getPrice());
            Mockito.verify(ohlcStorage, Mockito.times(0)).store(any());
            var result = ohlcService.getCurrent(1, period);
            checkOhlc(expectedOhlc, result);
            ohlcService.onQuote(quoteNextPeriod);
            Mockito.verify(ohlcStorage, Mockito.times(countOfInvokeStore)).store(ohlcCaptor.capture());

            var storedOhlc = ohlcCaptor.getValue();
            result = ohlcService.getCurrent(1, period);
            checkOhlc(expectedOhlcNextPeriod, result);
            checkOhlc(expectedOhlc, storedOhlc);

            checkOhlc(2, period, 29.32, 29.32, 29, 29);
            checkOhlc(4, period, 49.54, 49.54, 49.54, 49.54);
            checkOhlc(5, period, 59.75, 59.88, 59.75, 59.87);
        }

        private void checkOhlc(long instrumentId, OhlcPeriod p, double openPrice, double highPrice, double lowPrice, double closePrice) {
            var expectedAnotherInstrumentOhlc = Ohlc.builder()
                    .instrumentId(instrumentId)
                    .openPrice(openPrice)
                    .highPrice(highPrice)
                    .lowPrice(lowPrice)
                    .closePrice(closePrice)
                    .build();
            var result = ohlcService.getCurrent(instrumentId, p);
            checkOhlc(expectedAnotherInstrumentOhlc, result);
        }

        private void checkOhlc(Ohlc expectedOhlc, Ohlc actualOhlc) {
            Assertions.assertNotNull(actualOhlc);
            Assertions.assertEquals(expectedOhlc.getOpenPrice(), actualOhlc.getOpenPrice());
            Assertions.assertEquals(expectedOhlc.getLowPrice(), actualOhlc.getLowPrice());
            Assertions.assertEquals(expectedOhlc.getHighPrice(), actualOhlc.getHighPrice());
            Assertions.assertEquals(expectedOhlc.getClosePrice(), actualOhlc.getClosePrice());
            Assertions.assertEquals(expectedOhlc.getInstrumentId(), actualOhlc.getInstrumentId());
        }
    }

    @Nested
    @DisplayName("GetHistoricalOhlc")
    class GetHistoricalOhlcTest {

        @Test
        public void shouldGetHistoricalMin() {
            var historicalMinOhlcList = getOhlcList(1, OhlcPeriod.M1, 3);
            checkHistoricalData(historicalMinOhlcList, OhlcPeriod.M1, 3);
        }

        @Test
        public void shouldGetHistoricalHour() {
            var historicalHourOhlcList = getOhlcList(1, OhlcPeriod.H1, 2);
            checkHistoricalData(historicalHourOhlcList, OhlcPeriod.H1, 2);
        }

        @Test
        public void shouldGetHistoricalDay() {
            var historicalDayOhlcList = getOhlcList(1, OhlcPeriod.D1, 4);
            checkHistoricalData(historicalDayOhlcList, OhlcPeriod.D1, 4);
        }

        private void checkHistoricalData(List<Ohlc> expectedData, OhlcPeriod p, int expectedCount) {
            when(ohlcStorage.getHistorical(anyLong(), eq(p))).thenReturn(expectedData);
            var result = ohlcService.getHistorical(1, p);
            Assertions.assertNotNull(result);
            Assertions.assertFalse(result.isEmpty());
            Assertions.assertEquals(expectedCount, result.size());
        }
    }


    @Nested
    @DisplayName("GetHistoricalAndCurrentOhlc")
    class GetHistoricalAndCurrentOhlcTest {

        @Test
        public void shouldGetOnlyHistorical() {
            int expectedHistoricalSize = 4;
            when(ohlcStorage.getHistorical(anyLong(), any())).thenReturn(getOhlcList(1, OhlcPeriod.D1, expectedHistoricalSize));
            var result = ohlcService.getHistoricalAndCurrent(1, OhlcPeriod.D1);
            Assertions.assertNotNull(result);
            Assertions.assertFalse(result.isEmpty());
            Assertions.assertEquals(expectedHistoricalSize, result.size());
        }

        @Test
        public void shouldGetOnlyCurrent() {
            int expectedCurrentSize = 1;
            when(ohlcStorage.getHistorical(anyLong(), any())).thenReturn(new ArrayList<>());
            var quote = new QuoteTest(1, 10.32, now.toInstant(ZoneOffset.UTC).toEpochMilli());
            ohlcService.onQuote(quote);
            var result = ohlcService.getHistoricalAndCurrent(1, OhlcPeriod.D1);
            Assertions.assertNotNull(result);
            Assertions.assertFalse(result.isEmpty());
            Assertions.assertEquals(expectedCurrentSize, result.size());
        }

        @Test
        public void shouldGetCurrentAndHistorical() {
            int expectedCurrentSize = 1;
            int expectedHistoricalSize = 4;
            when(ohlcStorage.getHistorical(anyLong(), any())).thenReturn(getOhlcList(1, OhlcPeriod.D1, expectedHistoricalSize));
            var quote = new QuoteTest(1, 10.32, now.toInstant(ZoneOffset.UTC).toEpochMilli());
            ohlcService.onQuote(quote);
            var result = ohlcService.getHistoricalAndCurrent(1, OhlcPeriod.D1);
            Assertions.assertNotNull(result);
            Assertions.assertFalse(result.isEmpty());
            Assertions.assertEquals(expectedHistoricalSize + expectedCurrentSize, result.size());
        }

        @Test
        public void shouldGetEmpty() {
            when(ohlcStorage.getHistorical(anyLong(), any())).thenReturn(Collections.emptyList());
            var result = ohlcService.getHistoricalAndCurrent(1, OhlcPeriod.D1);
            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("ClosePeriodOhlc")
    class ClosePeriodOhlcTest {

        @Test
        public void shouldClosePeriodM1() {
            shouldClosePeriod(OhlcPeriod.M1, 4, () -> {
                var result = ohlcService.getCurrent(1, OhlcPeriod.D1);
                Assertions.assertNotNull(result);
                result = ohlcService.getCurrent(1, OhlcPeriod.H1);
                Assertions.assertNotNull(result);
                return null;
            });
        }

        @Test
        public void shouldClosePeriodH1() {
            shouldClosePeriod(OhlcPeriod.H1, 8, () -> {
                var result = ohlcService.getCurrent(1, OhlcPeriod.M1);
                Assertions.assertNull(result);
                result = ohlcService.getCurrent(1, OhlcPeriod.D1);
                Assertions.assertNotNull(result);
                return null;
            });
        }

        @Test
        public void shouldClosePeriodD1() {
            shouldClosePeriod(OhlcPeriod.D1, 12, () -> {
                var result = ohlcService.getCurrent(1, OhlcPeriod.H1);
                Assertions.assertNull(result);
                result = ohlcService.getCurrent(1, OhlcPeriod.M1);
                Assertions.assertNull(result);
                return null;
            });
        }

        private void shouldClosePeriod(OhlcPeriod period, int countOfInvokeStore, Supplier<Void> additionalCheck) {
            long time = now.toInstant(ZoneOffset.UTC).minus(1, period.getChronoUnit()).toEpochMilli();
            getQuoteList(time).forEach(q -> ohlcService.onQuote(q));
            ohlcService.closeQuotePeriod();
            Mockito.verify(ohlcStorage, Mockito.times(countOfInvokeStore)).store(any());
            var result = ohlcService.getCurrent(1, period);
            Assertions.assertNull(result);
            additionalCheck.get();
        }
    }

    static class QuoteTest implements Quote {

        private final long instrumentId;
        private final double price;
        private final long utcTimestamp;

        public QuoteTest(long instrumentId, double price, long utcTimestamp) {
            this.price = price;
            this.instrumentId = instrumentId;
            this.utcTimestamp = utcTimestamp;
        }

        @Override
        public double getPrice() {
            return price;
        }

        @Override
        public long getInstrumentId() {
            return instrumentId;
        }

        @Override
        public long getUtcTimestamp() {
            return utcTimestamp;
        }
    }

    private List<Quote> getQuoteList(long time) {
        return Arrays.asList(
                new QuoteTest(1, 10.32, time),
                new QuoteTest(1, 8, time),
                new QuoteTest(1, 12.54, time),
                new QuoteTest(1, 13.75, time),
                new QuoteTest(1, 8.88, time),
                new QuoteTest(1, 9.87, time),
                new QuoteTest(2, 29.32, time),
                new QuoteTest(2, 29, time),
                new QuoteTest(4, 49.54, time),
                new QuoteTest(5, 59.75, time),
                new QuoteTest(5, 59.88, time),
                new QuoteTest(5, 59.87, time)
        );
    }

    private List<Ohlc> getOhlcList(long instrumentId, OhlcPeriod p, int size) {
        List<Ohlc> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            result.add(createTestOhlc(instrumentId, p));
        }
        return result;
    }

    public Ohlc createTestOhlc(long instrumentId, OhlcPeriod p) {
        var result = factory.manufacturePojo(Ohlc.class);
        result.setPeriod(p);
        result.setInstrumentId(instrumentId);
        return result;
    }
}
