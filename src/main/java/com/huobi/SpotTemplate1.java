package com.huobi;

import com.huobi.constant.Constants;
import com.huobi.constant.enums.OrderSideEnum;
import com.huobi.exception.SDKException;
import com.huobi.model.generic.Symbol;
import com.huobi.model.trade.Order;
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
 * 高频
 */
public class SpotTemplate1 implements Job {
    private static String BASE_CURRENCY = "";
    private static final String QUOTE_CURRENCY = "usdt";
    private static String SYMBOL = ""; //htusdt
    private static String PORTION = "2000";
    private static final int CURRENT_STRATEGY = 1;

    private static volatile boolean insufficientFound = true;
    private static volatile boolean balanceChanged = false;
    private static Long spotAccountId = 14086863L;
    private static Long pointAccountId = 14424186L;
    private final static Spot spot = new Spot();
    private final AtomicInteger orderCount = new AtomicInteger(0);
    private static BigDecimal usdtBalance = BigDecimal.ZERO;
    private static final AtomicInteger ticker = new AtomicInteger();
    private static double highCount = 0;
    private static double mediumCount = 0;
    private static double lowCount = 0;
    private String level = "high";
    private static final Logger logger = LoggerFactory.getLogger(SpotTemplate1.class);

    public static void main(String[] args) {
        BASE_CURRENCY = args[0];
        PORTION = args[1];
//        BASE_CURRENCY = "ht";
//        PORTION = "2000";
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

        SpotTemplate1 spotBuyer = new SpotTemplate1();
        spotBuyer.init();
        JobManagement jobManagement = new JobManagement();
        jobManagement.addJob("0/5 * * * * ?", SpotTemplate1.class, SYMBOL);
        // TODO xlp 9/5/21 5:56 AM  : 创建新币种启动类后, 创建 EveryDayPush
//        jobManagement.addJob("0 0 8,12,19,22 * * ?", new EveryDayPush(BASE_CURRENCY).getClass(), SYMBOL + "-PUSH");
        jobManagement.startJob();
    }

    /**
     * 设置基本参数
     */
    public void init() {
        try {
            prepareSpot();
            HuobiUtil.cancelOpenOrders(spotAccountId, SYMBOL, CURRENT_STRATEGY, OrderSideEnum.BUY);
            launch();
        } catch (Exception exception) {
            logger.error("====== {}-{}-init-startup: {} ======", SYMBOL, CURRENT_STRATEGY, exception.getMessage());
        }

    }


