package com.huobi;

import com.huobi.client.req.trade.CreateOrderRequest;
import com.huobi.constant.Constants;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 8/19/21 2:09 PM
 */
public class StrategyCommon {
    private static final ArrayList<BigDecimal> priceList = new ArrayList<>();
    private static final ConcurrentHashMap<String, BigDecimal> buyOrderMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, BigDecimal> sellOrderMap = new ConcurrentHashMap<>();
    private static volatile BigDecimal profit = new BigDecimal("0");
    private static volatile BigDecimal fee = new BigDecimal("0");


    static Logger logger = LoggerFactory.getLogger(StrategyCommon.class);

    public static synchronized ArrayList<BigDecimal> calculateBuyPriceList(int strategy, BigDecimal latestPrice, int scale) {
        priceList.clear();
        if (strategy == 1) {
            // 高频
            calculateBuyPrice(latestPrice, scale, Constants.HIGH_RANGE_1, Constants.HIGH_COUNT_1, new BigDecimal("0"));
            //稳健
            calculateBuyPrice(latestPrice, scale, Constants.MEDIUM_RANGE_1 - Constants.HIGH_RANGE_1, Constants.MEDIUM_COUNT_1, new BigDecimal(String.valueOf(Constants.HIGH_RANGE_1)));
            //保守
            calculateBuyPrice(latestPrice, scale, Constants.LOW_RANGE_1 - Constants.MEDIUM_RANGE_1, Constants.LOW_COUNT_1, new BigDecimal(String.valueOf(Constants.MEDIUM_RANGE_1)));
        } else if (strategy == 2) {
            // 高频
            calculateBuyPrice(latestPrice, scale, Constants.HIGH_RANGE_2, Constants.HIGH_COUNT_2, new BigDecimal("0"));
            //稳健
            calculateBuyPrice(latestPrice, scale, Constants.MEDIUM_RANGE_2 - Constants.HIGH_RANGE_2, Constants.MEDIUM_COUNT_2, new BigDecimal(String.valueOf(Constants.HIGH_RANGE_2)));
            //保守
            calculateBuyPrice(latestPrice, scale, Constants.LOW_RANGE_2 - Constants.MEDIUM_RANGE_2, Constants.LOW_COUNT_2, new BigDecimal(String.valueOf(Constants.MEDIUM_RANGE_2)));
        } else if (strategy == 3) {
            // 高频
            calculateBuyPrice(latestPrice, scale, Constants.HIGH_RANGE_3, Constants.HIGH_COUNT_3, new BigDecimal("0"));
            //稳健
            calculateBuyPrice(latestPrice, scale, Constants.MEDIUM_RANGE_3 - Constants.HIGH_RANGE_3, Constants.MEDIUM_COUNT_3, new BigDecimal(String.valueOf(Constants.HIGH_RANGE_3)));
            //保守
            calculateBuyPrice(latestPrice, scale, Constants.LOW_RANGE_3 - Constants.MEDIUM_RANGE_3, Constants.LOW_COUNT_3, new BigDecimal(String.valueOf(Constants.MEDIUM_RANGE_3)));
        }

        return priceList;
    }

    public static ArrayList<BigDecimal> getPriceList() {
        return priceList;
    }

    public static ConcurrentHashMap<String, BigDecimal> getBuyOrderMap() {
        return buyOrderMap;
    }

    public static ConcurrentHashMap<Long, BigDecimal> getSellOrderMap() {
        return sellOrderMap;
    }

    /**
     * 根据参数 计算补仓点位
     *
     * @param latestPrice
     * @param scale
     * @param range
     * @param count
     */
    public static synchronized void calculateBuyPrice(BigDecimal latestPrice, int scale, double range, double count, BigDecimal previousPercent) {
        BigDecimal pre = previousPercent.multiply(new BigDecimal("0.01"));
        BigDecimal base = new BigDecimal("1");
        double gridPercentDoubleMedium = range / count;
        BigDecimal gridPercentMedium = new BigDecimal(String.valueOf(gridPercentDoubleMedium));
        for (int i = 1; i <= count; i++) {
            BigDecimal goDown = gridPercentMedium.multiply(new BigDecimal(String.valueOf(i))).multiply(new BigDecimal("0.01"));
            BigDecimal downTo = goDown.add(pre);
            System.out.println(downTo);
            BigDecimal buyPosition = base.subtract(downTo);
            logger.error("====== StrategyCommon-buyPosition(下跌到)= {} * 100% ======", buyPosition.toString());
            BigDecimal buyPrice = latestPrice.multiply(buyPosition).setScale(scale, RoundingMode.DOWN);
            logger.error("====== StrategyCommon-buyPrice= {} ======", buyPrice.toString());
            priceList.add(buyPrice);
        }
        logger.error("==============================================================");
    }

