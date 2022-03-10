package com.example.ohlc.dao;

import com.example.ohlc.dto.OhlcPeriod;
import com.example.ohlc.entity.Ohlc;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OhlcDaoImpl implements OhlcDao {

    @Override
    public void store(Ohlc ohlc) {

    }

    @Override
    public List<Ohlc> getHistorical(long instrumentId, OhlcPeriod period) {
        return null;
    }
}
