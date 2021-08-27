package com.huobi;

import com.huobi.client.req.account.AccountBalanceRequest;
import com.huobi.client.req.trade.OpenOrdersRequest;
import com.huobi.constant.enums.OrderSideEnum;
import com.huobi.model.account.Account;
import com.huobi.model.account.AccountBalance;
import com.huobi.model.account.Balance;
import com.huobi.model.generic.Symbol;
import com.huobi.model.trade.Order;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class HuobiUtilTest {
    private final Long accountId = 14086863L;
    private Long pointAccountId = 14424186L;

    String symbol = "htusdt";
    BigDecimal amount = new BigDecimal("5");
    Logger logger = LoggerFactory.getLogger(HuobiUtilTest.class);


    @Test
    public void getAccountIdByType() {
        String type = "point";
        List<Account> accountList = CurrentAPI.getApiInstance().getAccountClient().getAccounts();
        accountList.forEach(account -> {
            if (account.getType().equals(type) && account.getState().equals("working")) {
                pointAccountId = account.getId();
            }
        });
        logger.error("====== pointAccountId : " + pointAccountId + " ======");
    }

    @Test
    public void getBalanceByAccountId() {
        String baseCurrency = "ht";
        String quotaCurrency = "usdt";
        StringBuilder sb = new StringBuilder();
        AtomicReference<BigDecimal> bal = new AtomicReference<>(new BigDecimal("0"));
        AccountBalance accountBalance = CurrentAPI.getApiInstance().getAccountClient().getAccountBalance(AccountBalanceRequest.builder().accountId(accountId).build());
        List<Balance> accountBalanceList = accountBalance.getList();
        accountBalanceList.forEach(balance -> {
            if (balance.getCurrency().equalsIgnoreCase(baseCurrency)) {
                if (balance.getType().equalsIgnoreCase("trade")) {
                    sb.append(baseCurrency).append("-trade: ").append(balance.getBalance()).append("; ");
                }
                if (balance.getType().equalsIgnoreCase("frozen")) {
                    sb.append(baseCurrency).append("-frozen: ").append(balance.getBalance()).append("; ");
                }

            }
            if (balance.getCurrency().equalsIgnoreCase(quotaCurrency)) {
                if (balance.getType().equalsIgnoreCase("trade")) {
                    bal.set(balance.getBalance());
                    sb.append(quotaCurrency).append("-trade: ").append(balance.getBalance()).append("; ");
                }
                if (balance.getType().equalsIgnoreCase("frozen")) {
                    sb.append(quotaCurrency).append("-frozen: ").append(balance.getBalance()).append("; ");
                }
            }
        });
        logger.info("====== HuobiUtil-getBalance4Push: {} ======", sb.toString());
    }

    @Test
    public void weChatPusher() {
    }

    @Test
    public void getCurrentTradPrice() {
    }

    @Test
    public void openOrders() {
        List<Order> orderList = CurrentAPI.getApiInstance().getTradeClient().getOpenOrders(OpenOrdersRequest.builder()
                .accountId(accountId)
                .symbol("dogeusdt")
                .side(OrderSideEnum.BUY)
                .build());
        orderList.forEach(order -> {
            logger.info("=== HuobiUtil-openOrders: " + order.toString() + " ======");
        });
    }

    @Test
    public void cancelOrder() {

        List<Order> orderList = CurrentAPI.getApiInstance().getTradeClient().getOpenOrders(OpenOrdersRequest.builder()
                .accountId(accountId)
                .symbol(symbol)
                .side(OrderSideEnum.BUY)
                .build());

        logger.error("====== HuobiUtil-cancelOpenOrders: 之前有 " + orderList.size() + " 个订单======");

        orderList.forEach(order -> {
            CurrentAPI.getApiInstance().getTradeClient().cancelOrder(order.getId());
        });
    }

    @Test
    public void symbols() {
        List<Symbol> symbolList = CurrentAPI.getApiInstance().getGenericClient().getSymbols();
        symbolList.forEach(symbol -> {
            System.out.println(symbol.toString());
        });
    }

    @Test
    public void precision() {
        List<Symbol> symbolList = CurrentAPI.getApiInstance().getGenericClient().getSymbols();
        symbolList.forEach(symbol -> {
            System.out.println(symbol.toString());
        });
    }
}