    /**
     * 下单 buy-market
     * 保存 clientOrderId, amount 以便轮巡时 查看下单状态
     * <p>
     * 根据 usdt 计算买入的币的数量
     *
     * @param spot
     * @param buyPrice
     * @param usdt     usdt 数量
     */
    public static synchronized void placeBuyOrder(Spot spot, BigDecimal buyPrice, BigDecimal usdt) {

        BigDecimal orderValue = new BigDecimal("0");
        //最小下单金额
        if (usdt.compareTo(spot.getMinOrderValue()) < 0) {
            logger.error("====== {}-StrategyCommon-placeSellOrder: 按最小下单金额下单 BUY {} ======", spot.getSymbol(), spot.getMinOrderValue());

            orderValue = orderValue.add(spot.getMinOrderValue());
        } else {
            orderValue = orderValue.add(usdt);
        }

        BigDecimal coinAmount = orderValue.divide(buyPrice, RoundingMode.HALF_UP);
        //自定义订单号
        String clientOrderId = spot.getSymbol() + System.nanoTime();
        // 价格,币数 有严格的小数位限制
        buyPrice = buyPrice.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
        coinAmount = coinAmount.setScale(spot.getAmountPrecision(), RoundingMode.HALF_UP);

        BigDecimal orderAmount = new BigDecimal("0");
        //最小下单量限制
        if (coinAmount.compareTo(spot.getLimitOrderMinOrderAmt()) < 0) {
            orderAmount = orderAmount.add(spot.getLimitOrderMinOrderAmt());
            logger.error("====== {}-StrategyCommon-placeSellOrder: 按最小下单币数下单 BUY {} ======", spot.getSymbol(), spot.getLimitOrderMinOrderAmt());

        } else {
            orderAmount = orderAmount.add(coinAmount);
        }

        CreateOrderRequest buyLimitRequest = CreateOrderRequest.spotBuyLimit(spot.getAccountId(), clientOrderId, spot.getSymbol(), buyPrice, orderAmount);
        CurrentAPI.getApiInstance().getTradeClient().createOrder(buyLimitRequest);
        buyOrderMap.putIfAbsent(clientOrderId, orderAmount);
        logger.error("====== {}-StrategyCommon-BUY at: {}, clientOrderId : {}  ======", spot.getSymbol(), buyPrice.toString(), clientOrderId);


    }

    /**
     * 计算卖单价格, 并挂单.
     * sell-limit
     *
     * @param spot
     * @param buyPrice
     * @param coinAmount
     */
    public static synchronized void placeSellOrder(int currentStrategy, Spot spot, BigDecimal buyPrice, BigDecimal coinAmount) {
        // 计算卖出价格 buyPrice * (1+offset);
        BigDecimal sellPrice = null;
        if (currentStrategy == 1) {
            sellPrice = buyPrice.multiply(Constants.SELL_OFFSET_1);
        } else if (currentStrategy == 2) {
            sellPrice = buyPrice.multiply(Constants.SELL_OFFSET_2);
        } else if (currentStrategy == 3) {
            sellPrice = buyPrice.multiply(Constants.SELL_OFFSET_3);
        }
        // 价格,币数 有严格的小数位限制
        sellPrice = sellPrice.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
        coinAmount = coinAmount.setScale(spot.getAmountPrecision(), RoundingMode.HALF_DOWN);
        //最小下单量限制
        BigDecimal orderAmount = new BigDecimal("0");
        //最小下单量限制
        if (coinAmount.compareTo(spot.getLimitOrderMinOrderAmt()) < 0) {
            logger.error("====== {}-StrategyCommon-placeSellOrder: 按最小下单币数下单 SELL {} ======", spot.getSymbol(), spot.getLimitOrderMinOrderAmt());

            orderAmount = orderAmount.add(spot.getLimitOrderMinOrderAmt());
        } else {
            orderAmount = orderAmount.add(coinAmount);
        }

        CreateOrderRequest sellLimitRequest = CreateOrderRequest.spotSellLimit(spot.getAccountId(), spot.getSymbol(), sellPrice, orderAmount);
        Long orderId = CurrentAPI.getApiInstance().getTradeClient().createOrder(sellLimitRequest);
        sellOrderMap.putIfAbsent(orderId, orderAmount);
        logger.error("====== {}-StrategyCommon-SELL at: {}, orderId : {}  ======", spot.getSymbol(), sellPrice.toString(), orderId);


    }


    public static void setProfit(BigDecimal win) {

        profit = profit.add(win);
    }

    public static void setFee(BigDecimal lose) {
        fee = fee.add(lose);
    }

    public static BigDecimal getProfit() {
        return profit;
    }

    public static BigDecimal getFee() {
        return fee;
    }

    public static void reset() {
        profit = new BigDecimal("0");
        fee = new BigDecimal("0");
    }


}
