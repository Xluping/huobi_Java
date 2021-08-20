package com.huobi.constant;

import java.math.BigDecimal;

public class Constants {

    public static final String API_KEY = "cbcbf07e-6c66e380-vf25treb80-6c28c";
    public static final String SECRET_KEY = "";

    // wechat pusher

    public static final String WX_PUSHER_TOKEN = "AT_RQ1xF00Lq69TkYNn0dC8DC8U6wM0vvaP";
    public static final String WX_PUSHER_URL = "http://wxpusher.zjiecode.com/api/send/message";

    public static final Double HIGH_RATIO = 0.8;
    public static final Double MEDIUM_RATIO = 0.1;
    public static final Double LOW_RATIO = 0.1;

    //策略区间

    public static final double HIGH_RANGE = 30;   // 0-30%
    public static final double MEDIUM_RANGE = 50; // 30-50%
    public static final double LOW_RANGE = 70;   // 50-70%

    // 策略区间内下单次数
    public static final double HIGH_COUNT = 20;
    public static final double MEDIUM_COUNT = 4;
    public static final double LOW_COUNT = 2;

    //offset 止盈点,  涨 2% 就卖掉.
    public static final BigDecimal SELL_OFFSET = new BigDecimal("1.02");

    //自编订单号prefix
    public static final String CLIENT_ID_PREFIX = "LUPING";

}
