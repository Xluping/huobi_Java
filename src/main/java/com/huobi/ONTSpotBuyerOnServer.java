package com.huobi;

import com.huobi.constant.Constants;
import com.huobi.constant.enums.OrderSideEnum;
import com.huobi.exception.SDKException;
import com.huobi.model.trade.Order;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 8/19/21 1:28 PM
 */
public class ONTSpotBuyerOnServer implements Job {
    private static final String SYMBOL = "ontusdt";
    private static volatile boolean insufficientFound = true;
    private static volatile boolean balanceChanged = false;
    private final BigDecimal alertPointBalance = new BigDecimal("50");
    private Long spotAccountId = 14086863L;
    private Long pointAccountId = 14424186L;
    private final boolean alertSend = false;
    private String currentStrategy = "high";
    private final static Spot spot = new Spot();
    private final static StrategyTogether strategyTogether = new StrategyTogether();
    private final AtomicInteger orderCount = new AtomicInteger(0);
    private static BigDecimal usdtBalance = new BigDecimal("0");

    Logger logger = LoggerFactory.getLogger(AAVESpotBuyerOnServer.class);

    public static void main(String[] args) {
        ONTSpotBuyerOnServer spotBuyer = new ONTSpotBuyerOnServer();
        spotBuyer.startUp();

        StrategyCommon.timer("0/5 * * * * ?", ONTSpotBuyerOnServer.class, SYMBOL); // 4s 执行一次
    }

