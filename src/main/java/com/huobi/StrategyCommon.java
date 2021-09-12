package com.huobi;

import com.huobi.client.req.trade.CreateOrderRequest;
import lombok.Synchronized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 8/19/21 2:09 PM
 */
public class StrategyCommon {
    private static final ArrayList<BigDecimal> priceList = new ArrayList<>();
    private static final ConcurrentHashMap<String, BigDecimal> buyOrderMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, BigDecimal> sellOrderMap = new ConcurrentHashMap<>();
    private static volatile BigDecimal profit = BigDecimal.ZERO;
    private static volatile BigDecimal fee = BigDecimal.ZERO;


    static Logger logger = LoggerFactory.getLogger(StrategyCommon.class);

    @Synchronized
    public static ArrayList<BigDecimal> calculateBuyPriceList(int strategy, BigDecimal latestPrice, int scale) {
        priceList.clear();
        if (strategy == 1) {
            // 高频
            calculateBuyPrice(1, latestPrice, scale, Constants.HIGH_RANGE_1, Constants.HIGH_COUNT_1, new BigDecimal("0"));
            //稳健
            calculateBuyPrice(1, latestPrice, scale, Constants.MEDIUM_RANGE_1 - Constants.HIGH_RANGE_1, Constants.MEDIUM_COUNT_1, new BigDecimal(String.valueOf(Constants.HIGH_RANGE_1)));
            //保守
            calculateBuyPrice(1, latestPrice, scale, Constants.LOW_RANGE_1 - Constants.MEDIUM_RANGE_1, Constants.LOW_COUNT_1, new BigDecimal(String.valueOf(Constants.MEDIUM_RANGE_1)));
        } else if (strategy == 2) {
            // 高频
            calculateBuyPrice(2, latestPrice, scale, Constants.HIGH_RANGE_2, Constants.HIGH_COUNT_2, new BigDecimal("0"));
            //稳健
            calculateBuyPrice(2, latestPrice, scale, Constants.MEDIUM_RANGE_2 - Constants.HIGH_RANGE_2, Constants.MEDIUM_COUNT_2, new BigDecimal(String.valueOf(Constants.HIGH_RANGE_2)));
            //保守
            calculateBuyPrice(2, latestPrice, scale, Constants.LOW_RANGE_2 - Constants.MEDIUM_RANGE_2, Constants.LOW_COUNT_2, new BigDecimal(String.valueOf(Constants.MEDIUM_RANGE_2)));
        } else if (strategy == 3) {
            // 高频
            calculateBuyPrice(3, latestPrice, scale, Constants.HIGH_RANGE_3, Constants.HIGH_COUNT_3, new BigDecimal("0"));
            //稳健
            calculateBuyPrice(3, latestPrice, scale, Constants.MEDIUM_RANGE_3 - Constants.HIGH_RANGE_3, Constants.MEDIUM_COUNT_3, new BigDecimal(String.valueOf(Constants.HIGH_RANGE_3)));
            //保守
            calculateBuyPrice(3, latestPrice, scale, Constants.LOW_RANGE_3 - Constants.MEDIUM_RANGE_3, Constants.LOW_COUNT_3, new BigDecimal(String.valueOf(Constants.MEDIUM_RANGE_3)));
        }

        return priceList;
    }

    public static ArrayList<BigDecimal> getPriceList() {
        return priceList;
    }

    public static ConcurrentHashMap<String, BigDecimal> getBuyOrderMap() {
        return buyOrderMap;
    }

    public static ConcurrentHashMap<String, BigDecimal> getSellOrderMap() {
        return sellOrderMap;
    }

