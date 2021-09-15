package com.huobi;

import com.huobi.client.GenericClient;
import com.huobi.client.req.account.AccountBalanceRequest;
import com.huobi.client.req.market.CandlestickRequest;
import com.huobi.client.req.trade.OrderHistoryRequest;
import com.huobi.client.req.trade.OrdersRequest;
import com.huobi.constant.HuobiOptions;
import com.huobi.constant.enums.CandlestickIntervalEnum;
import com.huobi.constant.enums.OrderStateEnum;
import com.huobi.constant.enums.OrderTypeEnum;
import com.huobi.constant.enums.QueryDirectionEnum;
import com.huobi.model.account.AccountBalance;
import com.huobi.model.account.Balance;
import com.huobi.model.generic.Symbol;
import com.huobi.model.market.Candlestick;
import com.huobi.model.trade.Order;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 9/13/21 10:45 AM
 */
@Slf4j
public class Test {
    static long accountId = 14086863L;

    public static void main(String[] args) {

    }

    public void filter() {
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


    }
}
