package com.huobi;

import com.huobi.client.req.market.SubMarketTradeRequest;
import com.huobi.client.req.trade.SubOrderUpdateV2Request;
import com.huobi.constant.enums.OrderSideEnum;
import com.huobi.exception.SDKException;
import com.huobi.model.generic.Symbol;
import com.huobi.model.trade.Order;
import com.huobi.model.trade.OrderUpdateV2;
import lombok.Synchronized;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
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
 * 高频
 */
public class SpotTemplateWebsocket3 implements Job {
    private static String BASE_CURRENCY = "";
    private static final String QUOTE_CURRENCY = "usdt";
    private static String SYMBOL;
    private static String PORTION;
    // TODO xlp 9/13/21 2:20 AM  :  复制之后, 修改 CURRENT_STRATEGY
    private static int CURRENT_STRATEGY = 3;


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
    private static final Logger logger = LoggerFactory.getLogger(SpotTemplateWebsocket3.class);

    public static void main(String[] args) {
        BASE_CURRENCY = args[0];
        PORTION = args[1];
//        BASE_CURRENCY = "cspr";
//        PORTION = "500";
        if (BASE_CURRENCY == null || BASE_CURRENCY.isEmpty()) {
            BASE_CURRENCY = "ht";
            logger.error("====== main: BASE_CURRENCY == null || BASE_CURRENCY.isEmpty() set BASE_CURRENCY = {} ======", BASE_CURRENCY);
        }
        SYMBOL = BASE_CURRENCY + QUOTE_CURRENCY;

        if (PORTION == null || PORTION.isEmpty()) {
            PORTION = "3000";
            logger.error("====== {}-main: PORTION == null || PORTION.isEmpty() set PORTION = {} ======", SYMBOL, PORTION);
        }
        logger.error("====== main:  SYMBOL = {} ======", SYMBOL);
        logger.error("====== main:  PORTION = {} ======", PORTION);
        logger.error("====== main:  STRATEGY = {} ======", CURRENT_STRATEGY);

        SpotTemplateWebsocket3 spotBuyer = new SpotTemplateWebsocket3();
        spotBuyer.init();
        JobManagement jobManagement = new JobManagement();
        // 5分钟一次
        jobManagement.addJob("50 0/5 * * *  ?", SpotTemplateWebsocket3.class, SYMBOL);
        jobManagement.startJob();

    }

    /**
     * 设置基本参数
     */
    public void init() {
        try {
            orderListener();
            Thread.sleep(3000);
            totalBalance = new BigDecimal(PORTION);
            prepareSpot(totalBalance, CURRENT_STRATEGY);
            HuobiUtil.cancelOpenOrders(spotAccountId, SYMBOL, CURRENT_STRATEGY, OrderSideEnum.BUY);
            StrategyCommon.getBuyOrderMap().clear();
            launch(CURRENT_STRATEGY);
            priceListener();
        } catch (Exception exception) {
            logger.error("====== {}-{}-init-startup: {} ======", SYMBOL, CURRENT_STRATEGY, exception.getMessage());
        }

    }