    /**
     * 根据参数 计算补仓点位
     */
    @Synchronized
    public static void calculateBuyPrice(int strategy, BigDecimal latestPrice, int scale, double range, double count, BigDecimal previousPercent) {
        BigDecimal pre = previousPercent.multiply(new BigDecimal("0.01"));
        BigDecimal base = new BigDecimal("1");
        double gridPercentDoubleMedium = range / count;
        BigDecimal gridPercentMedium = new BigDecimal(String.valueOf(gridPercentDoubleMedium));
        for (int i = 1; i <= count; i++) {
            BigDecimal goDown = gridPercentMedium.multiply(new BigDecimal(String.valueOf(i))).multiply(new BigDecimal("0.01"));
            BigDecimal downTo = goDown.add(pre);
            BigDecimal buyPosition = base.subtract(downTo);
            BigDecimal buyPrice = latestPrice.multiply(buyPosition).setScale(scale, RoundingMode.DOWN);
            logger.error("====== {}-StrategyCommon-buyPosition: 下跌 {}% 到 {}% = {} ======", strategy, downTo.multiply(new BigDecimal("100")).toString(), buyPosition.multiply(new BigDecimal("100")).toString(), buyPrice.toString());
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
     * @param type 1 buy-limit  2 buy-market
     */
    @Synchronized
    public static void buy(int strategy, Spot spot, BigDecimal buyPrice, BigDecimal usdt, int type) {

        BigDecimal orderValue;
        //最小下单金额
        if (usdt.compareTo(spot.getMinOrderValue()) < 0) {
            logger.error("====== {}-{}-StrategyCommon: 按最小下单金额下单 BUY {} ======", spot.getSymbol(), strategy, spot.getMinOrderValue());
            orderValue = spot.getMinOrderValue();
        } else {
            orderValue = usdt;
        }

        BigDecimal coinAmount = orderValue.divide(buyPrice, RoundingMode.HALF_UP);
        //自定义订单号
        String clientOrderId = spot.getSymbol() + System.nanoTime();
        // 价格,币数 有严格的小数位限制
        buyPrice = buyPrice.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
        coinAmount = coinAmount.setScale(spot.getAmountPrecision(), RoundingMode.HALF_UP);

        BigDecimal orderAmount;
        //最小下单量限制
        if (coinAmount.compareTo(spot.getLimitOrderMinOrderAmt()) < 0) {
            orderAmount = spot.getLimitOrderMinOrderAmt();
            logger.error("====== {}-{}-StrategyCommon: 按最小下单币数下单 BUY {} ======", spot.getSymbol(), strategy, spot.getLimitOrderMinOrderAmt());

        } else {
            orderAmount = coinAmount;
        }
        CreateOrderRequest buyRequest;

        if (type == 1) {
            // buy
            logger.error("====== {}-{}-StrategyCommon:  限价-BUY at: {},  clientOrderId: {}, orderAmount: {} 币 ======", spot.getSymbol(), strategy, buyPrice.toString(), clientOrderId, orderAmount);
            buyRequest = CreateOrderRequest.spotBuyLimit(spot.getAccountId(), clientOrderId, spot.getSymbol(), buyPrice, orderAmount);
        } else {
            logger.error("====== {}-{}-StrategyCommon:  市价-BUY at: {},  clientOrderId: {}, orderAmount: {} USDT ======", spot.getSymbol(), strategy, buyPrice.toString(), clientOrderId, orderValue);
            buyRequest = CreateOrderRequest.spotBuyMarket(spot.getAccountId(), clientOrderId, spot.getSymbol(), orderValue);
        }
        CurrentAPI.getApiInstance().getTradeClient().createOrder(buyRequest);
        buyOrderMap.putIfAbsent(clientOrderId, orderAmount);

    }


    /**
     * 计算卖单价格, 并挂单.
     * 1:sell-limit
     * 2:sell-market
     */
    @Synchronized
    public static void sell(int currentStrategy, Spot spot, BigDecimal buyPrice, BigDecimal coinAmount, int type) {
        // 计算卖出价格 buyPrice * (1+offset);
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
        sellPrice = sellPrice.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
        coinAmount = coinAmount.setScale(spot.getAmountPrecision(), RoundingMode.HALF_DOWN);
        //最小下单量限制
        BigDecimal orderAmount;
        //最小下单量限制
        if (coinAmount.compareTo(spot.getLimitOrderMinOrderAmt()) < 0) {
            logger.error("====== {}-{}-StrategyCommon: 按最小下单币数下单 SELL {} ======", spot.getSymbol(), currentStrategy, spot.getLimitOrderMinOrderAmt());
            orderAmount = spot.getLimitOrderMinOrderAmt();
        } else {
            orderAmount = coinAmount;
        }

        logger.error("====== {}-{}-StrategyCommon: SELL at: {},  clientOrderId: {}, orderAmount: {}, type: {} ======", spot.getSymbol(), currentStrategy, sellPrice.toString(), clientOrderId, orderAmount, type);
        CreateOrderRequest sellRequest;
        if (type == 1) {
            sellRequest = CreateOrderRequest.spotSellLimit(spot.getAccountId(), clientOrderId, spot.getSymbol(), sellPrice, orderAmount);
        } else {
            sellRequest = CreateOrderRequest.spotSellMarket(spot.getAccountId(), clientOrderId, spot.getSymbol(), orderAmount);
        }
        CurrentAPI.getApiInstance().getTradeClient().createOrder(sellRequest);
        sellOrderMap.putIfAbsent(clientOrderId, orderAmount);


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

    public static void resetFeeAndProfit(String symbol, int currentStrategy) {
        profit = BigDecimal.ZERO;
        fee = BigDecimal.ZERO;
        logger.info("====== {}-{}-StrategyCommon-resetFeeAndProfit : profit= {} , fee= {} ======", symbol, currentStrategy, profit.toString(), fee.toString());
    }


}
