package com.huobi;

import com.huobi.client.req.account.AccountBalanceRequest;
import com.huobi.client.req.market.MarketTradeRequest;
import com.huobi.client.req.trade.OpenOrdersRequest;
import com.huobi.constant.enums.OrderSideEnum;
import com.huobi.model.account.Account;
import com.huobi.model.account.AccountBalance;
import com.huobi.model.account.Balance;
import com.huobi.model.generic.Symbol;
import com.huobi.model.market.MarketTrade;
import com.huobi.model.trade.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 8/18/21 11:10 AM
 */
public class HuobiUtil {
    static Logger logger = LoggerFactory.getLogger(HuobiUtil.class);
    static long accountId = -1L;

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
        logger.error("====== HuobiUtil-getAccountIdByType: {} accountId= {} ======", type, accountId);
        return accountId > 0 ? accountId : -1;
    }

    /**
     * 根据账户 ID 查询账户余额
     *
     * @param accountId
     * @param quotaCurrency 交易对
     * @return 返回账户余额
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
        logger.info("====== HuobiUtil-getBalanceByAccountId: {}-trade(账户可用余额): {} ======", quotaCurrency, bal.get().toString());
        return bal.get();
    }

    /**
     * 根据账户 ID 查询账户余额
     *
     * @param accountId
     * @param baseCurrency
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
        logger.info("====== HuobiUtil-getBalance4Push: {} ======", sb.toString());
        return sb.toString();
    }

    /**
     * 根据账户 ID 查询账户余额
     *
     * @param accountId
     */
    public static BigDecimal getBalanceByAccountId(Long accountId) {
        AtomicReference<BigDecimal> bal = new AtomicReference<>(BigDecimal.ZERO);

        AccountBalance accountBalance = CurrentAPI.getApiInstance().getAccountClient().getAccountBalance(AccountBalanceRequest.builder().accountId(accountId).build());
        List<Balance> accountBalanceList = accountBalance.getList();
        accountBalanceList.forEach(balance -> {
            if (balance.getType().equalsIgnoreCase("trade")) {
                logger.info("====== HuobiUtil-getBalanceByAccountId: point balance: {} ======", balance.toString());

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
                logger.error("=== HuobiUtil-weChatPusher strategy-{}-推送成功: {} ======", strategy, msg);
            }
        } catch (Exception e) {
            logger.error("=== HuobiUtil-weChatPusher: strategy-{}-无法推送消息 ======", strategy);
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
        logger.info("====== symbol: {}, currentPrice: {}", symbol, currentPrice);
        return currentPrice.get();
    }


    /**
     * 重启后,取消当前交易对的所有orderSide方向的订单
     *
     * @param accountId
     * @param symbol
     * @param orderSide
     * @return
     */
    public static void cancelOpenOrders(Long accountId, String symbol, int strategy, OrderSideEnum orderSide) {

        List<Order> orderList = CurrentAPI.getApiInstance().getTradeClient().getOpenOrders(OpenOrdersRequest.builder()
                .accountId(accountId)
                .symbol(symbol)
                .side(orderSide)
                .build());

        logger.error("====== HuobiUtil-cancelOpenOrders: 取消 {}-{} 的所有 {} 单,之前有 {} 个订单 ======", symbol, strategy, orderSide, orderList.size());
        orderList.forEach(order -> {
            CurrentAPI.getApiInstance().getTradeClient().cancelOrder(order.getId());
        });
    }

    /**
     * 取消 buy 订单
     * 7	canceled
     * 10 cancelling
     *
     * @param clientOrderId
     */
    public static int cancelOrder(int strategy, String clientOrderId) {
        int code = CurrentAPI.getApiInstance().getTradeClient().cancelOrder(clientOrderId);
        if (code == 7) {
            logger.error("=== HuobiUtil-cancelOrder: strategy-{}-{} canceled ======", strategy, clientOrderId);
        }
        return code;
    }


    /**
     * 查询卖单
     *
     * @param orderId
     * @return
     */
    public static Order getOrderByOrderId(Long orderId) {
        Order order = CurrentAPI.getApiInstance().getTradeClient().getOrder(orderId);
        return order;
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
        orderList.forEach(order -> {
            logger.error("=== HuobiUtil-getOpenOrders: " + order.toString() + " ======");
        });
        return orderList;
    }

    /**
     *
     */
    public static ArrayList<Spot> getAllAvailableSymbols(Long accountId) {
        ArrayList<Spot> list = new ArrayList<>();
        List<Symbol> symbolList = CurrentAPI.getApiInstance().getGenericClient().getSymbols();
        symbolList.forEach(symbol -> {
            Spot spot = new Spot();
            spot.setAccountId(accountId);
            spot.setBaseCurrency(symbol.getBaseCurrency());
            spot.setQuoteCurrency(symbol.getQuoteCurrency());
            spot.setSymbol(symbol.getSymbol());
            spot.setSellMarketMinOrderAmt(symbol.getSellMarketMinOrderAmt());
            spot.setLimitOrderMinOrderAmt(symbol.getLimitOrderMinOrderAmt());
            spot.setMinOrderValue(symbol.getMinOrderValue());
            spot.setPricePrecision(symbol.getPricePrecision());
            spot.setAmountPrecision(symbol.getAmountPrecision());
            list.add(spot);
        });

        logger.error("====== HuobiUtil-getAllAvailableSymbols:  state=online  list.size= {}======", list.size());

        return list;
    }
}
