package com.huobi;

import com.huobi.constant.enums.OrderSideEnum;
import com.huobi.exception.SDKException;
import com.huobi.model.generic.Symbol;
import com.huobi.model.trade.Order;
import lombok.Synchronized;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 8/24/21 9:12 PM
 * <p>
 * V1
 */
public class SpotTemplateRestful implements Job {
    private static String BASE_CURRENCY = "";
    private static final String QUOTE_CURRENCY = "usdt";
    private static String SYMBOL; //htusdt
    private static String PORTION;
    private static final int CURRENT_STRATEGY = 1;


    private static Long spotAccountId = 14086863L;
    private static Long pointAccountId = 14424186L;
    private final static Spot spot = new Spot();
    private static final AtomicInteger ticker = new AtomicInteger();
    private static double highCount = 0;
    private static double mediumCount = 0;
    private String level = "high";
    private static BigDecimal totalBalance;

    private static final AtomicInteger orderCount = new AtomicInteger(-1);
    private static volatile BigDecimal usdtBalance = BigDecimal.ZERO;
    private static volatile BigDecimal latestPrice;
    private static volatile boolean insufficientFound = true;
    private static volatile boolean balanceChanged = false;
    private static final Logger logger = LoggerFactory.getLogger(SpotTemplateRestful.class);

    public static void main(String[] args) {
        BASE_CURRENCY = args[0];
        PORTION = args[1];
//        BASE_CURRENCY = "cspr";
//        PORTION = "1000";
        if (BASE_CURRENCY == null || BASE_CURRENCY.isEmpty()) {
            BASE_CURRENCY = "ht";
            logger.error("====== main: BASE_CURRENCY == null || BASE_CURRENCY.isEmpty() set BASE_CURRENCY = {} ======", BASE_CURRENCY);
        }
        SYMBOL = BASE_CURRENCY + QUOTE_CURRENCY;

        if (PORTION == null || PORTION.isEmpty()) {
            PORTION = "3000";
            logger.error("====== {}-main: PORTION == null || PORTION.isEmpty() set PORTION = {} ======", SYMBOL, PORTION);
        }
        logger.info("====== main:  SYMBOL = {} ======", SYMBOL);
        logger.info("====== main:  PORTION = {} ======", PORTION);
        logger.info("====== main:  STRATEGY = {} ======", CURRENT_STRATEGY);

        SpotTemplateRestful spotBuyer = new SpotTemplateRestful();
        spotBuyer.init();
        JobManagement jobManagement = new JobManagement();
        jobManagement.addJob("0/4 * * * * ?", SpotTemplateRestful.class, SYMBOL);
        jobManagement.startJob();
    }

    /**
     * 设置基本参数
     */
    public void init() {
        try {
            totalBalance = new BigDecimal(PORTION);
            prepareSpot(totalBalance, CURRENT_STRATEGY);
//            StrategyCommon.cancelOpenOrders(CURRENT_STRATEGY, spotAccountId, SYMBOL, OrderSideEnum.BUY);
            StrategyCommon.getBuyOrderMap().clear();
            launch();
        } catch (Exception exception) {
            logger.error("====== {}-{}-init-startup: {} ======", SYMBOL, CURRENT_STRATEGY, exception.getMessage());
        }

    }

