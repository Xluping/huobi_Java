package com.huobi;

import com.huobi.client.req.account.AccountBalanceRequest;
import com.huobi.client.req.market.MarketTradeRequest;
import com.huobi.client.req.trade.CreateOrderRequest;
import com.huobi.client.req.trade.OpenOrdersRequest;
import com.huobi.constant.enums.OrderSideEnum;
import com.huobi.model.account.Account;
import com.huobi.model.account.AccountBalance;
import com.huobi.model.account.Balance;
import com.huobi.model.generic.Symbol;
import com.huobi.model.market.MarketTrade;
import com.huobi.model.trade.Order;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 8/19/21 2:09 PM
 */
public class StrategyCommon {
    private static final ArrayList<BigDecimal> priceList = new ArrayList<>();
    private static final ConcurrentHashMap<String, Spot> buyOrderMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Spot> sellOrderMap = new ConcurrentHashMap<>();
    private static volatile BigDecimal profit = BigDecimal.ZERO;
    private static volatile BigDecimal fee = BigDecimal.ZERO;
    private static long accountId = -1L;


    static Logger log = LoggerFactory.getLogger(StrategyCommon.class);

    @Synchronized
    public static ArrayList<BigDecimal> calculateBuyPriceList(int strategy, BigDecimal latestPrice, int scale) {
        priceList.clear();
        if (strategy == 1) {
            // 高频
            calculateBuyPrice(1, latestPrice, scale, Constants.HIGH_RANGE_1, Constants.HIGH_COUNT_1, new BigDecimal("0"));
            //稳健
            calculateBuyPrice(1, latestPrice, scale, Constants.MEDIUM_RANGE_1 - Constants.HIGH_RANGE_1, Constants.MEDIUM_COUNT_1, new BigDecimal(String.valueOf(Constants.HIGH_RANGE_1)));
            //保守
            calculateBuyPrice(1, latestPrice, scale, Constants.LOW_RANGE_1 - Constants.MEDIUM_RANGE_1, Constants.LOW_COUNT_1, new BigDecimal(String.valueOf(Constants.MEDIUM_RANGE_1)));
        } else if (strategy == 2) {
            // 高频
            calculateBuyPrice(2, latestPrice, scale, Constants.HIGH_RANGE_2, Constants.HIGH_COUNT_2, new BigDecimal("0"));
            //稳健
            calculateBuyPrice(2, latestPrice, scale, Constants.MEDIUM_RANGE_2 - Constants.HIGH_RANGE_2, Constants.MEDIUM_COUNT_2, new BigDecimal(String.valueOf(Constants.HIGH_RANGE_2)));
            //保守
            calculateBuyPrice(2, latestPrice, scale, Constants.LOW_RANGE_2 - Constants.MEDIUM_RANGE_2, Constants.LOW_COUNT_2, new BigDecimal(String.valueOf(Constants.MEDIUM_RANGE_2)));
        } else if (strategy == 3) {
            // 高频
            calculateBuyPrice(3, latestPrice, scale, Constants.HIGH_RANGE_3, Constants.HIGH_COUNT_3, new BigDecimal("0"));
            //稳健
            calculateBuyPrice(3, latestPrice, scale, Constants.MEDIUM_RANGE_3 - Constants.HIGH_RANGE_3, Constants.MEDIUM_COUNT_3, new BigDecimal(String.valueOf(Constants.HIGH_RANGE_3)));
            //保守
            calculateBuyPrice(3, latestPrice, scale, Constants.LOW_RANGE_3 - Constants.MEDIUM_RANGE_3, Constants.LOW_COUNT_3, new BigDecimal(String.valueOf(Constants.MEDIUM_RANGE_3)));
        }

        return priceList;
    }

    public static ArrayList<BigDecimal> getPriceList() {
        return priceList;
    }

    public static ConcurrentHashMap<String, Spot> getBuyOrderMap() {
        return buyOrderMap;
    }

    public static ConcurrentHashMap<String, Spot> getSellOrderMap() {
        return sellOrderMap;
    }

