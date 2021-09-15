package com.huobi;

import com.huobi.client.GenericClient;
import com.huobi.client.req.market.CandlestickRequest;
import com.huobi.constant.HuobiOptions;
import com.huobi.constant.enums.CandlestickIntervalEnum;
import com.huobi.model.generic.Symbol;
import com.huobi.model.market.Candlestick;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 9/13/21 10:45 AM
 */
@Slf4j
public class Test {
    public static void main(String[] args) {
        GenericClient genericService = GenericClient.create(HuobiOptions.builder().build());
        List<Symbol> result = new ArrayList<>();


        List<Symbol> symbolList = genericService.getSymbols();
        log.error("======Test.main :symbolList {} ======", symbolList.size());
        symbolList.forEach(symbol -> {
            // 不包含  btc*3 之类的
            if (null == symbol.getUnderlying()) {
                // online  usdt交易对
                if ("online".equalsIgnoreCase(symbol.getState()) && "usdt".equalsIgnoreCase(symbol.getQuoteCurrency())) {
                    // 主板, 观察区
                    if (SymbolPartionEnum.INNOVATION.getName().equalsIgnoreCase(symbol.getSymbolPartition())
                            || SymbolPartionEnum.POTENTIALS.getName().equalsIgnoreCase(symbol.getSymbolPartition())

                    ) {
                        log.info("======Test.main : {} ======", symbol.toString());
                        result.add(symbol);
                    }
                }
            }
        });
        log.error("======Test.main : result {} ======", result.size());


        List<Candlestick> klineList = CurrentAPI.getApiInstance().getMarketClient().getCandlestick(CandlestickRequest.builder()
                .symbol("btcusdt")
                .interval(CandlestickIntervalEnum.MIN30)
                .size(3)
                .build());
        klineList.forEach(candlestick -> {
            log.error("======Test.main : {} ======", candlestick.toString());
        });

    }
}
