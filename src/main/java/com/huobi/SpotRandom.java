package com.huobi;

import com.huobi.client.req.trade.SubOrderUpdateV2Request;
import com.huobi.constant.enums.CandlestickIntervalEnum;
import com.huobi.model.trade.Order;
import com.huobi.model.trade.OrderUpdateV2;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
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

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 9/14/21 5:38 PM
 */
public class SpotRandom implements Job {

    private static final int CURRENT_STRATEGY = 1;// 决定了止盈百分比
    private static final CandlestickIntervalEnum candlestickIntervalEnum = CandlestickIntervalEnum.MIN60; //按照30分钟周期
    private static final int numberOfCandlestick = 6; // 按照过去4个蜡烛图来筛选symbol
    private static final String QUOTE_CURRENCY = "usdt";
    private static final int HOLD_SIZE = 3; // 允许在3个symbol 没有卖出的情况下,可以重启

    private static ConcurrentHashMap<String, Spot> finalSymbolMap = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger(SpotRandom.class);
    private static volatile boolean balanceChanged = false;
    private static volatile BigDecimal totalBalance;
    private static final ArrayList<String> symbolList;
    private static Long spotAccountId;


    static {
        // 第一次过滤, 得到 symbol string , like "btcusdt"
        symbolList = StrategyCommon.getSymbolByConditions(QUOTE_CURRENCY);
    }


    public static void main(String[] args) {

        orderListener();
        SpotRandom spotRandom = new SpotRandom();
        spotRandom.launch();
        JobManagement jobManagement = new JobManagement();
        jobManagement.addJob("0/35 * * * *  ?", SpotRandom.class, "SpotRandom");
        // 时间设置上避开整点,
        jobManagement.addJob("0 10 0/1 * *  ?", SpotFilter.class, "spotFilter");
        jobManagement.startJob();

    }

    public static void launch() {

        StopWatch clock = new StopWatch();
        clock.start(); // 计时开始
        spotAccountId = StrategyCommon.getAccountIdByType("spot");

        finalSymbolMap = SpotFilter.filter();
        log.error("====== SpotRandom.launch-{}: 按照 {} 个 {}  筛选出 {} 个symbol======", CURRENT_STRATEGY, numberOfCandlestick, candlestickIntervalEnum.getCode(), finalSymbolMap.size());

        // 为每个symbol 均分 等额的 usdt
//        totalBalance = new BigDecimal("10000");
        totalBalance = StrategyCommon.getQuotaBalanceByAccountId(spotAccountId, QUOTE_CURRENCY);
        log.info("====== SpotRandom-launch-{}: 当前账户余额: {} ======", CURRENT_STRATEGY, totalBalance);
        try {
            doBuy(totalBalance, finalSymbolMap);
        } catch (Exception e) {
            log.error("======SpotRandom.launch : {} ======", e.getMessage());

        }
        // 部分symbol 成交了
        if (StrategyCommon.getBuyOrderMap().size() != 0 && StrategyCommon.getBuyOrderMap().size() < finalSymbolMap.size()) {
            log.info("====== SpotRandom-launch: 部分买单成交 ======");
        }
        clock.stop();
        long executeTime = clock.getTime();
        log.info("====== SpotRandom.launch-{}: executeTime {} ms ======", CURRENT_STRATEGY, executeTime);

    }

    public static void doBuy(BigDecimal totalBalance, ConcurrentHashMap<String, Spot> finalSymbolMap) {
        try {
            BigDecimal portion = totalBalance.divide(new BigDecimal(finalSymbolMap.size()), 2, RoundingMode.HALF_UP);
            StrategyCommon.getSymbolInfoByName(finalSymbolMap, portion);
            log.info("====== SpotRandom.launch-{}: 每个symbol分配 {} {} ======", CURRENT_STRATEGY, portion, QUOTE_CURRENCY);
            log.info("====== SpotRandom.launch-{}: 最终筛选得到 {} 个symbol ======", CURRENT_STRATEGY, finalSymbolMap.size());
            log.info("====== SpotRandom.launch-{}: 开始获取各个symbol的信息, 并按市价下单 ======", CURRENT_STRATEGY);

            for (Map.Entry<String, Spot> entry : finalSymbolMap.entrySet()) {
                // buy-market  for every symbol
                Spot spot = entry.getValue();
                BigDecimal latestPrice = StrategyCommon.getCurrentTradPrice(spot.getSymbol());
                spot.setAccountId(spotAccountId);
                spot.setStartPrice(latestPrice);
                StrategyCommon.buy(CURRENT_STRATEGY, spot, latestPrice, portion, 2);
            }
        } catch (Exception e) {
            log.error("======SpotRandom.doBuy : {} ======", e.getMessage());
        }
    }