    /**
     * 根据参数 计算补仓点位
     */
    @Synchronized
    public static void calculateBuyPrice(int strategy, BigDecimal latestPrice, int scale, double range, double count, BigDecimal previousPercent) {
        BigDecimal pre = previousPercent.multiply(new BigDecimal("0.01"));
        BigDecimal base = new BigDecimal("1");
        double gridPercentDoubleMedium = range / count;
        BigDecimal gridPercentMedium = new BigDecimal(String.valueOf(gridPercentDoubleMedium));
        for (int i = 1; i <= count; i++) {
            BigDecimal goDown = gridPercentMedium.multiply(new BigDecimal(String.valueOf(i))).multiply(new BigDecimal("0.01"));
            BigDecimal downTo = goDown.add(pre);
            BigDecimal buyPosition = base.subtract(downTo);
            BigDecimal buyPrice = latestPrice.multiply(buyPosition).setScale(scale, RoundingMode.DOWN);
            log.error("====== {}-StrategyCommon-buyPosition: 下跌 {}% 到 {}% = {} ======", strategy, downTo.multiply(new BigDecimal("100")).toString(), buyPosition.multiply(new BigDecimal("100")).toString(), buyPrice.toString());
            priceList.add(buyPrice);
        }
        log.error("==============================================================");
    }

    /**
     * 下单 buy-market
     * 保存 clientOrderId, amount 以便轮巡时 查看下单状态
     * <p>
     * 根据 usdt 计算买入的币的数量
     *
     * @param type 1 buy-limit  2 buy-market
     */
    @Synchronized
    public static void buy(int strategy, Spot spot, BigDecimal buyPrice, BigDecimal usdt, int type) {

        //最小下单金额
        if (usdt.compareTo(spot.getMinOrderValue()) < 0) {
            log.error("====== {}-{}-buy: 所剩 usdt 余额不足,等待卖单成交  ======", spot.getSymbol(), strategy);
            return;
        }
        spot.setOrderValue(usdt);

        BigDecimal coinAmount = usdt.divide(buyPrice, RoundingMode.HALF_UP);
        //自定义订单号
        String clientOrderId = spot.getSymbol() + System.nanoTime();
        // 价格,币数 有严格的小数位限制
        buyPrice = buyPrice.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
        coinAmount = coinAmount.setScale(spot.getAmountPrecision(), RoundingMode.HALF_UP);

        //最小下单量限制
        if (coinAmount.compareTo(spot.getLimitOrderMinOrderAmt()) < 0) {
            log.error("====== {}-{}-buy: 所剩 usdt 余额不足,等待卖单成交  ======", spot.getSymbol(), strategy);
            return;

        }
        spot.setOrderAmount(coinAmount);
        CreateOrderRequest buyRequest;

        if (type == 1) {
            // buy
            log.error("====== {}-{}-StrategyCommon:  限价-BUY at: {},  clientOrderId: {}, orderAmount: {} 币 ======", spot.getSymbol(), strategy, buyPrice.toString(), clientOrderId, coinAmount);
            buyRequest = CreateOrderRequest.spotBuyLimit(spot.getAccountId(), clientOrderId, spot.getSymbol(), buyPrice, coinAmount);
        } else {
            log.error("====== {}-{}-StrategyCommon:  市价-BUY at: {},  clientOrderId: {}, orderAmount: {} USDT ======", spot.getSymbol(), strategy, buyPrice.toString(), clientOrderId, usdt);
            buyRequest = CreateOrderRequest.spotBuyMarket(spot.getAccountId(), clientOrderId, spot.getSymbol(), usdt);
        }

        CurrentAPI.getApiInstance().getTradeClient().createOrder(buyRequest);
        buyOrderMap.putIfAbsent(clientOrderId, spot);

    }