    @Synchronized
    public void prepareSpot(BigDecimal totalBalance, int currentStrategy) {
        latestPrice = StrategyCommon.getCurrentTradPrice(CURRENT_STRATEGY, SYMBOL);
        spot.setStartPrice(latestPrice);
        if (spot.getDoublePrice() == null) {
            spot.setDoublePrice(latestPrice.multiply(new BigDecimal("2")));
        }
        if (spot.getTriplePrice() == null) {
            spot.setTriplePrice(latestPrice.multiply(new BigDecimal("3")));
        }
        spot.setBaseCurrency(BASE_CURRENCY);
        spot.setQuoteCurrency(QUOTE_CURRENCY);
        spot.setSymbol(SYMBOL);

        List<Symbol> symbolList = CurrentAPI.getApiInstance(CURRENT_STRATEGY).getGenericClient().getSymbols();
        symbolList.forEach(symbol -> {
            if (symbol.getBaseCurrency().equalsIgnoreCase(spot.getBaseCurrency()) && symbol.getQuoteCurrency().equalsIgnoreCase(spot.getQuoteCurrency())) {
                spot.setPricePrecision(symbol.getPricePrecision());
                spot.setAmountPrecision(symbol.getAmountPrecision());
                spot.setMinOrderValue(symbol.getMinOrderValue());
                spot.setLimitOrderMinOrderAmt(symbol.getLimitOrderMinOrderAmt());
                spot.setSellMarketMinOrderAmt(symbol.getSellMarketMinOrderAmt());
            }
        });

        spotAccountId = StrategyCommon.getAccountIdByType(CURRENT_STRATEGY, "spot");
        pointAccountId = StrategyCommon.getAccountIdByType(CURRENT_STRATEGY, "point");
        spot.setAccountId(spotAccountId);

        usdtBalance = StrategyCommon.getQuotaBalanceByAccountId(CURRENT_STRATEGY, spotAccountId, spot.getQuoteCurrency());
        logger.info("{}-{}-prepareSpot: 分配到的仓位: {} ======", SYMBOL, currentStrategy, PORTION);
        spot.setTotalBalance(totalBalance);
        BigDecimal highBalance;
        BigDecimal mediumBalance;
        BigDecimal lowBalance;
        BigDecimal portionHigh;
        BigDecimal portionMedium;
        BigDecimal portionLow;
        //2. 稳健 3.保守
        double lowCount;
        switch (currentStrategy) {
            case 2:
                highBalance = totalBalance.multiply(new BigDecimal(Constants.HIGH_RATIO_2.toString()));
                highBalance = highBalance.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
                mediumBalance = totalBalance.multiply(new BigDecimal(Constants.MEDIUM_RATIO_2.toString()));
                mediumBalance = mediumBalance.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
                lowBalance = totalBalance.multiply(new BigDecimal(Constants.LOW_RATIO_2.toString()));
                lowBalance = lowBalance.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);

                portionHigh = highBalance.divide(new BigDecimal(String.valueOf(Constants.HIGH_COUNT_2)), spot.getPricePrecision(), RoundingMode.HALF_UP);

                portionMedium = mediumBalance.divide(new BigDecimal(String.valueOf(Constants.MEDIUM_COUNT_2)), spot.getPricePrecision(), RoundingMode.HALF_UP);

                portionLow = lowBalance.divide(new BigDecimal(String.valueOf(Constants.LOW_COUNT_2)), spot.getPricePrecision(), RoundingMode.HALF_UP);
                highCount = Constants.HIGH_COUNT_2;
                mediumCount = Constants.MEDIUM_COUNT_2;
                lowCount = Constants.LOW_COUNT_2;

                break;
            case 3:
                highBalance = totalBalance.multiply(new BigDecimal(Constants.HIGH_RATIO_3.toString()));
                highBalance = highBalance.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
                mediumBalance = totalBalance.multiply(new BigDecimal(Constants.MEDIUM_RATIO_3.toString()));
                mediumBalance = mediumBalance.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
                lowBalance = totalBalance.multiply(new BigDecimal(Constants.LOW_RATIO_3.toString()));
                lowBalance = lowBalance.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);

                portionHigh = highBalance.divide(new BigDecimal(String.valueOf(Constants.HIGH_COUNT_3)), spot.getPricePrecision(), RoundingMode.HALF_UP);

                portionMedium = mediumBalance.divide(new BigDecimal(String.valueOf(Constants.MEDIUM_COUNT_3)), spot.getPricePrecision(), RoundingMode.HALF_UP);