    /**
     * websocket
     * 监听所有订单状态
     */
    public static void orderListener() {
        CurrentAPI.getApiInstance().getTradeClient().subOrderUpdateV2(SubOrderUpdateV2Request.builder().symbols("*").build(), orderUpdateV2Event -> {
//            System.out.println(" -- SpotTemplateWebsocket1.orderListener -- " + orderUpdateV2Event.toString());
            ConcurrentHashMap<String, Spot> buyOrderMap = StrategyCommon.getBuyOrderMap();
            ConcurrentHashMap<String, Spot> sellOrderMap = StrategyCommon.getSellOrderMap();
            log.info("======SpotRandom.orderListener buyOrderMap.size: {} ======", buyOrderMap.size());
            log.info("======SpotRandom.orderListener sellOrderMap.size: {} ======", sellOrderMap.size());
            if (buyOrderMap.size() == 0) {
                // maps.size 都等于0, 说明余额不足,下单失败.
                doBuy(totalBalance, finalSymbolMap);
            }

            OrderUpdateV2 order = orderUpdateV2Event.getOrderUpdate();
            String clientOrderId = order.getClientOrderId();

            // 确保多个策略的订单不会互相影响
            if (buyOrderMap.containsKey(clientOrderId)
                    || sellOrderMap.containsKey(clientOrderId)) {
                BigDecimal orderTradePrice = order.getTradePrice();
                BigDecimal orderTradeVolume = order.getTradeVolume();
                // 已成交
                if (OrderStatusEnum.FILLED.getName().equalsIgnoreCase(order.getOrderStatus())) {
                    balanceChanged = true;
                    if (OrderTypeEnum.BUY_LIMIT.getName().equalsIgnoreCase(order.getType()) || OrderTypeEnum.BUY_MARKET.getName().equalsIgnoreCase(order.getType())) {
                        log.info("====== {}-{}-{}已成交 : price: {}, amount: {} ======", order.getSymbol(), CURRENT_STRATEGY, order.getType(), orderTradePrice, orderTradeVolume);
                        StrategyCommon.sell(CURRENT_STRATEGY, finalSymbolMap.get(order.getSymbol()), orderTradePrice, orderTradeVolume, 1);
                    } else if (OrderTypeEnum.SELL_LIMIT.getName().equalsIgnoreCase(order.getType())) {
                        StrategyCommon.setProfit(orderTradePrice.multiply(orderTradeVolume));
                        if (StringUtils.isNotBlank(clientOrderId)) {
                            sellOrderMap.remove(clientOrderId);
                        }
                    }
                } else if (OrderStatusEnum.CANCELED.getName().equalsIgnoreCase(order.getOrderStatus())) {
                    log.info("====== {}-{}-{}-已取消 : {} ======", order.getSymbol(), CURRENT_STRATEGY, order.getType(), order.toString());
                    if (OrderTypeEnum.BUY_LIMIT.getName().equalsIgnoreCase(order.getType())) {
                        balanceChanged = true;
                    } else if (OrderTypeEnum.SELL_LIMIT.getName().equalsIgnoreCase(order.getType()) || OrderTypeEnum.SELL_MARKET.getName().equalsIgnoreCase(order.getType())) {
                        //有时候会取消订单,然后手动挂卖单
                        if (StringUtils.isNotBlank(clientOrderId)) {
                            sellOrderMap.remove(clientOrderId);
                        }
                        log.info("====== SpotRandom-orderListener-{}: 现有卖单 {} 个 ======", CURRENT_STRATEGY, sellOrderMap.size());

                    }

                }


            } else {
                //其他卖单成交了 或其他买单取消了, 更新余额
                if (OrderStatusEnum.FILLED.getName().equalsIgnoreCase(order.getOrderStatus()) && (OrderTypeEnum.BUY_LIMIT.getName().equalsIgnoreCase(order.getType()) || OrderTypeEnum.BUY_MARKET.getName().equalsIgnoreCase(order.getType()))
                ) {
                    balanceChanged = true;
                }
                if (OrderStatusEnum.CANCELED.getName().equalsIgnoreCase(order.getOrderStatus()) && (OrderTypeEnum.SELL_LIMIT.getName().equalsIgnoreCase(order.getType()) || OrderTypeEnum.SELL_MARKET.getName().equalsIgnoreCase(order.getType()))
                ) {
                    balanceChanged = true;
                }


            }

        });

    }