    /**
     * todo 模拟下单.
     * 保存 clientOrderId, amount 以便轮巡时 查看下单状态
     * 根据 usdt 计算买入的币的数量
     *
     * @param type 1 buy-limit  2 buy-market
     */
    public static void buySimulation(int strategy, Spot spot, BigDecimal buyPrice, BigDecimal usdt, int type) {

        //最小下单金额
        if (usdt.compareTo(spot.getMinOrderValue()) < 0) {
            log.error("====== {}-{}-buy: 所剩 usdt 余额不足,等待卖单成交  ======", spot.getSymbol(), strategy);
            return;
        }
        spot.setOrderValue(usdt);

        BigDecimal coinAmount = usdt.divide(buyPrice, RoundingMode.HALF_UP);
        //自定义订单号
        String clientOrderId = spot.getSymbol() + System.nanoTime();
        // 价格,币数 有严格的小数位限制
        buyPrice = buyPrice.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
        coinAmount = coinAmount.setScale(spot.getAmountPrecision(), RoundingMode.HALF_UP);

        //最小下单量限制
        if (coinAmount.compareTo(spot.getLimitOrderMinOrderAmt()) < 0) {
            log.error("====== {}-{}-buy: 所剩 usdt 余额不足,等待卖单成交  ======", spot.getSymbol(), strategy);
            return;

        }
        spot.setOrderAmount(coinAmount);
        CreateOrderRequest buyRequest;

        if (type == 1) {
            // buy
            log.error("====== {}-{}-StrategyCommon:  限价-BUY at: {},  clientOrderId: {}, orderAmount: {} 币 ======", spot.getSymbol(), strategy, buyPrice.toString(), clientOrderId, coinAmount);
        } else {
            log.error("====== {}-{}-StrategyCommon:  市价-BUY at: {},  clientOrderId: {}, orderAmount: {} USDT ======", spot.getSymbol(), strategy, buyPrice.toString(), clientOrderId, usdt);
        }

        buyOrderMap.putIfAbsent(clientOrderId, spot);

    }

    /**
     * 计算卖单价格, 并挂单.
     * 1:sell-limit
     * 2:sell-market
     */
    @Synchronized
    public static void sell(int currentStrategy, Spot spot, BigDecimal buyPrice, BigDecimal coinAmount, int type) {
        // 计算卖出价格 buyPrice * (1+offset);
        BigDecimal sellPrice = null;
        if (currentStrategy == 1) {
            sellPrice = buyPrice.multiply(Constants.SELL_OFFSET_1);
        } else if (currentStrategy == 2) {
            sellPrice = buyPrice.multiply(Constants.SELL_OFFSET_2);
        } else if (currentStrategy == 3) {
            sellPrice = buyPrice.multiply(Constants.SELL_OFFSET_3);
        }
        //自定义订单号
        String clientOrderId = spot.getSymbol() + System.nanoTime();
        // 价格,币数 有严格的小数位限制
        assert sellPrice != null;
        sellPrice = sellPrice.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
        coinAmount = coinAmount.setScale(spot.getAmountPrecision(), RoundingMode.HALF_DOWN);
        //最小下单量限制
        BigDecimal orderAmount;
        //最小下单量限制
        if (coinAmount.compareTo(spot.getLimitOrderMinOrderAmt()) < 0) {
            log.error("====== {}-{}-StrategyCommon: 按最小下单币数下单 SELL {} ======", spot.getSymbol(), currentStrategy, spot.getLimitOrderMinOrderAmt());
            orderAmount = spot.getLimitOrderMinOrderAmt();
        } else {
            orderAmount = coinAmount;
        }

        spot.setOrderAmount(orderAmount);

        log.error("====== {}-{}-StrategyCommon: SELL at: {},  clientOrderId: {}, orderAmount: {}, type: {} ======", spot.getSymbol(), currentStrategy, sellPrice.toString(), clientOrderId, orderAmount, type);
        CreateOrderRequest sellRequest;
        if (type == 1) {
            sellRequest = CreateOrderRequest.spotSellLimit(spot.getAccountId(), clientOrderId, spot.getSymbol(), sellPrice, orderAmount);
        } else {
            sellRequest = CreateOrderRequest.spotSellMarket(spot.getAccountId(), clientOrderId, spot.getSymbol(), orderAmount);
        }
        CurrentAPI.getApiInstance().getTradeClient().createOrder(sellRequest);
        sellOrderMap.putIfAbsent(clientOrderId, spot);


    }

