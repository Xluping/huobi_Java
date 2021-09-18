package com.huobi;

import com.huobi.constant.enums.CandlestickIntervalEnum;
import com.huobi.model.trade.Order;
import org.apache.commons.lang.time.StopWatch;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 9/14/21 5:38 PM
 */
public class SpotRandom implements Job {
    /**
     * api key/ secret key
     */
    // TODO: 9/17/21 测试用100  正式API_CODE = 0 , API_CODE 主要用在方法参数上
    private static final int API_CODE = 0;
    /**
     * 用在log上,以及sell()
     * 1: 2%
     * 2: 5%
     * 3: 10%
     */
    private static final int CURRENT_STRATEGY = 2;
    /**
     * 蜡烛图周期
     */
    private static final CandlestickIntervalEnum candlestickIntervalEnum = CandlestickIntervalEnum.MIN60;
    /**
     * 按照过去4个蜡烛图来筛选symbol
     */
    private static final int NUMBER_OF_CANDLESTICK = 6;
    /**
     * 允许在3个symbol 没有卖出的情况下,可以重启
     */
    private static int hold_size = 0;
    private static final String QUOTE_CURRENCY = "usdt";


    private static ConcurrentHashMap<String, Spot> finalSymbolMap = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(SpotRandom.class);
    private static volatile boolean balanceChanged = false;
    private static volatile BigDecimal totalBalance;
    private static final ArrayList<String> symbolList;
    private static Long spotAccountId;
    private static final ConcurrentHashMap<String, Long> orderHistory = new ConcurrentHashMap<>();


    static {
        // 第一次过滤, 得到 symbol string , like "btcusdt"
        symbolList = StrategyCommon.getSymbolByConditions(API_CODE, QUOTE_CURRENCY);
    }


    public static void main(String[] args) {
        launch();
        JobManagement jobManagement = new JobManagement();
        jobManagement.addJob("0/35 * * * *  ?", SpotRandom.class, "SpotRandom");
        // 时间设置上避开整点,
        jobManagement.addJob("0 0 0/1 * *  ?", SpotFilter.class, "spotFilter");
        jobManagement.startJob();

    }

    public static void launch() {
        log.error("====== SpotRandom.launch-{} : 策略启动 ======", CURRENT_STRATEGY);
        StopWatch clock = new StopWatch();
        clock.start(); // 计时开始
        spotAccountId = StrategyCommon.getAccountIdByType(API_CODE, "spot");
        finalSymbolMap = SpotFilter.filter(API_CODE);
        log.error("====== SpotRandom.launch-{} : 按照 {} 个 {}  筛选出 {} 个symbol======", CURRENT_STRATEGY, NUMBER_OF_CANDLESTICK, candlestickIntervalEnum.getCode(), finalSymbolMap.size());

        // 为每个symbol 均分 等额的 usdt
//        totalBalance = new BigDecimal("10000");
        try {
            if (finalSymbolMap.size() > 0) {
                hold_size = finalSymbolMap.size() >> 2;
                doBuy(finalSymbolMap);
            } else {
                log.info("====== SpotRandom-launch-{} : finalSymbolMap.size=0,没有符合条件的symbol ======", CURRENT_STRATEGY);
            }
        } catch (Exception e) {
            log.error("======SpotRandom.launch-{} : {} ======", CURRENT_STRATEGY, e.getMessage());

        }
        // 部分symbol 成交了
        if (StrategyCommon.getBuyOrderMap().size() != 0 && StrategyCommon.getBuyOrderMap().size() < finalSymbolMap.size()) {
            log.info("====== SpotRandom-launch-{} : 部分买单成交 ======", CURRENT_STRATEGY);
        }
        clock.stop();
        long executeTime = clock.getTime();
        log.info("====== SpotRandom.launch-{} : executeTime {} ms ======", CURRENT_STRATEGY, executeTime);

    }

    public static void doBuy(ConcurrentHashMap<String, Spot> finalSymbolMap) {
        try {
            totalBalance = StrategyCommon.getQuotaBalanceByAccountId(API_CODE, spotAccountId, QUOTE_CURRENCY);
            log.info("====================================================================");
            log.info("====== SpotRandom-launch-{} : 当前账户余额: {} ======", CURRENT_STRATEGY, totalBalance);
            BigDecimal portion = totalBalance.divide(new BigDecimal(finalSymbolMap.size()), 2, RoundingMode.HALF_UP);
            StrategyCommon.getSymbolInfoByName(API_CODE, finalSymbolMap, portion);
            log.info("====== SpotRandom.launch-{} : 每个symbol分配 {} {} ======", CURRENT_STRATEGY, portion, QUOTE_CURRENCY);
            log.info("====== SpotRandom.launch-{} : 最终筛选得到 {} 个symbol ======", CURRENT_STRATEGY, finalSymbolMap.size());
            log.info("====== SpotRandom.launch-{} : 开始获取各个symbol的信息, 并按市价下单 ======", CURRENT_STRATEGY);
            log.info("====================================================================");

            for (Map.Entry<String, Spot> entry : finalSymbolMap.entrySet()) {
                // buy-market  for every symbol
                Spot spot = entry.getValue();
                BigDecimal latestPrice = StrategyCommon.getCurrentTradPrice(API_CODE, spot.getSymbol());
                spot.setAccountId(spotAccountId);
                spot.setStartPrice(latestPrice);
                StrategyCommon.buy(API_CODE, spot, latestPrice, portion, 2);
            }
        } catch (Exception e) {
            log.error("======SpotRandom.doBuy-{} : {} ======", CURRENT_STRATEGY, e.getMessage());
        }
    }


