package com.huobi;

import com.huobi.client.req.market.CandlestickRequest;
import com.huobi.client.req.trade.SubOrderUpdateV2Request;
import com.huobi.constant.enums.CandlestickIntervalEnum;
import com.huobi.constant.enums.OrderStateEnum;
import com.huobi.model.market.Candlestick;
import com.huobi.model.trade.Order;
import com.huobi.model.trade.OrderUpdateV2;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 9/14/21 5:38 PM
 */
public class SpotRandom implements Job {
    private static int CURRENT_STRATEGY = 1;// 决定了止盈百分比
    private static CandlestickIntervalEnum candlestickIntervalEnum = CandlestickIntervalEnum.MIN60; //按照30分钟周期
    private static int numberOfCandlestick = 6; // 按照过去4个蜡烛图来筛选symbol
    private static final String QUOTE_CURRENCY = "usdt";
    private static int HOLD_SIZE = 3; // 允许在3个symbol 没有卖出的情况下,可以重启

    private static volatile boolean qualified = false;
    private static HashMap<String, Spot> finalSymbolMap;
    private static List<String> symbolList;
    private static final Logger log = LoggerFactory.getLogger(SpotRandom.class);

    static {
        finalSymbolMap = new HashMap<>();
        symbolList = StrategyCommon.getSymbolByConditions(QUOTE_CURRENCY);

    }


    public static void main(String[] args) {
        orderListener();

        SpotRandom spotRandom = new SpotRandom();
        spotRandom.launch();
        JobManagement jobManagement = new JobManagement();
        jobManagement.addJob("10 0/3 * * *  ?", SpotRandom.class, "SpotRandom");
        jobManagement.startJob();
    }