    /**
     * 定时任务处理之前的买单,卖单, 防止 websocket 断掉,买/卖单 没有及时更新
     */
    public static void checkOrderStatus() {


        if (balanceChanged) {
            totalBalance = StrategyCommon.getQuotaBalanceByAccountId(spotAccountId, QUOTE_CURRENCY);
        }
        ConcurrentHashMap<String, Spot> buyOrderMap = StrategyCommon.getBuyOrderMap();
        ConcurrentHashMap<String, Spot> sellOrderMap = StrategyCommon.getSellOrderMap();
        log.info("======SpotRandom.checkOrderStatus buyOrderMap.size: {} ======", buyOrderMap.size());
        log.info("======SpotRandom.checkOrderStatus sellOrderMap.size: {} ======", sellOrderMap.size());
        if (buyOrderMap.size() == 0) {
            // maps.size 都等于0, 说明余额不足,下单失败.
            doBuy(totalBalance, finalSymbolMap);
        }

        Iterator<ConcurrentHashMap.Entry<String, Spot>> buyIterator = buyOrderMap.entrySet().iterator();
        Iterator<ConcurrentHashMap.Entry<String, Spot>> sellIterator = sellOrderMap.entrySet().iterator();
        while (buyIterator.hasNext()) {
            Map.Entry<String, Spot> entry = buyIterator.next();
            String clientOrderId = entry.getKey();
            Spot spot = entry.getValue();
            Order buyOrder = StrategyCommon.getOrderByClientId(clientOrderId);
            if (buyOrder != null) {

                if (OrderStatusEnum.FILLED.getName().equalsIgnoreCase(buyOrder.getState().trim())) {
                    log.info("====== {}-checkOrderStatus-{} 买单已成交 : {} ======", CURRENT_STRATEGY, clientOrderId, buyOrder.toString());
                    BigDecimal buyAmount = buyOrder.getFilledAmount();
                    // TODO xlp 9/7/21 11:01 AM  : 市场价下单时, Order 里 price =0
                    StrategyCommon.sell(CURRENT_STRATEGY, spot, spot.getStartPrice(), buyAmount, 1);
                } else if (OrderStatusEnum.CANCELED.getName().equalsIgnoreCase(buyOrder.getState().trim())) {
                    log.info("====== {}-{}-checkOrderStatus-买单已取消 : {} ======", spot.getSymbol(), CURRENT_STRATEGY, buyOrder.toString());
                }
            }

        }

        while (sellIterator.hasNext()) {
            Map.Entry<String, Spot> entry = sellIterator.next();
            String orderId = entry.getKey();
            Spot spot = entry.getValue();
            Order sellOrder = StrategyCommon.getOrderByClientId(orderId);
            if (sellOrder != null) {
                if (OrderStatusEnum.FILLED.getName().equalsIgnoreCase(sellOrder.getState().trim())) {
                    StrategyCommon.setProfit(sellOrder.getFilledAmount().multiply(sellOrder.getPrice()));
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
        // 允许接收3个订单没有卖出;&& 原订单数 > 3
        if (sellOrderMap.size() <= HOLD_SIZE && buyOrderMap.size() > HOLD_SIZE) {
            buyOrderMap.clear();
            log.info("====== SpotRandom-checkOrderStatus: 清空 buyOrderMap ======");
            log.info("====== SpotRandom-checkOrderStatus: sellOrderMap.size: {} ======", sellOrderMap.size());
            if (sellOrderMap.size() == 0) {
                log.info("====== SpotRandom-checkOrderStatus: 卖单已全部成交 ======");

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
        return numberOfCandlestick;
    }
}
