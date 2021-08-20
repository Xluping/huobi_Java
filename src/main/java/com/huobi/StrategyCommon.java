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

    public static synchronized ArrayList<BigDecimal> calculateBuyPriceList(BigDecimal latestPrice, int scale) {
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
     * <p>
     * 根据 usdt 计算买入的币的数量
     *
     * @param spot
     * @param buyPrice
     * @param usdt     计价币种数量
     */
    public static synchronized void placeBuyOrder(Spot spot, BigDecimal buyPrice, BigDecimal usdt) {

        BigDecimal orderValue = new BigDecimal("0");
        //最小下单金额
        if (usdt.compareTo(spot.getMinOrderValue()) < 0) {
            orderValue = orderValue.add(spot.getMinOrderValue());
        } else {
            orderValue = orderValue.add(usdt);
        }

        BigDecimal coinAmount = orderValue.divide(buyPrice, RoundingMode.HALF_UP);
        //自定义订单号
        String clientOrderId = spot.getSymbol() + System.nanoTime();
        // 价格,币数 有严格的小数位限制
        buyPrice = buyPrice.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
        coinAmount = coinAmount.setScale(spot.getAmountPrecision(), RoundingMode.HALF_DOWN);

        BigDecimal orderAmount = new BigDecimal("0");
        //最小下单量限制
        if (coinAmount.compareTo(spot.getLimitOrderMinOrderAmt()) < 0) {
            orderAmount = orderAmount.add(spot.getLimitOrderMinOrderAmt());
        } else {
            orderAmount = orderAmount.add(coinAmount);
        }

        CreateOrderRequest buyLimitRequest = CreateOrderRequest.spotBuyLimit(spot.getAccountId(), clientOrderId, spot.getSymbol(), buyPrice, orderAmount);
        CurrentAPI.getApiInstance().getTradeClient().createOrder(buyLimitRequest);
        buyOrderMap.putIfAbsent(clientOrderId, orderAmount);
        logger.error("====== BUY at:" + buyPrice.toString() + ", clientOrderId : " + clientOrderId + "  ======");


    }

    /**
     * 计算卖单价格, 并挂单.
     * sell-limit
     *
     * @param spot
     * @param buyPrice
     * @param coinAmount
     */
    public static synchronized void placeSellOrder(Spot spot, BigDecimal buyPrice, BigDecimal coinAmount) {
        // 计算卖出价格 buyPrice * (1+offset);
        BigDecimal sellPrice = buyPrice.multiply(Constants.SELL_OFFSET);
        // 价格,币数 有严格的小数位限制
        sellPrice = sellPrice.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
        coinAmount = coinAmount.setScale(spot.getAmountPrecision(), RoundingMode.HALF_DOWN);
        //最小下单量限制
        BigDecimal orderAmount = new BigDecimal("0");
        //最小下单量限制
        if (coinAmount.compareTo(spot.getLimitOrderMinOrderAmt()) < 0) {
            orderAmount = orderAmount.add(spot.getLimitOrderMinOrderAmt());
        } else {
            orderAmount = orderAmount.add(coinAmount);
        }

        CreateOrderRequest sellLimitRequest = CreateOrderRequest.spotSellLimit(spot.getAccountId(), spot.getSymbol(), sellPrice, orderAmount);
        Long orderId = CurrentAPI.getApiInstance().getTradeClient().createOrder(sellLimitRequest);
        sellOrderMap.putIfAbsent(orderId, orderAmount);
        logger.error("====== SELL at:" + sellPrice.toString() + ", orderId : " + orderId + "  ======");


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
    public static void timer(String time, Class<? extends Job> jobClass, String jobDetailsKey) {
        try {
            // 获取到一个StdScheduler, StdScheduler其实是QuartzScheduler的一个代理
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

            // 启动Scheduler
            scheduler.start();
            // 新建一个Job, 指定执行类是QuartzTest(需实现Job), 指定一个K/V类型的数据, 指定job的name和group
            JobDetail job = newJob(jobClass)
                    .usingJobData(jobDetailsKey, jobDetailsKey + "_quant")
                    .withIdentity(jobDetailsKey + "_group", jobDetailsKey + "_timer")
                    .build();
            // 新建一个Trigger, 表示JobDetail的调度计划, 这里的cron表达式是 每10秒执行一次
            CronTrigger trigger = newTrigger()
                    .withIdentity(jobDetailsKey + "_trigger", jobDetailsKey + "_timer")
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
