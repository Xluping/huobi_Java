package com.huobi;

import com.huobi.client.req.account.AccountBalanceRequest;
import com.huobi.model.account.Account;
import com.huobi.model.account.AccountBalance;
import com.huobi.model.account.Balance;
import com.huobi.model.generic.Symbol;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class HuobiUtilTest {
    private final Long spotAccountId = 14086863L;
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
        AccountBalance accountBalance = CurrentAPI.getApiInstance().getAccountClient().getAccountBalance(AccountBalanceRequest.builder().accountId(pointAccountId).build());
        List<Balance> accountBalanceList = accountBalance.getList();
        accountBalanceList.forEach(balance -> {
            if (balance.getType().equalsIgnoreCase("trade"))
                logger.error("====== 账户余额: " + balance.toString() + " ======");
        });
    }

    @Test
    public void weChatPusher() {
    }

    @Test
    public void getCurrentTradPrice() {
    }

    @Test
    public void openOrders() {
    }

    @Test
    public void cancelOrder() {
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
        BigDecimal buyPrice = new BigDecimal("12.34567856534");
        buyPrice = buyPrice.setScale(4, RoundingMode.HALF_UP);
        System.out.println(buyPrice);
    }
}