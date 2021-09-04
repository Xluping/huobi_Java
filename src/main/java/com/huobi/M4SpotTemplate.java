package com.huobi;

import com.huobi.constant.Constants;
import com.huobi.constant.enums.OrderSideEnum;
import com.huobi.exception.SDKException;
import com.huobi.model.generic.Symbol;
import com.huobi.model.trade.Order;
import com.huobi.push.DACEveryDayPush;
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
 */
public class M4SpotTemplate implements Job {
    private static final String BASE_CURRENCY = "dac";
    private static final String QUOTE_CURRENCY = "usdt";
    private static final String SYMBOL = BASE_CURRENCY + QUOTE_CURRENCY; //htusdt
    private static String PORTION = "2000";
    private static final int CURRENT_STRATEGY = 1;

    private static volatile boolean insufficientFound = true;
    private static volatile boolean balanceChanged = false;
    private static Long spotAccountId = 14086863L;
    private static Long pointAccountId = 14424186L;
    private final static Spot spot = new Spot();
    private final AtomicInteger orderCount = new AtomicInteger(0);
    private static BigDecimal usdtBalance = new BigDecimal("0");
    private static final AtomicInteger ticker = new AtomicInteger();
    private static double highCount = 0;
    private static double mediumCount = 0;
    private String level = "high";
    private static final Logger logger = LoggerFactory.getLogger(M4SpotTemplate.class);

    public static void main(String[] args) {
        PORTION = args[0];
        if (PORTION == null || PORTION.isEmpty()) {
            PORTION = "3000";
            logger.info("====== {}-main: PORTION == null || PORTION.isEmpty() set PORTION = {} ======", SYMBOL, PORTION);
        }
        logger.info("====== {}-main:  PORTION = {} ======", SYMBOL, PORTION);

        M4SpotTemplate spotBuyer = new M4SpotTemplate();
        spotBuyer.init();
        JobManagement jobManagement = new JobManagement();
        jobManagement.addJob("0/5 * * * * ?", M4SpotTemplate.class, SYMBOL);
        jobManagement.addJob("0 0 8,12,19,22 * * ?", DACEveryDayPush.class, SYMBOL + "-PUSH");
        jobManagement.startJob();
    }

    /**
     * 设置基本参数
     */
    public void init() {
        try {
            prepareSpot();
            HuobiUtil.cancelOpenOrders(spotAccountId, SYMBOL, OrderSideEnum.BUY);
            launch();
        } catch (Exception exception) {
            logger.error("====== {} SpotBuyer-startup: {} ======", SYMBOL, exception.getMessage());
        }

    }


    public void prepareSpot() {
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
        logger.error(" {} -prepareSpot: 分配到的仓位: {} ======", SYMBOL, PORTION);
        spot.setTotalBalance(totalBalance);
        BigDecimal highBalance = null;
        BigDecimal mediumBalance = null;
        BigDecimal lowBalance = null;
        BigDecimal portionHigh = null;
        BigDecimal portionMedium = null;
        BigDecimal portionLow = null;

        switch (CURRENT_STRATEGY) {
            case 1: //高频
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

                break;
            case 2: //稳健
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

                break;
            case 3: //保守
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
                break;
        }
        spot.setHighStrategyBalance(highBalance);
        spot.setMediumStrategyBalance(mediumBalance);
        spot.setLowStrategyBalance(lowBalance);
        spot.setPortionHigh(portionHigh);
        spot.setPortionMedium(portionMedium);
        spot.setPortionLow(portionLow);
        logger.error(SYMBOL + "-SpotBuyer-当前策略: {} ======", CURRENT_STRATEGY);
        logger.error(SYMBOL + "-SpotBuyer-分配到-H-的仓位: {}-{}", highBalance, spot.getQuoteCurrency());
        logger.error(SYMBOL + "-SpotBuyer-分配到-M-的仓位: {}-{}", mediumBalance, spot.getQuoteCurrency());
        logger.error(SYMBOL + "-SpotBuyer-分配到-L-的仓位: {}-{}", lowBalance, spot.getQuoteCurrency());
        logger.error(SYMBOL + "-SpotBuyer-H 每次补仓份额: {}-{}", portionHigh, spot.getQuoteCurrency());
        logger.error(SYMBOL + "-SpotBuyer-M 每次补仓份额: {}-{}", portionMedium, spot.getQuoteCurrency());
        logger.error(SYMBOL + "-SpotBuyer-L 每次补仓份额: {}-{}", portionLow, spot.getQuoteCurrency());
        logger.info("====== SpotTemplate-prepareSpot: {}-{}", spot.toString(), spot.getQuoteCurrency());

    }