    /**
     * 设置基本参数
     */
    public void startUp() {
        try {
            // TODO 8:14 PM  :  服务器 start
            spot.setBaseCurrency("ont");
            spot.setQuoteCurrency("usdt");
            BigDecimal totalBalance = new BigDecimal("2000");
            // TODO 9:50 PM  : 服务器 end

            spot.setSymbol(SYMBOL);

            spotAccountId = HuobiUtil.getAccountIdByType("spot");
            pointAccountId = HuobiUtil.getAccountIdByType("point");
            spot.setAccountId(spotAccountId);

            usdtBalance = usdtBalance.add(HuobiUtil.getBalanceByAccountId(spotAccountId, spot.getBaseCurrency(), spot.getQuoteCurrency()));
            System.out.println("分配到" + SYMBOL + "总仓位: ? " + spot.getQuoteCurrency() + " - 不能大于现有仓位,风险过高.");


            spot.setTotalBalance(totalBalance);

            BigDecimal highBalance = totalBalance.multiply(new BigDecimal(Constants.HIGH_RATIO.toString()));
            highBalance = highBalance.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
            spot.setHighStrategyBalance(highBalance);
            BigDecimal mediumBalance = totalBalance.multiply(new BigDecimal(Constants.MEDIUM_RATIO.toString()));
            mediumBalance = mediumBalance.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);

            spot.setMediumStrategyBalance(mediumBalance);
            BigDecimal lowBalance = totalBalance.multiply(new BigDecimal(Constants.LOW_RATIO.toString()));
            lowBalance = lowBalance.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
            spot.setLowStrategyBalance(lowBalance);

            logger.error(SYMBOL + "SpotBuyer-分配到-高频-的仓位: " + highBalance + spot.getQuoteCurrency());
            logger.error(SYMBOL + "SpotBuyer-分配到-稳健-的仓位: " + mediumBalance + spot.getQuoteCurrency());
            logger.error(SYMBOL + "SpotBuyer-分配到-保守-的仓位: " + lowBalance + spot.getQuoteCurrency());
            HuobiUtil.setBaseInfo(spot);
            strategyTogether.setSpot(spot);
            HuobiUtil.cancelOpenOrders(spotAccountId, SYMBOL, OrderSideEnum.BUY);
            strategyTogether.launch(usdtBalance);
        } catch (Exception exception) {
            logger.error("====== " + SYMBOL + "SpotBuyer-startup: " + exception.getMessage() + "======");
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
            // 点卡
//            BigDecimal pointBalance = HuobiUtil.getBalanceByAccountId(pointAccountId);
//            if (!alertSend && pointBalance.compareTo(alertPointBalance) < 0) {
//                logger.error("====== SpotBuyer-点卡余额: " + pointBalance.toString() + " ======");
//                HuobiUtil.weChatPusher("点卡余额不足,需要充值. " + pointBalance.toString(), 1);
//                alertSend = true;
//
//            }
//            if (alertSend && pointBalance.compareTo(alertPointBalance) > 0) {
//                alertSend = false;
//                logger.error("====== SpotBuyer-点卡余额: " + pointBalance.toString() + " ======");
//            }

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

                    logger.error("====== " + SYMBOL + "-SpotBuyer-买单已成交 : " + buyOrder.toString() + " ======");
                    BigDecimal cost = buyAmount.multiply(buyPrice);
                    StrategyCommon.setFee(cost);
                    StrategyCommon.setFee(buyOrder.getFilledFees());
                    StrategyCommon.placeSellOrder(spot, new BigDecimal(String.valueOf(buyOrder.getPrice())), buyAmount);
                    orderCount.getAndIncrement();
                    buyIterator.remove();
                } else if (buyOrder.getState().trim().equalsIgnoreCase("canceled")) {
                    logger.error("====== " + SYMBOL + "-SpotBuyer-买单已取消 : " + buyOrder.toString() + " ======");
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

                    logger.error("====== " + SYMBOL + "-SpotBuyer-卖单已成交 : " + sellOrder.toString() + " ======");
                    logger.info(sellOrder.toString());
                    BigDecimal sellPrice = sellOrder.getPrice();
                    BigDecimal sellAmount = sellOrder.getAmount();
                    BigDecimal gain = sellPrice.multiply(sellAmount);
                    StrategyCommon.setProfit(gain);
                    StrategyCommon.setFee(sellOrder.getFilledFees());
                    orderCount.getAndDecrement();
                    sellIterator.remove();
                } else if (sellOrder.getState().trim().equalsIgnoreCase("canceled")) {
                    logger.error("====== " + SYMBOL + "-SpotBuyer-卖单已取消 : " + sellOrder.toString() + " ======");
                    sellIterator.remove();
                }

            }
            //本轮买单已全部卖出. 重启应用
            if (sellOrderMap.size() == 0 && !insufficientFound) {
                logger.error("====== " + SYMBOL + "-SpotBuyer-开始清理残余买单.======");
                Iterator<Map.Entry<String, BigDecimal>> iterator = StrategyCommon.getBuyOrderMap().entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<String, BigDecimal> entry = iterator.next();
                    String clientId = entry.getKey();
                    Order remainOrder = HuobiUtil.getOrderByClientId(clientId);
                    logger.error("====== " + SYMBOL + "-SpotBuyer-正在取消订单: " + remainOrder.toString() + "======");
                    HuobiUtil.cancelOrder(clientId);
                    iterator.remove();
                }
                BigDecimal pureProfit = StrategyCommon.getProfit().subtract(StrategyCommon.getFee());
                pureProfit = pureProfit.setScale(2, RoundingMode.HALF_UP);
                HuobiUtil.weChatPusher(SYMBOL + " 最新收益: " + pureProfit.toString(), 2);

                orderCount.getAndSet(0);

                strategyTogether.launch(usdtBalance);

            }
            if (balanceChanged) { //订单成交后,更新余额
                BigDecimal currentBalance = HuobiUtil.getBalanceByAccountId(spotAccountId, spot.getBaseCurrency(), spot.getQuoteCurrency());
                usdtBalance = usdtBalance.max(currentBalance);
            }

            //检测是否需要下单
            ArrayList<BigDecimal> priceList = StrategyCommon.getPriceList();
            int i = 0;
            while (i < priceList.size() && latestPrice.compareTo(priceList.get(i)) <= 0) {
                i++;
            }
            if (i + 1 >= Constants.HIGH_COUNT) {
                currentStrategy = "medium";
                if (i + 1 >= Constants.MEDIUM_COUNT) {
                    currentStrategy = "low";
                }
                logger.error("====== " + SYMBOL + "-SpotBuyer-当前策略: " + currentStrategy + " ======");

            }
            //之前买单全部成交后, 才考虑下单.

            if (orderCount.get() == i && buyOrderMap.size() == 0) {
                if (i < priceList.size()) {
                    BigDecimal usdtPortion = new BigDecimal("10");
                    switch (currentStrategy) {
                        case "high":
                            usdtPortion = spot.getHighStrategyBalance().divide(new BigDecimal(Constants.HIGH_COUNT), RoundingMode.HALF_UP);
                            break;
                        case "medium":
                            usdtPortion = spot.getMediumStrategyBalance().divide(new BigDecimal(Constants.MEDIUM_COUNT), RoundingMode.HALF_UP);
                            break;
                        case "low":
                            usdtPortion = spot.getLowStrategyBalance().divide(new BigDecimal(Constants.LOW_COUNT), RoundingMode.HALF_UP);
                            break;

                    }
                    if (usdtBalance.compareTo(usdtPortion) >= 0) {
                        setInsufficientFound(false);
                        StrategyCommon.placeBuyOrder(spot, priceList.get(i), usdtPortion);
                    } else {
                        setInsufficientFound(true);
                        logger.error("====== " + SYMBOL + "-SpotBuyer-priceListener: 所剩 usdt 余额不足,等待卖单成交 " + usdtBalance.toString() + " ======");

                    }
                }

            }
        } catch (SDKException e) {
            logger.error("====== " + SYMBOL + "SpotBuyer-priceListener: " + e.getMessage() + "======");
        }
    }

    public static void setInsufficientFound(boolean flag) {
        insufficientFound = flag;
    }
}
