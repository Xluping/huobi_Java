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

    public static ArrayList<BigDecimal> calculateBuyPriceList(BigDecimal latestPrice, int scale) {
        priceList.clear();
        // 高频
        calculateBuyPrice(latestPrice, scale, Constants.HIGH_RANGE, Constants.HIGH_COUNT, new BigDecimal("0"));
        //稳健
        calculateBuyPrice(latestPrice, scale, Constants.MEDIUM_RANGE - Constants.HIGH_RANGE, Constants.MEDIUM_COUNT, new BigDecimal(String.valueOf(Constants.HIGH_RANGE)));
        //保守
        calculateBuyPrice(latestPrice, scale, Constants.LOW_RANGE - Constants.MEDIUM_RANGE, Constants.LOW_COUNT, new BigDecimal(String.valueOf(Constants.MEDIUM_RANGE)));

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
    public static void calculateBuyPrice(BigDecimal latestPrice, int scale, double range, double count, BigDecimal previousPercent) {
        BigDecimal pre = previousPercent.multiply(new BigDecimal("0.01"));
        BigDecimal base = new BigDecimal("1");
        double gridPercentDoubleMedium = range / count;
        BigDecimal gridPercentMedium = new BigDecimal(String.valueOf(gridPercentDoubleMedium));
        for (int i = 1; i <= count; i++) {
            BigDecimal goDown = gridPercentMedium.multiply(new BigDecimal(String.valueOf(i))).multiply(new BigDecimal("0.01"));
            BigDecimal downTo = goDown.add(pre);
            System.out.println(downTo);
            BigDecimal buyPosition = base.subtract(downTo);
            logger.info("====== buyPosition(下跌到)= " + buyPosition.toString() + " * 100% ======");
            BigDecimal buyPrice = latestPrice.multiply(buyPosition).setScale(scale, RoundingMode.DOWN);
            logger.info("====== buyPrice= " + buyPrice.toString() + " ======");
            priceList.add(buyPrice);
        }
        logger.info("==============================================================");
    }

    /**
     * 下单 buy-market
     * 保存 clientOrderId, amount 以便轮巡时 查看下单状态
     *
     * @param usdt quota-currency
     */
    public static void placeBuyOrder(Long accountId, String symbol, BigDecimal buyPrice, BigDecimal usdt, int pricePrecision, int amountPrecision) {
        BigDecimal coinAmount = usdt.divide(buyPrice, RoundingMode.HALF_UP);
        String clientOrderId = Constants.CLIENT_ID_PREFIX + System.nanoTime();
        buyPrice = buyPrice.setScale(pricePrecision, RoundingMode.HALF_UP);
        coinAmount = coinAmount.setScale(amountPrecision, RoundingMode.HALF_DOWN);
        CreateOrderRequest buyLimitRequest = CreateOrderRequest.spotBuyLimit(accountId, clientOrderId, symbol, buyPrice, coinAmount);
        CurrentAPI.getApiInstance().getTradeClient().createOrder(buyLimitRequest);
        buyOrderMap.putIfAbsent(clientOrderId, coinAmount);
        logger.error("====== 下 BUY 单 at:" + buyPrice.toString() + ", clientOrderId : " + clientOrderId + "  ======");


    }

    /**
     * 计算卖单价格, 并挂单.
     * sell-limit
     *
     * @param buyPrice
     * @param symbol
     * @param accountId
     * @param coinAmount
     */
    public static void placeSellOrder(Long accountId, String symbol, BigDecimal buyPrice, BigDecimal coinAmount, int pricePrecision, int amountPrecision) {
        // buyPrice * (1+offset);
        BigDecimal sellPrice = buyPrice.multiply(Constants.SELL_OFFSET);
        sellPrice = sellPrice.setScale(pricePrecision, RoundingMode.HALF_UP);
        logger.error("====== placeSellOrder: " + sellPrice.toString() + "======");
        coinAmount = coinAmount.setScale(amountPrecision, RoundingMode.HALF_DOWN);
        CreateOrderRequest sellLimitRequest = CreateOrderRequest.spotSellLimit(accountId, symbol, sellPrice, coinAmount);
        Long orderId = CurrentAPI.getApiInstance().getTradeClient().createOrder(sellLimitRequest);
        sellOrderMap.putIfAbsent(orderId, coinAmount);
        logger.error("====== 下 SELL 单 at:" + buyPrice.toString() + ", orderId : " + orderId + "  ======");


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

    /**
     * 计算当前价格与挂单的价格之间的差距
     * 差距过大可以取消订单
     */
    public static boolean bigDiff(BigDecimal currentPrice, BigDecimal buyPrice) {
        BigDecimal expectedSellPrice = buyPrice.multiply(Constants.SELL_OFFSET);
        return currentPrice.compareTo(expectedSellPrice) > 0;
    }

    /**
     * 定时任务
     */
    public static void timer(String time, Class<? extends Job> jobClass) {
        try {
            // 获取到一个StdScheduler, StdScheduler其实是QuartzScheduler的一个代理
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

            // 启动Scheduler
            scheduler.start();
            // 新建一个Job, 指定执行类是QuartzTest(需实现Job), 指定一个K/V类型的数据, 指定job的name和group
            JobDetail job = newJob(jobClass)
                    .usingJobData("swap_data", "test")
                    .withIdentity("swap_group", "huobi_timer")
                    .build();
            // 新建一个Trigger, 表示JobDetail的调度计划, 这里的cron表达式是 每10秒执行一次
            CronTrigger trigger = newTrigger()
                    .withIdentity("myTrigger", "huobi_timer")
                    .startNow()
                    .withSchedule(cronSchedule(time))
                    .build();


            // 让scheduler开始调度这个job, 按trigger指定的计划
            scheduler.scheduleJob(job, trigger);
            // 保持进程不被销毁
            //  scheduler.shutdown();
            Thread.sleep(10000000);

        } catch (SchedulerException | InterruptedException se) {
            se.printStackTrace();
        }
    }


}