    public void launch() {
        StrategyCommon.reset();
        HuobiUtil.weChatPusher("策略启动: " + spot.toString(), 1);
        BigDecimal currentTradPrice = HuobiUtil.getCurrentTradPrice(spot.getSymbol());
        logger.error(SYMBOL + "-startUp price: {} ======", currentTradPrice);
        StrategyCommon.calculateBuyPriceList(CURRENT_STRATEGY, currentTradPrice, spot.getPricePrecision());
        usdtBalance = usdtBalance.max(HuobiUtil.getBalanceByAccountId(spotAccountId, spot.getBaseCurrency(), spot.getQuoteCurrency()));
        // 启动后,根据当前价格下单 buy .
        if (usdtBalance.compareTo(spot.getPortionHigh()) >= 0) {
            StrategyCommon.placeBuyOrder(spot, currentTradPrice, spot.getPortionHigh());
        } else {
            logger.error("{}-startup: 所剩 usdt 余额不足,等待卖单成交 {} ======", SYMBOL, usdtBalance.toString());
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        priceListener();
    }

    /**
     * 监听价格变化
     */
    public void priceListener() {
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
                Order buyOrder = HuobiUtil.getOrderByClientId(clientId);
                BigDecimal buyPrice = buyOrder.getPrice();
                BigDecimal buyAmount = buyOrder.getAmount();
                if (buyOrder.getState().trim().equalsIgnoreCase("filled")) {
                    balanceChanged = true;

                    logger.error("====== {}-SpotBuyer-买单已成交 : {} ======", SYMBOL, buyOrder.toString());
                    BigDecimal cost = buyAmount.multiply(buyPrice);
                    StrategyCommon.setFee(cost);
                    StrategyCommon.setFee(buyOrder.getFilledFees());
                    StrategyCommon.placeSellOrder(CURRENT_STRATEGY, spot, new BigDecimal(String.valueOf(buyOrder.getPrice())), buyAmount);
                    orderCount.getAndIncrement();
                    buyIterator.remove();
                } else if (buyOrder.getState().trim().equalsIgnoreCase("canceled")) {
                    logger.error("====== {}-SpotBuyer-买单已取消 : {} ======", SYMBOL, buyOrder.toString());
                    orderCount.getAndDecrement();
                    buyIterator.remove();
                }
            }

            while (sellIterator.hasNext()) {
                Map.Entry<Long, BigDecimal> entry = sellIterator.next();
                Long orderId = entry.getKey();
                Order sellOrder = HuobiUtil.getOrderByOrderId(orderId);

                if (sellOrder.getState().trim().equalsIgnoreCase("filled")) {
                    balanceChanged = true;

                    logger.error("====== {}-SpotBuyer-卖单已成交 : {} ======", SYMBOL, sellOrder.toString());
                    logger.info(sellOrder.toString());
                    BigDecimal sellPrice = sellOrder.getPrice();
                    BigDecimal sellAmount = sellOrder.getAmount();
                    BigDecimal gain = sellPrice.multiply(sellAmount);
                    StrategyCommon.setProfit(gain);
                    StrategyCommon.setFee(sellOrder.getFilledFees());
                    orderCount.getAndDecrement();
                    sellIterator.remove();
                } else if (sellOrder.getState().trim().equalsIgnoreCase("canceled")) {
                    logger.error("====== {}-SpotBuyer-卖单已取消 : {} ======", SYMBOL, sellOrder.toString());
                    sellIterator.remove();
                }

            }
            //本轮买单已全部卖出. 重启应用
            if (sellOrderMap.size() == 0 && !insufficientFound) {
                logger.error("====== {}-SpotBuyer-开始清理残余买单.======", SYMBOL);
                Iterator<Map.Entry<String, BigDecimal>> iterator = StrategyCommon.getBuyOrderMap().entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<String, BigDecimal> entry = iterator.next();
                    String clientId = entry.getKey();
                    Order remainOrder = HuobiUtil.getOrderByClientId(clientId);
                    logger.error("====== {}-SpotBuyer-正在取消订单: {} ======", SYMBOL, remainOrder.toString());
                    HuobiUtil.cancelOrder(clientId);
                    iterator.remove();
                }
                BigDecimal pureProfit = StrategyCommon.getProfit().subtract(StrategyCommon.getFee());
                pureProfit = pureProfit.setScale(2, RoundingMode.HALF_UP);
                BigDecimal pointBalance = HuobiUtil.getBalanceByAccountId(pointAccountId);
                if (pureProfit.compareTo(new BigDecimal("0")) > 0) {

                    StringBuilder sb = new StringBuilder();
                    sb.append(SYMBOL).append(" 最新收益: ").append(pureProfit).append("; ");
                    sb.append(" 点卡余额: ").append(pointBalance.toString());
                    HuobiUtil.weChatPusher(sb.toString(), 2);
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
                logger.info("====== {}-SpotBuyer-当前阶段: {} ======", SYMBOL, level);

            }
            //之前买单全部成交后, 才考虑下单.
            if (orderCount.get() + 1 == i.get() && buyOrderMap.size() == 0) {
                if (i.get() < priceList.size()) {
                    BigDecimal usdtPortion = new BigDecimal("10");
                    if (CURRENT_STRATEGY == 1) {
                        if (level.equalsIgnoreCase("high")) {
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.HIGH_COUNT_1), RoundingMode.HALF_UP);

                        } else if (level.equalsIgnoreCase("medium")) {
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.MEDIUM_COUNT_1), RoundingMode.HALF_UP);

                        } else if (level.equalsIgnoreCase("low")) {
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.LOW_COUNT_1), RoundingMode.HALF_UP);

                        }
                    }
                    if (CURRENT_STRATEGY == 2) {
                        if (level.equalsIgnoreCase("high")) {
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.HIGH_COUNT_2), RoundingMode.HALF_UP);

                        } else if (level.equalsIgnoreCase("medium")) {
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.MEDIUM_COUNT_2), RoundingMode.HALF_UP);

                        } else if (level.equalsIgnoreCase("low")) {
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.LOW_COUNT_2), RoundingMode.HALF_UP);

                        }
                    }
                    if (CURRENT_STRATEGY == 3) {
                        if (level.equalsIgnoreCase("high")) {
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.HIGH_COUNT_3), RoundingMode.HALF_UP);

                        } else if (level.equalsIgnoreCase("medium")) {
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.MEDIUM_COUNT_3), RoundingMode.HALF_UP);

                        } else if (level.equalsIgnoreCase("low")) {
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.LOW_COUNT_3), RoundingMode.HALF_UP);

                        }
                    }


                    if (usdtBalance.compareTo(usdtPortion) >= 0) {
                        setInsufficientFound(false);
                        StrategyCommon.placeBuyOrder(spot, priceList.get(i.get()), usdtPortion);
                    } else {
                        setInsufficientFound(true);
                        ticker.getAndAdd(1);
                        if (ticker.get() % 10 == 0) {
                            ticker.getAndSet(1);
                            logger.info("====== {}-SpotBuyer-priceListener: 所剩 usdt 余额不足,等待卖单成交 {} ======", SYMBOL, usdtBalance.toString());
                            usdtBalance = usdtBalance.max(HuobiUtil.getBalanceByAccountId(spotAccountId, spot.getBaseCurrency(), spot.getQuoteCurrency()));
                        }
                    }
                }

            }
        } catch (SDKException e) {
            logger.error("====== {}-SpotBuyer-priceListener: {} ======", SYMBOL, e.getMessage());
        }
    }

    public static void setInsufficientFound(boolean flag) {
        insufficientFound = flag;
    }

}
