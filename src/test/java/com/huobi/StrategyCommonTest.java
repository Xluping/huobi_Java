package com.huobi;

import com.huobi.client.req.account.PointRequest;
import com.huobi.client.req.trade.CreateOrderRequest;
import com.huobi.client.req.trade.OpenOrdersRequest;
import com.huobi.constant.Constants;
import com.huobi.constant.enums.OrderSideEnum;
import com.huobi.model.account.Point;
import com.huobi.model.trade.Order;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class StrategyCommonTest {
    private Long accountId;
    String symbol = "htusdt";
    BigDecimal amount = new BigDecimal("5");

    @Before
    public void setUp() throws Exception {
        accountId = HuobiUtil.getAccountIdByType("spot");

    }


    @Test
    public void calculatePriceList() {
        BigDecimal latestPrice = new BigDecimal("14.5678");
        ArrayList<Double> priceList = new ArrayList<Double>();
        BigDecimal base = new BigDecimal("1");
        double gridPercentDouble = com.huobi.constant.Constants.HIGH_RANGE / Constants.HIGH_COUNT;
        BigDecimal gridPercent = new BigDecimal(gridPercentDouble + "");
        for (int i = 1; i <= Constants.HIGH_COUNT; i++) {
            BigDecimal down = gridPercent.multiply(new BigDecimal("" + i)).multiply(new BigDecimal("0.01"));
            System.out.println(down);
            BigDecimal buyPosition = base.subtract(down);
            BigDecimal buyPrice = latestPrice.multiply(buyPosition).setScale(4, RoundingMode.DOWN);
            priceList.add(buyPrice.doubleValue());
        }
    }

    @Test
    public void placeBuyOrder() {
        BigDecimal buyPrice = HuobiUtil.getCurrentTradPrice(symbol);
        String clientOrderId = "LUPING" + System.nanoTime();

        CreateOrderRequest buyLimitRequest = CreateOrderRequest.spotBuyLimit(accountId, clientOrderId, symbol, new BigDecimal("9"), amount);
        CurrentAPI.getApiInstance().getTradeClient().createOrder(buyLimitRequest);
//        buyOrderMap.putIfAbsent(clientOrderId, amount);
        System.out.println("====== create buy-limit order at:" + buyPrice.toString() + " ======");
        System.out.println("====== 下 BUY 单, clientOrderId: " + clientOrderId + " ======");
    }


    @Test
    public void getOpenOrders() {
        List<Order> orderList = CurrentAPI.getApiInstance().getTradeClient().getOpenOrders(OpenOrdersRequest.builder()
                .accountId(accountId)
                .symbol(symbol)
                .side(OrderSideEnum.BUY)
                .build());
        orderList.forEach(order -> {
            System.out.println(" -- StrategyCommonTest.t2 -- " + order.toString());
        });
    }

    @Test
    public void cancelOrder() {
        Long res = CurrentAPI.getApiInstance().getTradeClient().cancelOrder(345486007973454L);
        System.out.println(" -- StrategyCommonTest.cancelOrder -- " + res);
    }

    @Test
    public void getPoint() {
        //
        Point point = CurrentAPI.getApiInstance().getAccountClient().getPoint(PointRequest.builder().build());
        System.out.println(" -- StrategyCommonTest.getPoint -- " + point.toString());
    }
}