package com.example.ohlc.service.impl;

import com.example.ohlc.dao.OhlcDao;
import com.example.ohlc.dto.OhlcPeriod;
import com.example.ohlc.entity.Ohlc;
import com.example.ohlc.service.OhlcStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OhlcStorageImpl implements OhlcStorage {

    private final OhlcDao ohlcDao;

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void store(Ohlc ohlc) {
        ohlcDao.store(ohlc);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public List<Ohlc> getHistorical(long instrumentId, OhlcPeriod period) {
        return ohlcDao.getHistorical(instrumentId, period);
    }
}
