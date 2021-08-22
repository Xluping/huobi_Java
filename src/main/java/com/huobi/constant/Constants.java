package com.huobi.constant;

import java.math.BigDecimal;

public class Constants {

    public static final String SERVER_BASE_CURRENCY = "doge";
    public static final String SERVER_QUOTE_CURRENCY = "usdt";
    public static final String SERVER_SYMBOL = "dogeusdt";
    public static final double SERVER_USDT = 200.0;

    public static final String API_KEY = "cbcbf07e-6c66e380-vf25treb80-6c28c";
    public static final String SECRET_KEY = "";

    // wechat pusher

    public static final String WX_PUSHER_TOKEN = "";
    public static final String WX_PUSHER_URL = "http://wxpusher.zjiecode.com/api/send/message";

    /////////////////////////高频//////////////////////////////////
    public static final Double HIGH_RATIO = 0.8;
    public static final Double MEDIUM_RATIO = 0.1;
    public static final Double LOW_RATIO = 0.1;

    //策略区间

    public static final double HIGH_RANGE = 30;   // 0-30%
    public static final double MEDIUM_RANGE = 50; // 30-50%
    public static final double LOW_RANGE = 70;   // 50-70%   <100

    // 策略区间内下单次数
    public static final double HIGH_COUNT = 30;
    public static final double MEDIUM_COUNT = 4;
    public static final double LOW_COUNT = 2;

    //offset 止盈点,  涨 2% 就卖掉.
    public static final BigDecimal SELL_OFFSET = new BigDecimal("1.02");
    ///////////////////////////////////////////////////////////
    /////////////////////////稳健//////////////////////////////////
//    public static final Double HIGH_RATIO = 0.3;
//    public static final Double MEDIUM_RATIO = 0.6;
//    public static final Double LOW_RATIO = 0.1;
//    //策略区间
//    public static final double HIGH_RANGE = 10;   // 0-10%
//    public static final double MEDIUM_RANGE = 40; // 10-40%
//    public static final double LOW_RANGE = 50;   // 40-50%   <100
//
//    // 策略区间内下单次数
//    public static final double HIGH_COUNT = 5;
//    public static final double MEDIUM_COUNT = 10;
//    public static final double LOW_COUNT = 2;
//
//    //offset 止盈点,  涨 2% 就卖掉.
//    public static final BigDecimal SELL_OFFSET = new BigDecimal("1.15");
    ///////////////////////////////////////////////////////////
    /////////////////////////保守//////////////////////////////////
//    public static final Double HIGH_RATIO = 0.3;
//    public static final Double MEDIUM_RATIO = 0.3;
//    public static final Double LOW_RATIO = 0.4;
//    //策略区间
//    public static final double HIGH_RANGE = 20;   // 0-20%
//    public static final double MEDIUM_RANGE = 40; // 20-40%
//    public static final double LOW_RANGE = 80;   // 40-80%   <100
//
//    // 策略区间内下单次数
//    public static final double HIGH_COUNT = 4;  //5%下一次单
//    public static final double MEDIUM_COUNT = 4; //10% 下一次单
//    public static final double LOW_COUNT = 10;
//
//    //offset 止盈点,  涨 2% 就卖掉.
//    public static final BigDecimal SELL_OFFSET = new BigDecimal("1.2");
    ///////////////////////////////////////////////////////////

}