    /**
     * todo 模拟下单.
     * <p>
     * 1:sell-limit
     * 2:sell-market
     */
    public static void sellSimulation(int currentStrategy, Spot spot, BigDecimal buyPrice, BigDecimal coinAmount, int type) {
        // 计算卖出价格 buyPrice * (1+offset);
        BigDecimal sellPrice = null;
        if (currentStrategy == 1) {
            sellPrice = buyPrice.multiply(Constants.SELL_OFFSET_1);
        } else if (currentStrategy == 2) {
            sellPrice = buyPrice.multiply(Constants.SELL_OFFSET_2);
        } else if (currentStrategy == 3) {
            sellPrice = buyPrice.multiply(Constants.SELL_OFFSET_3);
        }
        //自定义订单号
        String clientOrderId = spot.getSymbol() + System.nanoTime();
        // 价格,币数 有严格的小数位限制
        assert sellPrice != null;
        sellPrice = sellPrice.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);
        coinAmount = coinAmount.setScale(spot.getAmountPrecision(), RoundingMode.HALF_UP);
        //最小下单量限制
        BigDecimal orderAmount;
        //最小下单量限制
        if (coinAmount.compareTo(spot.getLimitOrderMinOrderAmt()) < 0) {
            log.error("====== {}-{}-StrategyCommon: 按最小下单币数下单 SELL {} ======", spot.getSymbol(), currentStrategy, spot.getLimitOrderMinOrderAmt());
            orderAmount = spot.getLimitOrderMinOrderAmt();
        } else {
            orderAmount = coinAmount;
        }

        spot.setOrderAmount(orderAmount);

        CreateOrderRequest sellRequest;
        if (type == 1) {
            log.error("====== {}-{}-StrategyCommon: 限价-SELL at: {},  clientOrderId: {}, orderAmount: {}, type: {} ======", spot.getSymbol(), currentStrategy, sellPrice.toString(), clientOrderId, orderAmount, type);
        } else {
            log.error("====== {}-{}-StrategyCommon: 市价-SELL at: {},  clientOrderId: {}, orderAmount: {}, type: {} ======", spot.getSymbol(), currentStrategy, sellPrice.toString(), clientOrderId, orderAmount, type);
        }
        sellOrderMap.putIfAbsent(clientOrderId, spot);


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

    public static void resetFeeAndProfit(String symbol, int currentStrategy) {
        profit = BigDecimal.ZERO;
        fee = BigDecimal.ZERO;
        log.info("====== {}-{}-StrategyCommon-resetFeeAndProfit : profit= {} , fee= {} ======", symbol, currentStrategy, profit.toString(), fee.toString());
    }


    /**
     * 返回账户 ID
     * spot：现货账户
     * point: 点卡账户
     */
    public static Long getAccountIdByType(String type) {
        List<Account> accountList = CurrentAPI.getApiInstance().getAccountClient().getAccounts();
        accountList.forEach(account -> {
            if (account.getType().equals(type) && account.getState().equals("working")) {
                accountId = account.getId();
            }
        });
        log.error("====== HuobiUtil-getAccountIdByType: {} accountId= {} ======", type, accountId);
        return accountId > 0 ? accountId : -1;
    }

    /**
     * 根据账户 ID 查询账户余额
     *
     * @param quotaCurrency 交易对
     * @return 返回usdt余额
     */
    public static BigDecimal getQuotaBalanceByAccountId(Long accountId, String quotaCurrency) {
        AtomicReference<BigDecimal> bal = new AtomicReference<>(BigDecimal.ZERO);
        AccountBalance accountBalance = CurrentAPI.getApiInstance().getAccountClient().getAccountBalance(AccountBalanceRequest.builder().accountId(accountId).build());
        List<Balance> accountBalanceList = accountBalance.getList();
        accountBalanceList.forEach(balance -> {
            if (balance.getCurrency().equalsIgnoreCase(quotaCurrency)) {
                if (balance.getType().equalsIgnoreCase("trade")) {
                    bal.set(balance.getBalance());
                }
            }
        });
        log.info("====== HuobiUtil-getBalanceByAccountId: {}-trade(账户可用余额): {} ======", quotaCurrency, bal.get().toString());
        return bal.get();
    }

