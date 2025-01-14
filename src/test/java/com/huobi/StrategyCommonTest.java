package com.huobi;

import com.huobi.client.req.account.PointRequest;
import com.huobi.client.req.trade.CreateOrderRequest;
import com.huobi.client.req.trade.OpenOrdersRequest;
import com.huobi.constant.enums.OrderSideEnum;
import com.huobi.exception.SDKException;
import com.huobi.model.account.Point;
import com.huobi.model.trade.Order;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class StrategyCommonTest {
    static int API_CODE = 1000;
    private Long accountId;
    String symbol = "htusdt";
    BigDecimal amount = new BigDecimal("5");
    Logger logger = LoggerFactory.getLogger(StrategyCommonTest.class);

    @Before
    public void setUp() throws Exception {
        accountId = StrategyCommon.getAccountIdByType(API_CODE, "spot");

    }


    @Test
    public void calculatePriceList() {
        BigDecimal latestPrice = new BigDecimal("14.5678");
        ArrayList<Double> priceList = new ArrayList<Double>();
        BigDecimal base = new BigDecimal("1");
        double gridPercentDouble = 0.25;
        BigDecimal gridPercent = new BigDecimal(gridPercentDouble + "");
        for (int i = 1; i <= 40; i++) {
            BigDecimal down = gridPercent.multiply(new BigDecimal("" + i)).multiply(new BigDecimal("0.01"));
            System.out.println(down);
            BigDecimal buyPosition = base.subtract(down);
            BigDecimal buyPrice = latestPrice.multiply(buyPosition).setScale(4, RoundingMode.DOWN);
            priceList.add(buyPrice.doubleValue());
        }
    }

    @Test
    public void placeBuyOrder() {
        try {


            String symbol = "aaveusdt";
            BigDecimal buyPrice = StrategyCommon.getCurrentTradPrice(API_CODE, symbol);
            String clientOrderId = "LUPING" + System.nanoTime();

            CreateOrderRequest buyLimitRequest = CreateOrderRequest.spotBuyLimit(accountId, clientOrderId, symbol, buyPrice, new BigDecimal("0.001"));
            CurrentAPI.getApiInstance(API_CODE).getTradeClient().createOrder(buyLimitRequest);
//        buyOrderMap.putIfAbsent(clientOrderId, amount);
            System.out.println("====== create buy-limit order at:" + buyPrice.toString() + " ======");
            System.out.println("====== 下 BUY 单, clientOrderId: " + clientOrderId + " ======");
        } catch (SDKException e) {
            logger.error("====== StrategyCommonTest-placeBuyOrder: " + e.getErrCode() + "======");
            logger.error("====== StrategyCommonTest-placeBuyOrder: " + e.getMessage() + "======");

        }
    }


    @Test
    public void getOpenOrders() {
        List<Order> orderList = CurrentAPI.getApiInstance(API_CODE).getTradeClient().getOpenOrders(OpenOrdersRequest.builder()
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
        Long res = CurrentAPI.getApiInstance(API_CODE).getTradeClient().cancelOrder(345486007973454L);
        System.out.println(" -- StrategyCommonTest.cancelOrder -- " + res);
    }

    @Test
    public void getPoint() {
        //
        Point point = CurrentAPI.getApiInstance(API_CODE).getAccountClient().getPoint(PointRequest.builder().build());
        System.out.println(" -- StrategyCommonTest.getPoint -- " + point.toString());
    }

    @Test
    public void saveToFile() {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String sb = time + " : 0.5";

        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("./logs/profit.txt", true)), 1024);
            bw.write(sb);
            bw.newLine();
            bw.flush();
            bw.close();
        } catch (Exception e) {
            logger.error("====== StrategyCommon.saveToFile 写入文件出错: {} ======", e.getMessage());
        }

    }
}