    public synchronized void prepareSpot() {
        spot.setBaseCurrency(BASE_CURRENCY);
        spot.setQuoteCurrency(QUOTE_CURRENCY);
        BigDecimal totalBalance = new BigDecimal(PORTION);
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

        usdtBalance = usdtBalance.add(HuobiUtil.getBalanceByAccountId(spotAccountId, spot.getBaseCurrency(), spot.getQuoteCurrency()));
        logger.error("{}-{}-prepareSpot: 分配到的仓位: {} ======", SYMBOL, CURRENT_STRATEGY, PORTION);
        spot.setTotalBalance(totalBalance);
        BigDecimal highBalance;
        BigDecimal mediumBalance;
        BigDecimal lowBalance;
        BigDecimal portionHigh;
        BigDecimal portionMedium;
        BigDecimal portionLow;
        //2. 稳健 3.保守
        switch (CURRENT_STRATEGY) {
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
        logger.error("{}-prepareSpot-当前策略: {} ======", SYMBOL, CURRENT_STRATEGY);
        logger.error("{}-prepareSpot-分配到-H-的仓位: {}-{}", SYMBOL, highBalance, spot.getQuoteCurrency());
        logger.error("{}-prepareSpot-分配到-M-的仓位: {}-{}", SYMBOL, mediumBalance, spot.getQuoteCurrency());
        logger.error("{}-prepareSpot-分配到-L-的仓位: {}-{}", SYMBOL, lowBalance, spot.getQuoteCurrency());
        logger.error("{}-prepareSpot-H 每次补仓份额: {}-{}", SYMBOL, portionHigh, spot.getQuoteCurrency());
        logger.error("{}-prepareSpot-H 补仓次数: {}", SYMBOL, highCount);
        logger.error("{}-prepareSpot-M 每次补仓份额: {}-{}", SYMBOL, portionMedium, spot.getQuoteCurrency());
        logger.error("{}-prepareSpot-M 补仓次数: {}", SYMBOL, mediumCount);
        logger.error("{}-prepareSpot-L 每次补仓份额: {}-{}", SYMBOL, portionLow, spot.getQuoteCurrency());
        logger.error("{}-prepareSpot-L 补仓次数: {}", SYMBOL, lowCount);

        logger.info("====== SpotTemplate-prepareSpot: {}-{}", spot.toString(), spot.getQuoteCurrency());

    }

    public void launch() {
        StrategyCommon.resetFeeAndProfit(SYMBOL, CURRENT_STRATEGY);
//        HuobiUtil.weChatPusher("策略启动: " + spot.toString(), 1);
        logger.error("====== {}-{}-SpotTemplate-launch:策略启动: {} ======", SYMBOL, CURRENT_STRATEGY, spot);
        BigDecimal currentTradPrice = HuobiUtil.getCurrentTradPrice(spot.getSymbol());
        logger.error("====== {}-{}-launch price: {} ======", SYMBOL, CURRENT_STRATEGY, currentTradPrice);
        StrategyCommon.calculateBuyPriceList(CURRENT_STRATEGY, currentTradPrice, spot.getPricePrecision());
        usdtBalance = usdtBalance.min(HuobiUtil.getBalanceByAccountId(spotAccountId, spot.getBaseCurrency(), spot.getQuoteCurrency()));
        // 启动后,根据当前价格下单 buy .
        if (usdtBalance.compareTo(spot.getPortionHigh()) >= 0) {
            StrategyCommon.buyMarket(CURRENT_STRATEGY, spot, currentTradPrice, spot.getPortionHigh());
        } else {
            logger.error("====== {}-{}-launch: 所剩 usdt 余额不足,等待卖单成交 {} ======", SYMBOL, CURRENT_STRATEGY, usdtBalance.toString());
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        priceListener();
    }

    /**
     * 监听价格变化
     */
    public synchronized void priceListener() {
        try {
            BigDecimal latestPrice = HuobiUtil.getCurrentTradPrice(SYMBOL);
            // 处理之前的买单,卖单
            ConcurrentHashMap<String, BigDecimal> buyOrderMap = StrategyCommon.getBuyOrderMap();
            ConcurrentHashMap<Long, BigDecimal> sellOrderMap = StrategyCommon.getSellOrderMap();
            Iterator<ConcurrentHashMap.Entry<String, BigDecimal>> buyIterator = buyOrderMap.entrySet().iterator();
            Iterator<ConcurrentHashMap.Entry<Long, BigDecimal>> sellIterator = sellOrderMap.entrySet().iterator();

            while (buyIterator.hasNext()) {
                Map.Entry<String, BigDecimal> entry = buyIterator.next();
                String clientId = entry.getKey();
                Order buyOrder;
                boolean isLimit = true;
                BigDecimal buyPrice;
                if (clientId.contains(spot.getSymbol())) {
                    //buy limit
                    buyOrder = HuobiUtil.getOrderByClientId(clientId);
                    buyPrice = buyOrder.getPrice();
                } else {
                    //buy market
                    isLimit = false;
                    buyOrder = HuobiUtil.getOrderByOrderId(Long.parseLong(clientId));
                    buyPrice = latestPrice;
                }

                if ("filled".equalsIgnoreCase(buyOrder.getState().trim())) {
                    balanceChanged = true;
                    logger.error("====== {}-{}-priceListener-买单已成交 : {} ======", SYMBOL, CURRENT_STRATEGY, buyOrder.toString());
                    BigDecimal buyAmount = buyOrder.getFilledAmount();
                    // TODO xlp 9/7/21 11:01 AM  :  matchresults 接口获取准确值
                    StrategyCommon.setFee(buyOrder.getFilledFees());
                    if (isLimit) {
                        BigDecimal cost = buyAmount.multiply(buyPrice);
                        StrategyCommon.setFee(cost);
                        BigDecimal buyAtPrice = new BigDecimal(String.valueOf(buyOrder.getPrice()));
                        buyAtPrice = buyAtPrice.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
                        StrategyCommon.sellLimit(CURRENT_STRATEGY, spot, buyAtPrice, buyAmount);
                    } else {
                        // buy market , amount is usdt;
                        BigDecimal cost = buyOrder.getAmount();
                        StrategyCommon.setFee(cost);
                        BigDecimal buyAtPrice = latestPrice;
                        buyAtPrice = buyAtPrice.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
                        StrategyCommon.sellLimit(CURRENT_STRATEGY, spot, buyAtPrice, buyAmount);
                    }
                    orderCount.getAndIncrement();
                    buyIterator.remove();
                } else if ("canceled".equalsIgnoreCase(buyOrder.getState().trim())) {
                    balanceChanged = true;
                    logger.error("====== {}-{}-priceListener-买单已取消 : {} ======", SYMBOL, CURRENT_STRATEGY, buyOrder.toString());
                    orderCount.getAndDecrement();
                    buyIterator.remove();
                }
            }

            while (sellIterator.hasNext()) {
                Map.Entry<Long, BigDecimal> entry = sellIterator.next();
                Long orderId = entry.getKey();
                Order sellOrder = HuobiUtil.getOrderByOrderId(orderId);

                if ("filled".equalsIgnoreCase(sellOrder.getState().trim())) {
                    balanceChanged = true;

                    logger.error("====== {}-{}-priceListener-卖单已成交 : {} ======", SYMBOL, CURRENT_STRATEGY, sellOrder.toString());
                    logger.info(sellOrder.toString());
                    BigDecimal sellPrice = sellOrder.getPrice();
                    BigDecimal sellAmount = sellOrder.getAmount();
                    BigDecimal gain = sellPrice.multiply(sellAmount);
                    StrategyCommon.setProfit(gain);
                    StrategyCommon.setFee(sellOrder.getFilledFees());
                    orderCount.getAndDecrement();
                    sellIterator.remove();
                } else if ("canceled".equalsIgnoreCase(sellOrder.getState().trim())) {
                    logger.error("====== {}-{}-priceListener-卖单已取消 : {} ======", SYMBOL, CURRENT_STRATEGY, sellOrder.toString());
                    sellIterator.remove();
                }

            }

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
                orderCount.getAndSet(0);

                launch();

            }
            if (balanceChanged) { //订单成交后,更新余额
                BigDecimal currentBalance = HuobiUtil.getBalanceByAccountId(spotAccountId, spot.getBaseCurrency(), spot.getQuoteCurrency());
                usdtBalance = usdtBalance.max(currentBalance);
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
                if (i.get() >= mediumCount - 1) {
                    level = "low";
                }
                logger.info("====== {}-{}-priceListener-当前阶段: {} ======", SYMBOL, CURRENT_STRATEGY, level);

            }
            //之前买单全部成交后, 才考虑下单.
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
                    }
                    if (CURRENT_STRATEGY == 2) {
                        if ("high".equalsIgnoreCase(level)) {
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.HIGH_COUNT_2), RoundingMode.HALF_UP);

                        } else if ("medium".equalsIgnoreCase(level)) {
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.MEDIUM_COUNT_2), RoundingMode.HALF_UP);

                        } else if ("low".equalsIgnoreCase(level)) {
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.LOW_COUNT_2), RoundingMode.HALF_UP);

                        }
                    }
                    if (CURRENT_STRATEGY == 3) {
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
                        StrategyCommon.buyLimit(CURRENT_STRATEGY, spot, priceList.get(i.get()), usdtPortion);
                    } else {
                        insufficientFound = true;
                        ticker.getAndAdd(1);
                        if (ticker.get() % 10 == 0) {
                            ticker.getAndSet(1);
                            logger.info("====== {}-{}-priceListener: 所剩 usdt 余额不足,等待卖单成交 {} ======", SYMBOL, CURRENT_STRATEGY, usdtBalance.toString());
                            usdtBalance = usdtBalance.max(HuobiUtil.getBalanceByAccountId(spotAccountId, spot.getBaseCurrency(), spot.getQuoteCurrency()));
                        }
                    }
                }

            }
        } catch (SDKException e) {
            logger.error("====== {}-{}-priceListener: {} ======", SYMBOL, CURRENT_STRATEGY, e.getMessage());
        }
    }

}