                portionLow = lowBalance.divide(new BigDecimal(String.valueOf(Constants.LOW_COUNT_3)), spot.getPricePrecision(), RoundingMode.HALF_UP);
                highCount = Constants.HIGH_COUNT_3;
                mediumCount = Constants.MEDIUM_COUNT_3;
                lowCount = Constants.LOW_COUNT_3;

                break;
            default:  //高频
                highBalance = totalBalance.multiply(new BigDecimal(Constants.HIGH_RATIO_1.toString()));
                highBalance = highBalance.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
                mediumBalance = totalBalance.multiply(new BigDecimal(Constants.MEDIUM_RATIO_1.toString()));
                mediumBalance = mediumBalance.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
                lowBalance = totalBalance.multiply(new BigDecimal(Constants.LOW_RATIO_1.toString()));
                lowBalance = lowBalance.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);

                portionHigh = highBalance.divide(new BigDecimal(String.valueOf(Constants.HIGH_COUNT_1)), spot.getPricePrecision(), RoundingMode.HALF_UP);

                portionMedium = mediumBalance.divide(new BigDecimal(String.valueOf(Constants.MEDIUM_COUNT_1)), spot.getPricePrecision(), RoundingMode.HALF_UP);

                portionLow = lowBalance.divide(new BigDecimal(String.valueOf(Constants.LOW_COUNT_1)), spot.getPricePrecision(), RoundingMode.HALF_UP);
                highCount = Constants.HIGH_COUNT_1;
                mediumCount = Constants.MEDIUM_COUNT_1;
                lowCount = Constants.LOW_COUNT_1;

                break;
        }
        spot.setHighStrategyBalance(highBalance);
        spot.setMediumStrategyBalance(mediumBalance);
        spot.setLowStrategyBalance(lowBalance);
        spot.setPortionHigh(portionHigh);
        spot.setPortionMedium(portionMedium);
        spot.setPortionLow(portionLow);
        logger.info("{}-prepareSpot-当前策略: {} ======", SYMBOL, currentStrategy);
        logger.info("{}-prepareSpot-分配到-H-的仓位: {}-{}", SYMBOL, highBalance, spot.getQuoteCurrency());
        logger.info("{}-prepareSpot-分配到-M-的仓位: {}-{}", SYMBOL, mediumBalance, spot.getQuoteCurrency());
        logger.info("{}-prepareSpot-分配到-L-的仓位: {}-{}", SYMBOL, lowBalance, spot.getQuoteCurrency());
        logger.info("{}-prepareSpot-H 每次补仓份额: {}-{}", SYMBOL, portionHigh, spot.getQuoteCurrency());
        logger.info("{}-prepareSpot-H 补仓次数: {}", SYMBOL, highCount);
        logger.info("{}-prepareSpot-M 每次补仓份额: {}-{}", SYMBOL, portionMedium, spot.getQuoteCurrency());
        logger.info("{}-prepareSpot-M 补仓次数: {}", SYMBOL, mediumCount);
        logger.info("{}-prepareSpot-L 每次补仓份额: {}-{}", SYMBOL, portionLow, spot.getQuoteCurrency());
        logger.info("{}-prepareSpot-L 补仓次数: {}", SYMBOL, lowCount);

        logger.info("====== SpotTemplate-prepareSpot: {}-{}", spot.toString(), spot.getQuoteCurrency());

    }

    public void launch() {
        StrategyCommon.resetFeeAndProfit(CURRENT_STRATEGY);
        logger.error("====== {}-{}-SpotTemplate-launch:策略启动: {} ======", SYMBOL, CURRENT_STRATEGY, spot);
        latestPrice = StrategyCommon.getCurrentTradPrice(CURRENT_STRATEGY, spot.getSymbol());
        logger.info("====== {}-{}-launch price: {} ======", SYMBOL, CURRENT_STRATEGY, latestPrice);
        StrategyCommon.calculateBuyPriceList(CURRENT_STRATEGY, latestPrice, spot.getPricePrecision());
        usdtBalance = StrategyCommon.getQuotaBalanceByAccountId(CURRENT_STRATEGY, spotAccountId, spot.getQuoteCurrency());
        // 启动后,根据当前价格下单 buy .
        if (usdtBalance.compareTo(spot.getPortionHigh()) >= 0) {
            StrategyCommon.buy(CURRENT_STRATEGY, spot, latestPrice, spot.getPortionHigh(), 2);
        } else {
            logger.info("====== {}-{}-launch: 所剩 usdt 余额不足,等待卖单成交 {} ======", SYMBOL, CURRENT_STRATEGY, usdtBalance.toString());
        }
//        ConcurrentHashMap<String, BigDecimal> sellOrderMap = StrategyCommon.getSellOrderMap();
//        List<Order> sellOrders = StrategyCommon.getOpenOrders(spotAccountId, SYMBOL, OrderSideEnum.SELL);
//        logger.info("====== {}-{}-launch: 现在 all 卖单 {} 个  ======", SYMBOL, currentStrategy, sellOrders.size());
////        sellOrders.forEach(order -> {
////            if ("api".equalsIgnoreCase(order.getSource())) {
////                sellOrderMap.putIfAbsent(order.getId(), order.getAmount());
////            }
////        });
//        logger.info("====== {}-{}-launch: 现有 api 卖单 {} 个  ======", SYMBOL, currentStrategy, sellOrderMap.size());

    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        priceListener();
    }

    /**
     * 监听价格和订单变化
     */
    @Synchronized
    public void priceListener() {
        try {
            latestPrice = StrategyCommon.getCurrentTradPrice(CURRENT_STRATEGY, SYMBOL);
            //价格三倍,WeChat提示并退出
            if (latestPrice.compareTo(spot.getDoublePrice()) >= 0) {
                StrategyCommon.weChatPusher(CURRENT_STRATEGY, "价格翻倍,退出", 2);
                System.exit(0);
            }

            // 处理之前的买单
            ConcurrentHashMap<String, Spot> buyOrderMap = StrategyCommon.getBuyOrderMap();
            ConcurrentHashMap<String, Spot> sellOrderMap = StrategyCommon.getSellOrderMap();
            Iterator<ConcurrentHashMap.Entry<String, Spot>> buyIterator = buyOrderMap.entrySet().iterator();
            Iterator<ConcurrentHashMap.Entry<String, Spot>> sellIterator = sellOrderMap.entrySet().iterator();

            int requestLimitNum = 0;
            while (buyIterator.hasNext()) {
                Map.Entry<String, Spot> entry = buyIterator.next();
                String clientId = entry.getKey();
                Order buyOrder;
                boolean isLimit = true;
                if (clientId.contains(spot.getSymbol())) {
                    //buy market
                    isLimit = false;
                }
                buyOrder = StrategyCommon.getOrderByClientId(CURRENT_STRATEGY, clientId);

                assert buyOrder != null;

                if (OrderStatusEnum.FILLED.getName().equalsIgnoreCase(buyOrder.getState().trim())) {
                    balanceChanged = true;
                    logger.info("====== {}-{}-priceListener-买单已成交 : {} ======", SYMBOL, CURRENT_STRATEGY, buyOrder.toString());
                    BigDecimal buyAmount = buyOrder.getFilledAmount();
                    StrategyCommon.setFee(buyOrder.getFilledCashAmount());
                    BigDecimal buyAtPrice;
                    if (isLimit) {
                        buyAtPrice = buyOrder.getPrice();
                        buyAtPrice = buyAtPrice.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
                    } else {
                        // buy market , amount is usdt; 市场价下单需要计算买入价格
                        buyAtPrice = latestPrice;
                        buyAtPrice = buyAtPrice.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
                    }
                    StrategyCommon.sell(CURRENT_STRATEGY, spot, buyAtPrice, buyAmount, 1);

                    orderCount.incrementAndGet();
                    buyIterator.remove();
                } else if (OrderStatusEnum.CANCELED.getName().equalsIgnoreCase(buyOrder.getState().trim())) {
                    balanceChanged = true;
                    logger.info("====== {}-{}-priceListener-买单已取消 : {} ======", SYMBOL, CURRENT_STRATEGY, buyOrder.toString());
                    orderCount.decrementAndGet();
                    buyIterator.remove();
                }

            }
            // 处理之前的卖单
            while (sellIterator.hasNext() && requestLimitNum < 5) {
                Map.Entry<String, Spot> entry = sellIterator.next();
                String orderId = entry.getKey();
                Order sellOrder = StrategyCommon.getOrderByClientId(CURRENT_STRATEGY, orderId);

                assert sellOrder != null;
                if (OrderStatusEnum.FILLED.getName().equalsIgnoreCase(sellOrder.getState().trim())) {
                    balanceChanged = true;

                    logger.info("====== {}-{}-priceListener-卖单已成交 : {} ======", SYMBOL, CURRENT_STRATEGY, sellOrder.toString());
                    StrategyCommon.setProfit(sellOrder.getFilledCashAmount());
                    orderCount.decrementAndGet();
                    sellIterator.remove();
                } else if (OrderStatusEnum.CANCELED.getName().equalsIgnoreCase(sellOrder.getState().trim())) {
                    logger.info("====== {}-{}-priceListener-卖单已取消 : {} ======", SYMBOL, CURRENT_STRATEGY, sellOrder.toString());
                    orderCount.incrementAndGet();
                    sellIterator.remove();
                }

                requestLimitNum++;

            }

            //本轮买单已全部卖出. 重启应用
            // 启动时,余额不足 不执行此逻辑, insufficientFound=true
            if (sellOrderMap.size() == 0 && !insufficientFound) {
                logger.info("====== {}-{}-priceListener-开始清理残余买单.======", SYMBOL, CURRENT_STRATEGY);
                Iterator<Map.Entry<String, Spot>> iterator = StrategyCommon.getBuyOrderMap().entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<String, Spot> entry = iterator.next();
                    String clientId = entry.getKey();
                    Order remainOrder = StrategyCommon.getOrderByClientId(CURRENT_STRATEGY, clientId);
                    assert remainOrder != null;
                    logger.info("====== {}-{}-priceListener-正在取消订单: {} ======", SYMBOL, CURRENT_STRATEGY, remainOrder.toString());
                    StrategyCommon.cancelOrder(CURRENT_STRATEGY, clientId);
                    iterator.remove();
                }
                BigDecimal pureProfit = StrategyCommon.getProfit().subtract(StrategyCommon.getFee());
                pureProfit = pureProfit.setScale(2, RoundingMode.HALF_UP);
                BigDecimal pointBalance = StrategyCommon.getBalanceByAccountId(CURRENT_STRATEGY, pointAccountId);
                if (pureProfit.compareTo(BigDecimal.ZERO) > 0) {

                    String sb = SYMBOL + " 最新收益: " + pureProfit + "; " +
                            " 点卡余额: " + pointBalance.toString();
                    StrategyCommon.weChatPusher(CURRENT_STRATEGY, sb, 2);
                }
                orderCount.set(-1);
                launch();

            }
            if (balanceChanged) { //订单成交后,更新余额
                usdtBalance = StrategyCommon.getQuotaBalanceByAccountId(CURRENT_STRATEGY, spotAccountId, spot.getQuoteCurrency());
                balanceChanged = false;
            }

            //检测是否需要下单
            ArrayList<BigDecimal> priceList = StrategyCommon.getPriceList();
            AtomicInteger i = new AtomicInteger(0);
            while (i.get() < priceList.size() && latestPrice.compareTo(priceList.get(i.get())) <= 0) {
                i.getAndIncrement();
            }
            if (i.get() >= highCount - 1) {
                level = "medium";
                if (i.get() >= highCount + mediumCount - 1) {
                    level = "low";
                }
                logger.info("====== {}-{}-priceListener-当前阶段: {} ======", SYMBOL, CURRENT_STRATEGY, level);
            }
            System.out.println(" -- SpotTemplate1.priceListener -- " + "currentPrice= " + latestPrice);
            System.out.println(" -- SpotTemplate1.priceListener -- " + "i= " + i.get());
            System.out.println(" -- SpotTemplate1.priceListener -- " + "orderCount= " + orderCount);
            System.out.println(" -- SpotTemplate1.priceListener -- " + "price(i)= " + priceList.get(i.get()));

            //之前买单全部成交后, 才考虑下单.
            // buyOrderMap.size() == 0 避免同一时间 同一价格 多次下单
            if (orderCount.get() + 1 == i.get() && buyOrderMap.size() == 0) {
                if (i.get() < priceList.size()) {
                    BigDecimal usdtPortion = new BigDecimal("10");
                    if (CURRENT_STRATEGY == 1) {
                        if ("high".equalsIgnoreCase(level)) {
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.HIGH_COUNT_1), spot.getPricePrecision(), RoundingMode.HALF_UP);

                        } else if ("medium".equalsIgnoreCase(level)) {
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.MEDIUM_COUNT_1), spot.getPricePrecision(), RoundingMode.HALF_UP);

                        } else if ("low".equalsIgnoreCase(level)) {
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.LOW_COUNT_1), spot.getPricePrecision(), RoundingMode.HALF_UP);

                        }
                    }
                    if (CURRENT_STRATEGY == 2) {
                        if ("high".equalsIgnoreCase(level)) {
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.HIGH_COUNT_2), spot.getPricePrecision(), RoundingMode.HALF_UP);

                        } else if ("medium".equalsIgnoreCase(level)) {
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.MEDIUM_COUNT_2), spot.getPricePrecision(), RoundingMode.HALF_UP);

                        } else if ("low".equalsIgnoreCase(level)) {
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.LOW_COUNT_2), spot.getPricePrecision(), RoundingMode.HALF_UP);

                        }
                    }
                    if (CURRENT_STRATEGY == 3) {
                        if ("high".equalsIgnoreCase(level)) {
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.HIGH_COUNT_3), spot.getPricePrecision(), RoundingMode.HALF_UP);

                        } else if ("medium".equalsIgnoreCase(level)) {
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.MEDIUM_COUNT_3), spot.getPricePrecision(), RoundingMode.HALF_UP);

                        } else if ("low".equalsIgnoreCase(level)) {
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.LOW_COUNT_3), spot.getPricePrecision(), RoundingMode.HALF_UP);

                        }
                    }


                    if (usdtBalance.compareTo(usdtPortion) >= 0) {
                        insufficientFound = false;
                        StrategyCommon.buy(CURRENT_STRATEGY, spot, priceList.get(i.get()), usdtPortion, 1);
                    } else {
                        insufficientFound = true;
                        ticker.getAndAdd(1);
                        if (ticker.get() % 30 == 0) {
                            ticker.getAndSet(1);
                            usdtBalance = StrategyCommon.getQuotaBalanceByAccountId(CURRENT_STRATEGY, spotAccountId, spot.getQuoteCurrency());
                            logger.info("====== {}-{}-priceListener: 所剩 usdt 余额不足,等待卖单成交 {} ======", SYMBOL, CURRENT_STRATEGY, usdtBalance.toString());
                        }
                    }
                }

            }
        } catch (
                SDKException e) {
            logger.error("====== {}-{}-priceListener: {} ======", SYMBOL, CURRENT_STRATEGY, e.getMessage());
        }
    }

}
