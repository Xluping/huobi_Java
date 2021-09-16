package com.huobi;

import com.huobi.client.GenericClient;
import com.huobi.client.req.account.AccountBalanceRequest;
import com.huobi.client.req.market.CandlestickRequest;
import com.huobi.client.req.trade.CreateOrderRequest;
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
import java.math.RoundingMode;
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
    static int API_CODE = 1000;

    public static void main(String[] args) {
        Spot spot = new Spot();
        spot.setBaseCurrency("hc");
        spot.setQuoteCurrency("usdt");
        spot.setSymbol(spot.getBaseCurrency() + spot.getQuoteCurrency());
        spot.setAccountId(accountId);

        List<Symbol> symbolList = CurrentAPI.getApiInstance(API_CODE).getGenericClient().getSymbols();
        symbolList.forEach(symbol -> {
            if (symbol.getBaseCurrency().equalsIgnoreCase(spot.getBaseCurrency()) && symbol.getQuoteCurrency().equalsIgnoreCase(spot.getQuoteCurrency())) {
                spot.setPricePrecision(symbol.getPricePrecision());
                spot.setAmountPrecision(symbol.getAmountPrecision());
                spot.setMinOrderValue(symbol.getMinOrderValue());
                spot.setLimitOrderMinOrderAmt(symbol.getLimitOrderMinOrderAmt());
                spot.setSellMarketMinOrderAmt(symbol.getSellMarketMinOrderAmt());
            }
        });

        sell(1, spot, new BigDecimal("0.7946"), new BigDecimal("65.23577"), 1);
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

    public static void sell(int currentStrategy, Spot spot, BigDecimal buyPrice, BigDecimal coinAmount, int type) {
        // 计算卖出价格 buyPrice * (1+offset);
        try {
            BigDecimal sellPrice = null;
            if (currentStrategy == 1) {
                sellPrice = buyPrice.multiply(Constants.SELL_OFFSET_1);
            } else if (currentStrategy == 2) {
                sellPrice = buyPrice.multiply(Constants.SELL_OFFSET_2);
            } else if (currentStrategy == 3) {
                sellPrice = buyPrice.multiply(Constants.SELL_OFFSET_3);
            }
            //自定义订单号
            String clientOrderId = spot.getSymbol() + System.nanoTime();
            // 价格,币数 有严格的小数位限制
            assert sellPrice != null;
            sellPrice = sellPrice.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
            coinAmount = coinAmount.setScale(spot.getAmountPrecision(), RoundingMode.DOWN);
            //最小下单量限制
            BigDecimal orderAmount;
            //最小下单量限制
            if (coinAmount.compareTo(spot.getLimitOrderMinOrderAmt()) < 0) {
                log.info("====== {}-{}-StrategyCommon: 按最小下单币数下单 SELL {} ======", spot.getSymbol(), currentStrategy, spot.getLimitOrderMinOrderAmt());
                orderAmount = spot.getLimitOrderMinOrderAmt();
            } else {
                orderAmount = coinAmount;
            }

            spot.setOrderAmount(orderAmount);

            log.info("====== {}-{}-StrategyCommon: SELL at: {},  clientOrderId: {}, orderAmount: {}, type: {} ======", spot.getSymbol(), currentStrategy, sellPrice.toString(), clientOrderId, orderAmount, type);
            CreateOrderRequest sellRequest;
            if (type == 1) {
                sellRequest = CreateOrderRequest.spotSellLimit(spot.getAccountId(), clientOrderId, spot.getSymbol(), sellPrice, orderAmount);
            } else {
                sellRequest = CreateOrderRequest.spotSellMarket(spot.getAccountId(), clientOrderId, spot.getSymbol(), orderAmount);
            }
            CurrentAPI.getApiInstance(API_CODE).getTradeClient().createOrder(sellRequest);
        } catch (Exception e) {
            log.error("======StrategyCommon.sell : 卖出时发生异常 {}, 重新尝试下单 99% ======", e.getMessage());
            sell(currentStrategy, spot, buyPrice, coinAmount.multiply(new BigDecimal("0.99")), type);
        }

    }

}