    @Synchronized
    public void prepareSpot(BigDecimal totalBalance, int currentStrategy) {
        spot.setBaseCurrency(BASE_CURRENCY);
        spot.setQuoteCurrency(QUOTE_CURRENCY);
        spot.setSymbol(SYMBOL);
        List<Symbol> symbolList = CurrentAPI.getApiInstance().getGenericClient().getSymbols();
        symbolList.forEach(symbol -> {
            if (symbol.getBaseCurrency().equalsIgnoreCase(spot.getBaseCurrency()) && symbol.getQuoteCurrency().equalsIgnoreCase(spot.getQuoteCurrency())) {
                spot.setPricePrecision(symbol.getPricePrecision());
                spot.setAmountPrecision(symbol.getAmountPrecision());
                spot.setMinOrderValue(symbol.getMinOrderValue());
                spot.setLimitOrderMinOrderAmt(symbol.getLimitOrderMinOrderAmt());
                spot.setSellMarketMinOrderAmt(symbol.getSellMarketMinOrderAmt());
            }
        });

        spotAccountId = HuobiUtil.getAccountIdByType("spot");
        pointAccountId = HuobiUtil.getAccountIdByType("point");
        spot.setAccountId(spotAccountId);

        usdtBalance = HuobiUtil.getBalanceByAccountId(spotAccountId, spot.getBaseCurrency(), spot.getQuoteCurrency());
        logger.error("{}-{}-prepareSpot: 分配到的仓位: {} ======", SYMBOL, currentStrategy, PORTION);
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

                portionHigh = highBalance.divide(new BigDecimal(String.valueOf(Constants.HIGH_COUNT_2)), RoundingMode.HALF_UP);
                portionHigh = portionHigh.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);