    /**
     * 根据账户 ID 查询账户余额
     *
     * @param quotaCurrency 交易对
     * @return 返回账户余额
     */
    public static String getBalance4Push(Long accountId, String baseCurrency, String quotaCurrency) {
        StringBuilder sb = new StringBuilder();
        AtomicReference<BigDecimal> bal = new AtomicReference<>(new BigDecimal("0"));
        AccountBalance accountBalance = CurrentAPI.getApiInstance().getAccountClient().getAccountBalance(AccountBalanceRequest.builder().accountId(accountId).build());
        List<Balance> accountBalanceList = accountBalance.getList();
        accountBalanceList.forEach(balance -> {
            if (balance.getCurrency().equalsIgnoreCase(baseCurrency)) {
                if (balance.getType().equalsIgnoreCase("trade")) {
                    sb.append(baseCurrency).append("-trade: ").append(balance.getBalance()).append("; \n");
                }
                if (balance.getType().equalsIgnoreCase("frozen")) {
                    sb.append(baseCurrency).append("-frozen: ").append(balance.getBalance()).append("; \n");
                }

            }
            if (balance.getCurrency().equalsIgnoreCase(quotaCurrency)) {
                if (balance.getType().equalsIgnoreCase("trade")) {
                    bal.set(balance.getBalance());
                    sb.append(quotaCurrency).append("-trade: ").append(balance.getBalance()).append("; \n");
                }
                if (balance.getType().equalsIgnoreCase("frozen")) {
                    sb.append(quotaCurrency).append("-frozen: ").append(balance.getBalance()).append("; \n");
                }
            }
        });
        log.info("====== HuobiUtil-getBalance4Push: {} ======", sb.toString());
        return sb.toString();
    }

    /**
     * 根据账户 ID 查询账户余额
     *
     * @param accountId 现货,点卡
     */
    public static BigDecimal getBalanceByAccountId(Long accountId) {
        AtomicReference<BigDecimal> bal = new AtomicReference<>(BigDecimal.ZERO);

        AccountBalance accountBalance = CurrentAPI.getApiInstance().getAccountClient().getAccountBalance(AccountBalanceRequest.builder().accountId(accountId).build());
        List<Balance> accountBalanceList = accountBalance.getList();
        accountBalanceList.forEach(balance -> {
            if (balance.getType().equalsIgnoreCase("trade")) {
                log.info("====== HuobiUtil-getBalanceByAccountId: point balance: {} ======", balance.toString());

                bal.set(balance.getBalance());

            }
        });
        return bal.get();


    }

    /**
     * 微信消息推送
     *
     * @param msg  消息内容
     * @param type 消息类型  1: 价格 2:其他
     *             1775 价格监控
     *             1776 其他
     */
    public static void weChatPusher(int strategy, String msg, int type) {
        try {
            Map<String, Object> params = new HashMap<>();
            Long[] topics = null;
            params.put("appToken", Constants.WX_PUSHER_TOKEN);
            params.put("content", msg);
            if (type == 1) {
                topics = new Long[]{1775L};
            } else if (type == 2) {
                topics = new Long[]{1776L};
            }
            params.put("topicIds", topics);

            String body = HbdmHttpClient.getInstance().doPost2WX(Constants.WX_PUSHER_URL, params);
            if (body.contains("处理成功")) {
                log.error("=== HuobiUtil-weChatPusher strategy-{}-推送成功: {} ======", strategy, msg);
            }
        } catch (Exception e) {
            log.error("=== HuobiUtil-weChatPusher: strategy-{}-无法推送消息 ======", strategy);
            e.printStackTrace();
        }
    }

    /**
     * @return 最近成交价
     */
    public static BigDecimal getCurrentTradPrice(String symbol) {
        AtomicReference<BigDecimal> currentPrice = new AtomicReference<>(new BigDecimal("0"));
        List<MarketTrade> marketTradeList = CurrentAPI.getApiInstance().getMarketClient().getMarketTrade(MarketTradeRequest.builder().symbol(symbol).build());
        marketTradeList.forEach(marketTrade -> {
//            logger.info(marketTrade.toString());
            currentPrice.set(marketTrade.getPrice());
        });
        log.info("====== symbol: {}, currentPrice: {}", symbol, currentPrice);
        return currentPrice.get();
    }


    /**
     * 重启后,取消当前交易对的所有orderSide方向的订单
     */
    public static void cancelOpenOrders(Long accountId, String symbol, int strategy, OrderSideEnum orderSide) {

        List<Order> orderList = CurrentAPI.getApiInstance().getTradeClient().getOpenOrders(OpenOrdersRequest.builder()
                .accountId(accountId)
                .symbol(symbol)
                .side(orderSide)
                .build());

        log.error("====== HuobiUtil-cancelOpenOrders: 取消 {}-{} 的所有 {} 单,之前有 {} 个订单 ======", symbol, strategy, orderSide, orderList.size());
        orderList.forEach(order -> CurrentAPI.getApiInstance().getTradeClient().cancelOrder(order.getId()));
    }

