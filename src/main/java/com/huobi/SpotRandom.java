package com.huobi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 9/14/21 5:38 PM
 */
public class SpotRandom {
    private static final String QUOTE_CURRENCY = "usdt";
    private static Long spotAccountId = 14086863L;
    private static Long pointAccountId = 14424186L;
    private static volatile BigDecimal totalBalance = BigDecimal.ZERO;
    private static volatile BigDecimal portion = BigDecimal.ZERO;


    public static void main(String[] args) {

    }

    public void prepareSpot() {
        spotAccountId = HuobiUtil.getAccountIdByType("spot");
        pointAccountId = HuobiUtil.getAccountIdByType("point");
        totalBalance = HuobiUtil.getQuotaBalanceByAccountId(spotAccountId, QUOTE_CURRENCY);
        List<Spot> symbolList = HuobiUtil.getAllAvailableSymbols(spotAccountId);
        portion = totalBalance.divide(new BigDecimal(symbolList.size()));
        portion.setScale(1, RoundingMode.HALF_DOWN);
        // 当portion < 5 时, 删减 symbolList, usdt交易对,通常最小下单限制是 5 USDT
        while (portion.compareTo(new BigDecimal("5")) < 0) {

        }

    }

}