    public void launch() {
        StopWatch clock = new StopWatch();
        clock.start(); // 计时开始
        // 第一次过滤, 得到 symbol string , like "btcusdt"
        // 第二次过滤symbol, 获取每个symbol 的 closePrice 数组,
        symbolList.forEach(symbolStr -> {
            List<Candlestick> klineList = CurrentAPI.getApiInstance().getMarketClient().getCandlestick(CandlestickRequest.builder()
                    .symbol(symbolStr)
                    .interval(candlestickIntervalEnum)
                    .size(numberOfCandlestick)
                    .build());
            // 按照过去30分钟蜡烛图来看,筛选出连续4*30分钟 下跌的symbol
            StringBuilder sb = new StringBuilder();
            BigDecimal basePrice = klineList.get(0).getClose();
            sb.append(basePrice.toString()).append(" --> ");

            for (int i = 1; i < klineList.size(); i++) {
                sb.append(klineList.get(i).getClose());

                if (basePrice.compareTo(klineList.get(i).getClose()) < 0) {

                    qualified = false;
                    break;
                }
                if (i != klineList.size() - 1) {
                    sb.append(" --> ");
                }
                qualified = true;
                basePrice = klineList.get(i).getClose();
            }

            log.info("======SpotRandom.prepareSpot: symbol: {}, closePrice: {}, qualified: {} ======", symbolStr, sb.toString(), qualified);
            if (qualified) {
                finalSymbolMap.put(symbolStr, null);
            }
            qualified = false;
        });
        // reset  qualified, 重启后会用到
        qualified = false;
        log.error("======SpotRandom.prepareSpot-{}: 按照 {} 个 {}  筛选出 {} 个symbol======", CURRENT_STRATEGY, numberOfCandlestick, candlestickIntervalEnum.getCode(), finalSymbolMap.size());


        // 为每个symbol 均分 等额的 usdt
        Long spotAccountId = StrategyCommon.getAccountIdByType("spot");
//        totalBalance = new BigDecimal("10000");
        BigDecimal totalBalance = StrategyCommon.getQuotaBalanceByAccountId(spotAccountId, QUOTE_CURRENCY);
        BigDecimal portion = totalBalance.divide(new BigDecimal(finalSymbolMap.size()), 2, RoundingMode.HALF_UP);
        log.error("======SpotRandom.prepareSpot-{}: 每个symbol分配 {} {} ======", CURRENT_STRATEGY, portion, QUOTE_CURRENCY);
        log.error("======SpotRandom.prepareSpot-{}: 最终筛选得到 {} 个symbol ======", CURRENT_STRATEGY, finalSymbolMap.size());
        log.error("======SpotRandom.prepareSpot-{}: 开始获取各个symbol的信息, 并按市价下单 ======", CURRENT_STRATEGY);
        StrategyCommon.getSymbolInfoByName(finalSymbolMap, portion);
        for (Map.Entry<String, Spot> entry : finalSymbolMap.entrySet()) {
            // buy-market  for every symbol
            Spot spot = entry.getValue();
            BigDecimal latestPrice = StrategyCommon.getCurrentTradPrice(spot.getSymbol());
            spot.setStartPrice(latestPrice);
            StrategyCommon.buy(CURRENT_STRATEGY, spot, latestPrice, portion, 2);
        }
        clock.stop();
        long executeTime = clock.getTime();
        log.info("======SpotRandom.prepareSpot-{}: executeTime {} ms ======", CURRENT_STRATEGY, executeTime);
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
            OrderUpdateV2 order = orderUpdateV2Event.getOrderUpdate();
            String clientOrderId = order.getClientOrderId();

            // 确保多个策略的订单不会互相影响
            if (buyOrderMap.containsKey(clientOrderId) || sellOrderMap.containsKey(clientOrderId)) {
                // 已成交
                if (OrderStatusEnum.FILLED.getName().equalsIgnoreCase(order.getOrderStatus())) {
                    BigDecimal orderTradePrice = order.getTradePrice();
                    BigDecimal orderTradeVolume = order.getTradeVolume();
                    log.error("====== {}-{}-{}已成交 : price: {}, amount: {} ======", order.getSymbol(), CURRENT_STRATEGY, order.getType(), orderTradePrice, orderTradeVolume);
                    if (OrderTypeEnum.BUY_LIMIT.getName().equalsIgnoreCase(order.getType()) || OrderTypeEnum.BUY_MARKET.getName().equalsIgnoreCase(order.getType())) {
                        StrategyCommon.sell(CURRENT_STRATEGY, finalSymbolMap.get(order.getSymbol()), orderTradePrice, orderTradeVolume, 1);
                    } else if (OrderTypeEnum.SELL_LIMIT.getName().equalsIgnoreCase(order.getType())) {
                        StrategyCommon.setProfit(orderTradePrice.multiply(orderTradeVolume));
                        if (StringUtils.isNotBlank(clientOrderId)) {
                            sellOrderMap.remove(clientOrderId);
                        }

                    }
                } else if (OrderStatusEnum.CANCELED.getName().equalsIgnoreCase(order.getOrderStatus())) {
                    log.error("====== {}-{}-{}-已取消 : {} ======", order.getSymbol(), CURRENT_STRATEGY, order.getType(), order.toString());
                    if (OrderTypeEnum.SELL_LIMIT.getName().equalsIgnoreCase(order.getType()) || OrderTypeEnum.SELL_MARKET.getName().equalsIgnoreCase(order.getType())) {
                        //有时候会取消订单,然后手动挂卖单
                        if (StringUtils.isNotBlank(clientOrderId)) {
                            sellOrderMap.remove(clientOrderId);
                        }
                        log.error("====== SpotRandom-orderListener-{}: 现有卖单 {} 个 ======", CURRENT_STRATEGY, sellOrderMap.size());

                    }

                }


            }


        });
    }

    /**
     * 定时任务处理之前的买单,卖单, 防止 websocket 断掉,买/卖单 没有及时更新
     */
    public void checkOrderStatus() {
        ConcurrentHashMap<String, Spot> buyOrderMap = StrategyCommon.getBuyOrderMap();
        ConcurrentHashMap<String, Spot> sellOrderMap = StrategyCommon.getSellOrderMap();
        Iterator<ConcurrentHashMap.Entry<String, Spot>> buyIterator = buyOrderMap.entrySet().iterator();
        Iterator<ConcurrentHashMap.Entry<String, Spot>> sellIterator = sellOrderMap.entrySet().iterator();
        while (buyIterator.hasNext()) {
            Map.Entry<String, Spot> entry = buyIterator.next();
            String clientOrderId = entry.getKey();
            Spot spot = entry.getValue();
            Order buyOrder = StrategyCommon.getOrderByClientId(clientOrderId);
            if (buyOrder != null) {

                if (OrderStatusEnum.FILLED.getName().equalsIgnoreCase(buyOrder.getState().trim())) {
                    log.error("====== {}-checkOrderStatus-{} 买单已成交 : {} ======", CURRENT_STRATEGY, clientOrderId, buyOrder.toString());
                    BigDecimal buyAmount = buyOrder.getFilledAmount();
                    // TODO xlp 9/7/21 11:01 AM  : 市场价下单时, Order 里 price =0
                    StrategyCommon.sell(CURRENT_STRATEGY, spot, spot.getStartPrice(), buyAmount, 1);
                } else if (OrderStatusEnum.CANCELED.getName().equalsIgnoreCase(buyOrder.getState().trim())) {
                    log.error("====== {}-{}-checkOrderStatus-买单已取消 : {} ======", spot.getSymbol(), CURRENT_STRATEGY, buyOrder.toString());
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
                    log.error("====== {}-{}-checkOrderStatus-卖单已成交 : {} ======", spot.getSymbol(), CURRENT_STRATEGY, sellOrder.toString());
                    sellIterator.remove();
                } else if (OrderStatusEnum.CANCELED.getName().equalsIgnoreCase(sellOrder.getState().trim())) {
                    log.error("====== {}-{}-checkOrderStatus-卖单已取消 : {} ======", spot.getSymbol(), CURRENT_STRATEGY, sellOrder.toString());
                    sellIterator.remove();
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    log.error("====== SpotRandom-checkOrderStatus-{}: {} ======", CURRENT_STRATEGY, e.getMessage());

                }
            } else {
                sellIterator.remove();
            }
        }
        // 允许接收3个订单没有卖出;&& 原订单数 > 3
        if (sellOrderMap.size() <= HOLD_SIZE && buyOrderMap.size() > HOLD_SIZE) {
            launch();
        }

    }

    @Override
    public void execute(JobExecutionContext context) {
        checkOrderStatus();
    }


}