    /**
     * 取消 buy 订单
     * 7	canceled
     * 10 cancelling
     */
    public static void cancelOrder(int strategy, String clientOrderId) {
        int code = CurrentAPI.getApiInstance().getTradeClient().cancelOrder(clientOrderId);
        if (code == 7) {
            log.error("=== HuobiUtil-cancelOrder: strategy-{}-{} canceled ======", strategy, clientOrderId);
        }
    }


    /**
     * 查询卖单
     */
    public static Order getOrderByOrderId(Long orderId) {
        return CurrentAPI.getApiInstance().getTradeClient().getOrder(orderId);
    }

    /**
     * 查询卖单
     *
     * @param clientOrderId 只有买单有自定义的clientOrderId
     * @return
     */
    public static Order getOrderByClientId(String clientOrderId) {
        try {
            Order order = CurrentAPI.getApiInstance().getTradeClient().getOrder(clientOrderId);
            return order;

        } catch (Exception e) {
            // 避免各种原因下单错误, 导致整体逻辑异常
            return null;
        }
    }

    /**
     * @param side buy, sell
     * @return 某一方向上, 所有未成交订单.
     */
    public static List<Order> getOpenOrders(Long accountId, String symbol, OrderSideEnum side) {
        List<Order> orderList = CurrentAPI.getApiInstance().getTradeClient().getOpenOrders(OpenOrdersRequest.builder()
                .accountId(accountId)
                .symbol(symbol)
                .side(side)
                .build());
        orderList.forEach(order -> log.error("=== HuobiUtil-getOpenOrders: " + order.toString() + " ======"));
        return orderList;
    }

    /**
     * 原本 大概1000左右, 筛选后 几十个 币种
     * 分区 symbolPartition :
     * main 主板-主区
     * innovation 创业板
     * potentials 观察区
     * pioneer  新币
     */
    public static ArrayList<String> getSymbolByConditions(String quoteCurrency) {
        ArrayList<String> list = new ArrayList<>();
        List<Symbol> symbolList = CurrentAPI.getApiInstance().getGenericClient().getSymbols();
        symbolList.forEach(symbol -> {
            // 不包含  btc*3 之类的
            if (null == symbol.getUnderlying()) {
                // online  usdt交易对
                if ("online".equalsIgnoreCase(symbol.getState()) && quoteCurrency.equalsIgnoreCase(symbol.getQuoteCurrency())) {
                    // 主板,观察区,创业板
                    if (SymbolPartionEnum.MAIN.getName().equalsIgnoreCase(symbol.getSymbolPartition())
                            || SymbolPartionEnum.POTENTIALS.getName().equalsIgnoreCase(symbol.getSymbolPartition())
                            || SymbolPartionEnum.INNOVATION.getName().equalsIgnoreCase(symbol.getSymbolPartition())
                    ) {
                        list.add(symbol.getSymbol());
                    }
                }
            }
        });

        log.error("====== HuobiUtil-getAllAvailableSymbols: 从主板,观察区,创业板 筛选出 {} 个交易对======", list.size());

        return list;
    }

    public static HashMap<String, Spot> getSymbolInfoByName(HashMap<String, Spot> map, BigDecimal portion) {
        List<Symbol> symbolList = CurrentAPI.getApiInstance().getGenericClient().getSymbols();
        symbolList.forEach(symbol -> {
            if (map.containsKey(symbol.getSymbol())) {
                Spot spot = new Spot();
                spot.setSymbol(symbol.getSymbol());
                spot.setBaseCurrency(symbol.getBaseCurrency());
                spot.setQuoteCurrency(symbol.getQuoteCurrency());
                spot.setMinOrderValue(symbol.getMinOrderValue());
                spot.setPricePrecision(symbol.getPricePrecision());
                spot.setAmountPrecision(symbol.getAmountPrecision());
                spot.setSellMarketMinOrderAmt(symbol.getSellMarketMinOrderAmt());
                spot.setLimitOrderMinOrderAmt(symbol.getLimitOrderMinOrderAmt());
                spot.setTotalBalance(portion);
                map.replace(symbol.getSymbol(), spot);
            }
        });

        return map;

    }


}