                portionMedium = mediumBalance.divide(new BigDecimal(String.valueOf(Constants.MEDIUM_COUNT_2)), RoundingMode.HALF_UP);
                portionMedium = portionMedium.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);

                portionLow = lowBalance.divide(new BigDecimal(String.valueOf(Constants.LOW_COUNT_2)), RoundingMode.HALF_UP);
                portionLow = portionLow.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
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

                portionHigh = highBalance.divide(new BigDecimal(String.valueOf(Constants.HIGH_COUNT_3)), RoundingMode.HALF_UP);
                portionHigh = portionHigh.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);

                portionMedium = mediumBalance.divide(new BigDecimal(String.valueOf(Constants.MEDIUM_COUNT_3)), RoundingMode.HALF_UP);
                portionMedium = portionMedium.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);

                portionLow = lowBalance.divide(new BigDecimal(String.valueOf(Constants.LOW_COUNT_3)), RoundingMode.HALF_UP);
                portionLow = portionLow.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
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

                portionHigh = highBalance.divide(new BigDecimal(String.valueOf(Constants.HIGH_COUNT_1)), RoundingMode.HALF_UP);
                portionHigh = portionHigh.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);

                portionMedium = mediumBalance.divide(new BigDecimal(String.valueOf(Constants.MEDIUM_COUNT_1)), RoundingMode.HALF_UP);
                portionMedium = portionMedium.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);

                portionLow = lowBalance.divide(new BigDecimal(String.valueOf(Constants.LOW_COUNT_1)), RoundingMode.HALF_UP);
                portionLow = portionLow.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
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
        logger.error("{}-prepareSpot-当前策略: {} ======", SYMBOL, currentStrategy);
        logger.error("{}-prepareSpot-分配到-H-的仓位: {}-{}", SYMBOL, highBalance, spot.getQuoteCurrency());
        logger.error("{}-prepareSpot-分配到-M-的仓位: {}-{}", SYMBOL, mediumBalance, spot.getQuoteCurrency());
        logger.error("{}-prepareSpot-分配到-L-的仓位: {}-{}", SYMBOL, lowBalance, spot.getQuoteCurrency());
        logger.error("{}-prepareSpot-H 每次补仓份额: {}-{}", SYMBOL, portionHigh, spot.getQuoteCurrency());
        logger.error("{}-prepareSpot-H 补仓次数: {}", SYMBOL, highCount);
        logger.error("{}-prepareSpot-M 每次补仓份额: {}-{}", SYMBOL, portionMedium, spot.getQuoteCurrency());
        logger.error("{}-prepareSpot-M 补仓次数: {}", SYMBOL, mediumCount);
        logger.error("{}-prepareSpot-L 每次补仓份额: {}-{}", SYMBOL, portionLow, spot.getQuoteCurrency());
        logger.error("{}-prepareSpot-L 补仓次数: {}", SYMBOL, lowCount);
    }

    public void launch(int currentStrategy) {
        StrategyCommon.resetFeeAndProfit(SYMBOL, currentStrategy);
        latestPrice = HuobiUtil.getCurrentTradPrice(spot.getSymbol());
        spot.setStartPrice(latestPrice);
        spot.setDoublePrice(latestPrice.multiply(new BigDecimal("2")));
        spot.setTriplePrice(latestPrice.multiply(new BigDecimal("3")));
        logger.error("====== {}-{}-SpotTemplate-launch:策略启动: {} ======", SYMBOL, currentStrategy, spot);
        logger.error("====== {}-{}-launch price: {} ======", SYMBOL, currentStrategy, latestPrice);
        StrategyCommon.calculateBuyPriceList(currentStrategy, latestPrice, spot.getPricePrecision());
        usdtBalance = HuobiUtil.getBalanceByAccountId(spotAccountId, spot.getBaseCurrency(), spot.getQuoteCurrency());
        // 启动后,根据当前价格下单 buy .
        if (usdtBalance.compareTo(spot.getPortionHigh()) >= 0) {
            StrategyCommon.buy(currentStrategy, spot, latestPrice, spot.getPortionHigh(), 2);
        } else {
            logger.error("====== {}-{}-launch: 所剩 usdt 余额不足,等待卖单成交 {} ======", SYMBOL, currentStrategy, usdtBalance.toString());
        }

        List<Order> sellOrders = HuobiUtil.getOpenOrders(spotAccountId, SYMBOL, OrderSideEnum.SELL);
        logger.error("====== {}-{}-launch: 现在 all 卖单 {} 个  ======", SYMBOL, currentStrategy, sellOrders.size());
    }


    /**
     * websocket
     * 监听价格变化
     */
    public void priceListener() {
        try {
            CurrentAPI.getApiInstance().getMarketClient().subMarketTrade(SubMarketTradeRequest.builder().symbol(SYMBOL).build(), (marketTradeEvent) -> {
                marketTradeEvent.getList().forEach(marketTrade -> {
                    StopWatch clock = new StopWatch();
                    clock.start(); // 计时开始
                    latestPrice = marketTrade.getPrice();
                    //价格三倍,WeChat提示并退出
                    if (latestPrice.compareTo(spot.getTriplePrice()) >= 0) {
                        HuobiUtil.weChatPusher(CURRENT_STRATEGY, "价格三倍,退出", 2);
                        System.exit(0);
                    }
                    // 价格翻倍,策略提档
                    if (latestPrice.compareTo(spot.getDoublePrice()) >= 0) {
                        spot.setDoublePrice(latestPrice.multiply(new BigDecimal("2")));
                        if (CURRENT_STRATEGY < 3) {
                            // 提升一档, 高频变成稳健, 稳健变成保守, 保守直接退出
                            CURRENT_STRATEGY += 1;
                            prepareSpot(totalBalance.divide(new BigDecimal("2"), RoundingMode.HALF_DOWN), CURRENT_STRATEGY);
                            launch(CURRENT_STRATEGY);
                            HuobiUtil.weChatPusher(CURRENT_STRATEGY, SYMBOL + " 价格翻倍,策略提档", 2);

                        }
                    }

                    ConcurrentHashMap<String, BigDecimal> buyOrderMap = StrategyCommon.getBuyOrderMap();
                    ConcurrentHashMap<String, BigDecimal> sellOrderMap = StrategyCommon.getSellOrderMap();
                    //本轮买单已全部卖出. 重启应用
                    // 启动时,余额不足 不执行此逻辑, insufficientFound=true
                    if (sellOrderMap.size() == 0 && !insufficientFound) {
                        logger.error("====== {}-{}-priceListener-开始清理残余买单.======", SYMBOL, CURRENT_STRATEGY);
                        Iterator<Map.Entry<String, BigDecimal>> iterator = StrategyCommon.getBuyOrderMap().entrySet().iterator();

                        while (iterator.hasNext()) {
                            Map.Entry<String, BigDecimal> entry = iterator.next();
                            String clientId = entry.getKey();
                            Order remainOrder = HuobiUtil.getOrderByClientId(clientId);
                            logger.error("====== {}-{}-priceListener-正在取消订单: {} ======", SYMBOL, CURRENT_STRATEGY, remainOrder.toString());
                            HuobiUtil.cancelOrder(CURRENT_STRATEGY, clientId);
                            iterator.remove();
                        }
                        BigDecimal pureProfit = StrategyCommon.getProfit().subtract(StrategyCommon.getFee());
                        pureProfit = pureProfit.setScale(2, RoundingMode.HALF_UP);
                        BigDecimal pointBalance = HuobiUtil.getBalanceByAccountId(pointAccountId);
                        if (pureProfit.compareTo(BigDecimal.ZERO) > 0) {

                            String sb = SYMBOL + " 最新收益: " + pureProfit + "; " +
                                    " 点卡余额: " + pointBalance.toString();
                            HuobiUtil.weChatPusher(CURRENT_STRATEGY, sb, 2);
                        }
                        orderCount.set(-1);
                    }
                    if (balanceChanged) { //订单成交后,更新余额
                        usdtBalance = HuobiUtil.getBalanceByAccountId(spotAccountId, spot.getBaseCurrency(), spot.getQuoteCurrency());
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

//                    System.out.println("buyOrderMap.size(): {" + buyOrderMap.size() + "} ,currentPrice: {" + latestPrice + "} ,i= {" + i.get() + "} ,orderCount = {" + orderCount + "} ,price(i)= {" + priceList.get(i.get()) + "}");
                    //之前买单全部成交后, 才考虑下单. buyOrderMap.size() == 0 避免同一时间 同一价格 多次下单
                    if (orderCount.get() + 1 == i.get() && buyOrderMap.size() == 0) {
                        if (i.get() < priceList.size()) {
                            BigDecimal usdtPortion = new BigDecimal("10");
                            if (CURRENT_STRATEGY == 1) {
                                if ("high".equalsIgnoreCase(level)) {
                                    usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.HIGH_COUNT_1), RoundingMode.HALF_UP);

                                } else if ("medium".equalsIgnoreCase(level)) {
                                    usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.MEDIUM_COUNT_1), RoundingMode.HALF_UP);

                                } else if ("low".equalsIgnoreCase(level)) {
                                    usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.LOW_COUNT_1), RoundingMode.HALF_UP);

                                }
                            } else if (CURRENT_STRATEGY == 2) {
                                if ("high".equalsIgnoreCase(level)) {
                                    usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.HIGH_COUNT_2), RoundingMode.HALF_UP);

                                } else if ("medium".equalsIgnoreCase(level)) {
                                    usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.MEDIUM_COUNT_2), RoundingMode.HALF_UP);

                                } else if ("low".equalsIgnoreCase(level)) {
                                    usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.LOW_COUNT_2), RoundingMode.HALF_UP);

                                }
                            } else if (CURRENT_STRATEGY == 3) {
                                if ("high".equalsIgnoreCase(level)) {
                                    usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.HIGH_COUNT_3), RoundingMode.HALF_UP);

                                } else if ("medium".equalsIgnoreCase(level)) {
                                    usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.MEDIUM_COUNT_3), RoundingMode.HALF_UP);

                                } else if ("low".equalsIgnoreCase(level)) {
                                    usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.LOW_COUNT_3), RoundingMode.HALF_UP);

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
                                    usdtBalance = HuobiUtil.getBalanceByAccountId(spotAccountId, spot.getBaseCurrency(), spot.getQuoteCurrency());
                                    logger.info("====== {}-{}-priceListener: 所剩 usdt 余额不足,等待卖单成交 {} ======", SYMBOL, CURRENT_STRATEGY, usdtBalance.toString());
                                }
                            }
                        }
                    }
                    clock.stop();
                    long executeTime = clock.getTime();
                    logger.info("symbol: {}, currentPrice: {}, direction: {}, executeTime: {}ms", SYMBOL, latestPrice, marketTrade.getDirection(), executeTime);
                });
            });
        } catch (SDKException e) {
            logger.error("====== {}-{}-priceListener: {} ======", SYMBOL, CURRENT_STRATEGY, e.getMessage());
        }

    }

    /**
     * websocket
     * 监听订单状态
     */
    public void orderListener() {
        CurrentAPI.getApiInstance().getTradeClient().subOrderUpdateV2(SubOrderUpdateV2Request.builder().symbols(SYMBOL).build(), orderUpdateV2Event -> {
//            System.out.println(" -- SpotTemplateWebsocket1.orderListener -- " + orderUpdateV2Event.toString());
            ConcurrentHashMap<String, BigDecimal> buyOrderMap = StrategyCommon.getBuyOrderMap();
            ConcurrentHashMap<String, BigDecimal> sellOrderMap = StrategyCommon.getSellOrderMap();
            OrderUpdateV2 order = orderUpdateV2Event.getOrderUpdate();
            String clientOrderId = order.getClientOrderId();
            Long orderId = order.getOrderId();


            // 确保多个策略的订单不会互相影响
            if (buyOrderMap.containsKey(clientOrderId)
                    || sellOrderMap.containsKey(clientOrderId)
                    || sellOrderMap.containsKey(String.valueOf(orderId))
            ) {
                // 已成交
                if ("filled".equalsIgnoreCase(order.getOrderStatus())) {
                    balanceChanged = true;
                    BigDecimal orderTradePrice = order.getTradePrice();
                    BigDecimal orderTradeVolume = order.getTradeVolume();
                    logger.error("====== {}-{}-{}已成交 : price: {}, amount: {} ======", SYMBOL, CURRENT_STRATEGY, order.getType(), orderTradePrice, orderTradeVolume);
                    if ("buy-limit".equalsIgnoreCase(order.getType()) || "buy-market".equalsIgnoreCase(order.getType())) {
                        orderCount.incrementAndGet();
                        StrategyCommon.setFee(orderTradePrice.multiply(orderTradeVolume));
                        buyOrderMap.remove(clientOrderId);
                        StrategyCommon.sell(CURRENT_STRATEGY, spot, orderTradePrice, orderTradeVolume, 1);
                    } else if ("sell-limit".equalsIgnoreCase(order.getType())) {
                        orderCount.decrementAndGet();
                        StrategyCommon.setProfit(orderTradePrice.multiply(orderTradeVolume));
                        if (StringUtils.isNotBlank(clientOrderId)) {
                            sellOrderMap.remove(clientOrderId);
                        } else {
                            sellOrderMap.remove(String.valueOf(orderId));
                        }

                    }
                } else if ("canceled".equalsIgnoreCase(order.getOrderStatus())) {
                    logger.error("====== {}-{}-{}-已取消 : {} ======", SYMBOL, CURRENT_STRATEGY, order.getType(), order.toString());
                    if ("buy-limit".equalsIgnoreCase(order.getType()) || "buy-market".equalsIgnoreCase(order.getType())) {
                        balanceChanged = true;
                        orderCount.decrementAndGet();
                        buyOrderMap.remove(clientOrderId);
                    }
                    if ("sell-limit".equalsIgnoreCase(order.getType()) || "sell-market".equalsIgnoreCase(order.getType())) {
                        orderCount.incrementAndGet();
                        if (StringUtils.isNotBlank(clientOrderId)) {
                            sellOrderMap.remove(clientOrderId);
                        } else {
                            sellOrderMap.remove(String.valueOf(orderId));
                        }
                        logger.error("====== SpotTemplateWebsocket1-orderListener: 现有卖单 {} 个 ======", sellOrderMap.size());

                    }

                }

            }


        });
    }

    /**
     * 定时任务处理之前的买单,卖单, 防止 websocket 断掉,买/卖单 没有及时更新
     */
    public void checkOrderStatus() {
        ConcurrentHashMap<String, BigDecimal> buyOrderMap = StrategyCommon.getBuyOrderMap();
        ConcurrentHashMap<String, BigDecimal> sellOrderMap = StrategyCommon.getSellOrderMap();
        Iterator<ConcurrentHashMap.Entry<String, BigDecimal>> buyIterator = buyOrderMap.entrySet().iterator();
        Iterator<ConcurrentHashMap.Entry<String, BigDecimal>> sellIterator = sellOrderMap.entrySet().iterator();
        // TODO xlp 9/12/21 4:37 AM  : * 每个API Key 在1秒之内限制10次  * 若接口不需要API Key，则每个IP在1秒内限制10次
        while (buyIterator.hasNext()) {
            Map.Entry<String, BigDecimal> entry = buyIterator.next();
            String clientId = entry.getKey();
            boolean isLimit = true;
            Order buyOrder = HuobiUtil.getOrderByClientId(clientId);

            if (!clientId.contains(spot.getSymbol())) {
                //buy market
                isLimit = false;
            }


            if ("filled".equalsIgnoreCase(buyOrder.getState().trim())) {
                balanceChanged = true;
                logger.error("====== {}-{}-checkOrderStatus-买单已成交 : {} ======", SYMBOL, CURRENT_STRATEGY, buyOrder.toString());
                BigDecimal buyAmount = buyOrder.getFilledAmount();
                // TODO xlp 9/7/21 11:01 AM  :  matchresults 接口获取准确值
                BigDecimal buyAtPrice;
                if (isLimit) {
                    buyAtPrice = new BigDecimal(String.valueOf(buyOrder.getPrice()));
                    StrategyCommon.setFee(buyAtPrice.multiply(buyAmount));
                } else {
                    buyAtPrice = HuobiUtil.getCurrentTradPrice(SYMBOL);
                    StrategyCommon.setFee(buyAmount);
                }
                buyAtPrice = buyAtPrice.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);

                StrategyCommon.sell(CURRENT_STRATEGY, spot, buyAtPrice, buyAmount, 1);
                orderCount.incrementAndGet();
                buyIterator.remove();
            } else if ("canceled".equalsIgnoreCase(buyOrder.getState().trim())) {
                balanceChanged = true;
                logger.error("====== {}-{}-checkOrderStatus-买单已取消 : {} ======", SYMBOL, CURRENT_STRATEGY, buyOrder.toString());
                orderCount.decrementAndGet();
                buyIterator.remove();
            }

        }

        while (sellIterator.hasNext()) {
            Map.Entry<String, BigDecimal> entry = sellIterator.next();
            String orderId = entry.getKey();
            Order sellOrder = HuobiUtil.getOrderByClientId(orderId);
            if ("filled".equalsIgnoreCase(sellOrder.getState().trim())) {
                balanceChanged = true;
                StrategyCommon.setProfit(sellOrder.getFilledAmount().multiply(sellOrder.getPrice()));
                logger.error("====== {}-{}-checkOrderStatus-卖单已成交 : {} ======", SYMBOL, CURRENT_STRATEGY, sellOrder.toString());
                orderCount.decrementAndGet();
                sellIterator.remove();
            } else if ("canceled".equalsIgnoreCase(sellOrder.getState().trim())) {
                logger.error("====== {}-{}-checkOrderStatus-卖单已取消 : {} ======", SYMBOL, CURRENT_STRATEGY, sellOrder.toString());
                orderCount.incrementAndGet();
                sellIterator.remove();
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                logger.error("====== SpotTemplateWebsocket1-checkOrderStatus: {} ======", e.getMessage());

            }
        }

    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        checkOrderStatus();
    }
}