    /**
     * 定时任务处理之前的买单,卖单, 防止 websocket 断掉,买/卖单 没有及时更新
     */
    public static void checkOrderStatus() {

        ConcurrentHashMap<String, Spot> buyOrderMap = StrategyCommon.getBuyOrderMap();
        ConcurrentHashMap<String, Spot> sellOrderMap = StrategyCommon.getSellOrderMap();
        log.info("====== SpotRandom.checkOrderStatus-{} : buyOrderMap.size: {} ======", CURRENT_STRATEGY, buyOrderMap.size());
        log.info("====== SpotRandom.checkOrderStatus-{} : sellOrderMap.size: {} ======", CURRENT_STRATEGY, sellOrderMap.size());
        int sellSize = sellOrderMap.size();
        Iterator<ConcurrentHashMap.Entry<String, Spot>> buyIterator = buyOrderMap.entrySet().iterator();
        Iterator<ConcurrentHashMap.Entry<String, Spot>> sellIterator = sellOrderMap.entrySet().iterator();
        while (buyIterator.hasNext()) {
            Map.Entry<String, Spot> entry = buyIterator.next();
            String clientOrderId = entry.getKey();
            Spot spot = entry.getValue();
            Order buyOrder = StrategyCommon.getOrderByClientId(API_CODE, clientOrderId);
            if (buyOrder != null) {
                balanceChanged = true;
                if (OrderStatusEnum.FILLED.getName().equalsIgnoreCase(buyOrder.getState().trim())) {
                    log.info("====== checkOrderStatus-{} 买单已成交 : {} ======", CURRENT_STRATEGY, buyOrder.toString());
                    BigDecimal buyAmount = buyOrder.getFilledAmount();
                    buyIterator.remove();
                    StrategyCommon.setFee(buyOrder.getFilledCashAmount());
                    orderHistory.put(buyOrder.getSymbol(), System.currentTimeMillis());
                    StrategyCommon.sell(API_CODE, spot, spot.getStartPrice(), buyAmount, 1);
                } else if (OrderStatusEnum.CANCELED.getName().equalsIgnoreCase(buyOrder.getState().trim())) {
                    log.info("====== {}-{}-checkOrderStatus-买单已取消 : {} ======", spot.getSymbol(), CURRENT_STRATEGY, buyOrder.toString());
                }
            } else {
                buyIterator.remove();
            }

        }

        while (sellIterator.hasNext()) {
            Map.Entry<String, Spot> entry = sellIterator.next();
            String orderId = entry.getKey();
            Spot spot = entry.getValue();
            Order sellOrder = StrategyCommon.getOrderByClientId(API_CODE, orderId);
            if (sellOrder != null) {
                if (OrderStatusEnum.FILLED.getName().equalsIgnoreCase(sellOrder.getState().trim())) {
                    balanceChanged = true;
                    StrategyCommon.setProfit(sellOrder.getFilledCashAmount());
                    log.info("====== {}-{}-checkOrderStatus-卖单已成交 : {} ======", spot.getSymbol(), CURRENT_STRATEGY, sellOrder.toString());
                    sellIterator.remove();
                } else if (OrderStatusEnum.CANCELED.getName().equalsIgnoreCase(sellOrder.getState().trim())) {
                    log.info("====== {}-{}-checkOrderStatus-卖单已取消 : {} ======", spot.getSymbol(), CURRENT_STRATEGY, sellOrder.toString());
                    sellIterator.remove();
                }
            } else {
                sellIterator.remove();
            }
        }

        if (balanceChanged) {
            totalBalance = StrategyCommon.getQuotaBalanceByAccountId(API_CODE, spotAccountId, QUOTE_CURRENCY);
            balanceChanged = false;
        }

        // 允许接收3个订单没有卖出;&& 原订单数 > 3
        if (sellOrderMap.size() <= hold_size) {
            log.info("====== SpotRandom-checkOrderStatus-{} : sellOrderMap.size: {}, 可以重新启动 ======", CURRENT_STRATEGY, sellOrderMap.size());
            if (sellSize != 0 && sellOrderMap.size() == 0) {
                log.info("====== SpotRandom-checkOrderStatus-{} : sellOrderMap.size:{} <= hold_size:{} ======", CURRENT_STRATEGY, sellOrderMap.size(), hold_size);
                BigDecimal pureProfit = StrategyCommon.getProfit().subtract(StrategyCommon.getFee());
                pureProfit = pureProfit.setScale(2, RoundingMode.HALF_DOWN);
                if (pureProfit.compareTo(BigDecimal.ZERO) > 0) {
                    String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                    String sb = time + " : " + pureProfit;
                    StrategyCommon.saveToFile(sb);
                    StrategyCommon.resetFeeAndProfit(CURRENT_STRATEGY);
                    log.error("====== SpotRandom.checkOrderStatus-{} profit: {} ======", CURRENT_STRATEGY, pureProfit);
                }
            }
            launch();
        }

    }

    @Override
    public void execute(JobExecutionContext context) {
        checkOrderStatus();
    }


    public static ArrayList<String> getSymbolList() {
        return symbolList;
    }

    public static CandlestickIntervalEnum getCandlestickIntervalEnum() {
        return candlestickIntervalEnum;
    }

    public static int getNumberOfCandlestick() {
        return NUMBER_OF_CANDLESTICK;
    }

    // TODO xlp 9/16/21 4:21 PM  :  持久化存储 orderHistory
    public static ConcurrentHashMap<String, Long> getOrderHistory() {
        return orderHistory;
    }
}
