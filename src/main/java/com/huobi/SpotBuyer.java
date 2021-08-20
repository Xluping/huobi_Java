package com.huobi;

import com.huobi.constant.Constants;
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
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 8/19/21 1:28 PM
 */
public class SpotBuyer implements Job {
    private final BigDecimal alertPointBalance = new BigDecimal("50");
    private final static Spot spot = new Spot();
    private final static StrategyTogether strategyTogether = new StrategyTogether();
    private final AtomicInteger orderCount = new AtomicInteger(0);
    private static String symbol = "htusdt";

    Logger logger = LoggerFactory.getLogger(SpotBuyer.class);
    private Long spotAccountId = 14086863L;
    private Long pointAccountId = 14424186L;
    private boolean alertSend = false;
    private String currentStrategy = "high";
    private int sendCount = 0;

    public static void main(String[] args) {
        SpotBuyer spotBuyer = new SpotBuyer();
        spotBuyer.startUp();

        StrategyCommon.timer("0/5 * * * * ?", SpotBuyer.class, symbol); // 4s 执行一次
    }


    /**
     * 设置基本参数
     */
    public void startUp() {
        Scanner sc = new Scanner(System.in);
        System.out.println("现货币种 spot currency: (BTC-USDT)");
        String spotInputStr = sc.next();
        String[] currencys = spotInputStr.split("-");
        if (currencys.length == 2) {
            spot.setBaseCurrency(currencys[0]);
            spot.setQuoteCurrency(currencys[1]);
            symbol = currencys[0] + currencys[1];
        } else {//默认 USDT 交易对
            spot.setBaseCurrency(spotInputStr);
            spot.setQuoteCurrency("usdt");
            symbol = spotInputStr + "usdt";
        }
        spot.setSymbol(symbol);


        spotAccountId = HuobiUtil.getAccountIdByType("spot");
        pointAccountId = HuobiUtil.getAccountIdByType("point");
        spot.setAccountId(spotAccountId);

        HuobiUtil.getBalanceByAccountId(spotAccountId, spot.getBaseCurrency(), spot.getQuoteCurrency());
        System.out.println("分配到" + spotInputStr + "总仓位: ? " + spot.getQuoteCurrency() + " - 不能大于现有仓位,风险过高.");
        double totalBalance = sc.nextDouble();
        spot.setTotalBalance(totalBalance);

        double highBalance = totalBalance * Constants.HIGH_RATIO;
        spot.setHighStrategyBalance(highBalance);
        double mediumBalance = totalBalance * Constants.MEDIUM_RATIO;
        spot.setMediumStrategyBalance(mediumBalance);
        double lowBalance = totalBalance * Constants.LOW_RATIO;
        spot.setLowStrategyBalance(lowBalance);
        logger.error("分配到-高频-的仓位: " + highBalance + spot.getQuoteCurrency());
        logger.error("分配到-稳健-的仓位: " + mediumBalance + spot.getQuoteCurrency());
        logger.error("分配到-保守-的仓位: " + lowBalance + spot.getQuoteCurrency());
        HuobiUtil.setBaseInfo(spot);
        strategyTogether.setSpot(spot);
        strategyTogether.launch();

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
            BigDecimal latestPrice = HuobiUtil.getCurrentTradPrice(symbol);
            // 点卡
            BigDecimal pointBalance = HuobiUtil.getBalanceByAccountId(pointAccountId);
            if (!alertSend && pointBalance.compareTo(alertPointBalance) < 0) {
                logger.error("====== 点卡余额: " + pointBalance.toString() + " ======");
                HuobiUtil.weChatPusher("点卡余额不足,需要充值. " + pointBalance.toString(), 1);
                alertSend = true;

            }
            if (alertSend && pointBalance.compareTo(alertPointBalance) > 0) {
                alertSend = false;
                logger.error("====== 点卡余额: " + pointBalance.toString() + " ======");
            }
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
                    logger.error("====== 买单已成交 : " + buyOrder.toString() + " ======");
                    BigDecimal cost = buyAmount.multiply(buyPrice);
                    StrategyCommon.setFee(cost);
                    StrategyCommon.setFee(buyOrder.getFilledFees());
                    StrategyCommon.placeSellOrder(spot, new BigDecimal(String.valueOf(buyOrder.getPrice())), buyAmount);
                    orderCount.getAndIncrement();
                    buyIterator.remove();
                } else if (buyOrder.getState().trim().equalsIgnoreCase("canceled")) {
                    logger.error("====== 买单已取消 : " + buyOrder.toString() + " ======");
                    orderCount.getAndDecrement();
                    buyIterator.remove();
                } else if (buyOrder.getState().trim().equalsIgnoreCase("submitted") && StrategyCommon.bigDiff(latestPrice, buyPrice)) {
                    int code = HuobiUtil.cancelOrder(clientId);
                    if (code == 7) { //取消成功
                        orderCount.getAndDecrement();
                        buyIterator.remove();
                    }
                }
            }

            while (sellIterator.hasNext()) {
                Map.Entry<Long, BigDecimal> entry = sellIterator.next();
                Long orderId = entry.getKey();
                Order sellOrder = HuobiUtil.getOrderByOrderId(orderId);

                if (sellOrder.getState().trim().equalsIgnoreCase("filled")) {
                    logger.error("====== 卖单已成交 : " + sellOrder.toString() + " ======");
                    logger.info(sellOrder.toString());
                    BigDecimal sellPrice = sellOrder.getPrice();
                    BigDecimal sellAmount = sellOrder.getAmount();
                    BigDecimal gain = sellPrice.multiply(sellAmount);
                    StrategyCommon.setProfit(gain);
                    StrategyCommon.setFee(sellOrder.getFilledFees());
                    orderCount.getAndDecrement();
                    sellIterator.remove();
                } else if (sellOrder.getState().trim().equalsIgnoreCase("canceled")) {
                    logger.error("====== 卖单已取消 : " + sellOrder.toString() + " ======");
                    sellIterator.remove();
                }

            }
            //本轮买单已全部卖出. 重启应用
            if (sellOrderMap.size() == 0) {
                BigDecimal pureProfit = StrategyCommon.getProfit().subtract(StrategyCommon.getFee());
                pureProfit = pureProfit.setScale(2, RoundingMode.HALF_UP);
                logger.error("====== 最新收益: " + pureProfit.toString() + " ======");
                HuobiUtil.weChatPusher("最新收益: " + pureProfit.toString(), 2);
                orderCount.getAndSet(0);

                strategyTogether.launch();

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
                logger.error("====== 当前策略: " + currentStrategy + " ======");

            }
            // TODO 3:46 AM  :  canOpen() && 下单次数, 总仓位
            //之前买单全部成交后, 才考虑下单.

            if (orderCount.get() == i && buyOrderMap.size() == 0) {
                if (i < priceList.size()) {
                    double usdtAmount = 10.0;
                    switch (currentStrategy) {
                        case "high":
                            usdtAmount = spot.getHighStrategyBalance() / Constants.HIGH_COUNT;
                            break;
                        case "medium":
                            usdtAmount = spot.getMediumStrategyBalance() / Constants.MEDIUM_COUNT;
                            break;
                        case "low":
                            usdtAmount = spot.getLowStrategyBalance() / Constants.LOW_COUNT;
                            break;

                    }
                    StrategyCommon.placeBuyOrder(spot, priceList.get(i), new BigDecimal(usdtAmount));
                }

            }
        } catch (SDKException e) {
            logger.error("====== priceListener: " + e.getMessage() + "======");
            if (e.getMessage().contains("insufficient")) {
                sendCount++;
                if (sendCount == 50) {
                    HuobiUtil.weChatPusher("账户余额不足!!", 2);
                    sendCount = 0;
                }
            }
        }
    }
}
