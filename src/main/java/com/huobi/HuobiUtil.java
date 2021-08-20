package com.huobi;

import com.huobi.client.req.account.AccountBalanceRequest;
import com.huobi.client.req.market.MarketTradeRequest;
import com.huobi.client.req.trade.OpenOrdersRequest;
import com.huobi.constant.Constants;
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
        logger.info("===getAccountIdByType()  accountId : " + accountId + " ======");
        return accountId > 0 ? accountId : -1;
    }

    /**
     * 根据账户 ID 查询账户余额
     *
     * @param accountId
     * @param baseCurrency
     * @param quotaCurrency 交易对
     */
    public static BigDecimal getBalanceByAccountId(Long accountId, String baseCurrency, String quotaCurrency) {
        AtomicReference<BigDecimal> bal = new AtomicReference<>(new BigDecimal("0"));
        AccountBalance accountBalance = CurrentAPI.getApiInstance().getAccountClient().getAccountBalance(AccountBalanceRequest.builder().accountId(accountId).build());
        List<Balance> accountBalanceList = accountBalance.getList();
        accountBalanceList.forEach(balance -> {
            if (balance.getCurrency().equalsIgnoreCase(baseCurrency) || balance.getCurrency().equalsIgnoreCase(quotaCurrency)) {
                if (balance.getType().equalsIgnoreCase("trade")) {
                    logger.info("===getBalanceByAccountId() 现有仓位: " + balance.toString() + " ======");
                    bal.set(balance.getBalance());
                }
            }
        });

        return bal.get();

    }

    /**
     * 根据账户 ID 查询账户余额
     *
     * @param accountId
     */
    public static BigDecimal getBalanceByAccountId(Long accountId) {
        AtomicReference<BigDecimal> bal = new AtomicReference<>(new BigDecimal("0"));

        AccountBalance accountBalance = CurrentAPI.getApiInstance().getAccountClient().getAccountBalance(AccountBalanceRequest.builder().accountId(accountId).build());
        List<Balance> accountBalanceList = accountBalance.getList();
        accountBalanceList.forEach(balance -> {
            if (balance.getType().equalsIgnoreCase("trade")) {

//                logger.info("===getBalanceByAccountId() 账户余额: " + balance.toString() + " ======");
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
    public static void weChatPusher(String msg, int type) {
        try {
            Map<String, Object> params = new HashMap<>();
            Long[] topics = null;
            params.put("appToken", Constants.WX_PUSHER_TOKEN);
            params.put("content", msg);
            if (type == 1) topics = new Long[]{1775L};
            if (type == 2) topics = new Long[]{1776L};
            params.put("topicIds", topics);

            String body = HbdmHttpClient.getInstance().doPost2WX(Constants.WX_PUSHER_URL, params);
            if (body.contains("处理成功")) logger.error("===weChatPusher(): 推送成功 " + msg + " ======");
        } catch (Exception e) {
            logger.error("===weChatPusher(): 无法推送消息 ======");
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
        System.out.println("getCurrentTradPrice " + symbol + " : " + currentPrice);
        return currentPrice.get();
    }


    /**
     * 获取所有挂单
     */
    public static List<Order> openOrders(Long accountId, String symbol, OrderSideEnum orderSide) {
        List<Order> orderList = CurrentAPI.getApiInstance().getTradeClient().getOpenOrders(OpenOrdersRequest.builder()
                .accountId(accountId)
                .symbol(symbol)
                .side(orderSide)
                .build());
        orderList.forEach(order -> {
            logger.info("===openOrders(): " + order.toString() + " ======");
        });
        return orderList;
    }

    /**
     * 取消 buy 订单
     * 7	canceled
     * 10 cancelling
     *
     * @param clientOrderId
     */
    public static int cancelOrder(String clientOrderId) {
        int code = CurrentAPI.getApiInstance().getTradeClient().cancelOrder(clientOrderId);
        if (code == 7) {
            logger.info("=== cancelOrder(): " + clientOrderId + " ======");
            logger.info("=== cancelOrder(): canceled ======");
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
//        logger.info("===卖单: " + order);
        return order;
    }

    /**
     * 查询卖单
     *
     * @param clientOrderId 只有买单有自定义的clientOrderId
     * @return
     */
    public static Order getOrderByClientId(String clientOrderId) {
        Order order = CurrentAPI.getApiInstance().getTradeClient().getOrder(clientOrderId);
//        logger.info("===买单: " + order);
        return order;
    }

    public static void setPrecision(Spot spot) {
        List<Symbol> symbolList = CurrentAPI.getApiInstance().getGenericClient().getSymbols();
        symbolList.forEach(symbol -> {
            if (symbol.getBaseCurrency().equalsIgnoreCase(spot.getBaseCurrency()) && symbol.getQuoteCurrency().equalsIgnoreCase(spot.getQuoteCurrency())) {
                spot.setPricePrecision(symbol.getPricePrecision());
                spot.setAmountPrecision(symbol.getAmountPrecision());
            }
        });
    }

}
