package com.huobi.constant;

import java.math.BigDecimal;

public class Constants {

    public static final String API_KEY = "cbcbf07e-6c66e380-vf25treb80-6c28c";
    public static final String SECRET_KEY = "";

    // wechat pusher

    public static final String WX_PUSHER_TOKEN = "";
    public static final String WX_PUSHER_URL = "http://wxpusher.zjiecode.com/api/send/message";

    /////////////////////////高频//////////////////////////////////
    public static final Double HIGH_RATIO_1 = 0.8;
    public static final Double MEDIUM_RATIO_1 = 0.1;
    public static final Double LOW_RATIO_1 = 0.1;

    //策略区间

    public static final double HIGH_RANGE_1 = 30;   // 0-30%
    public static final double MEDIUM_RANGE_1 = 50; // 30-50%
    public static final double LOW_RANGE_1 = 70;   // 50-70%   <100

    // 策略区间内下单次数
    public static final double HIGH_COUNT_1 = 30;
    public static final double MEDIUM_COUNT_1 = 4;
    public static final double LOW_COUNT_1 = 2;

    //offset 止盈点,  涨 2% 就卖掉.
    public static final BigDecimal SELL_OFFSET_1 = new BigDecimal("1.02");
    ///////////////////////////////////////////////////////////
    /////////////////////////稳健//////////////////////////////////
    public static final Double HIGH_RATIO_2 = 0.3;
    public static final Double MEDIUM_RATIO_2 = 0.6;
    public static final Double LOW_RATIO_2 = 0.1;
    //策略区间
    public static final double HIGH_RANGE_2 = 10;   // 0-10%
    public static final double MEDIUM_RANGE_2 = 40; // 10-40%
    public static final double LOW_RANGE_2 = 50;   // 40-50%   <100

    // 策略区间内下单次数
    public static final double HIGH_COUNT_2 = 5;
    public static final double MEDIUM_COUNT_2 = 10;
    public static final double LOW_COUNT_2 = 10;

    //offset 止盈点,  涨 2% 就卖掉.
    public static final BigDecimal SELL_OFFSET_2 = new BigDecimal("1.10");
    ///////////////////////////////////////////////////////////
    /////////////////////////保守//////////////////////////////////
    public static final Double HIGH_RATIO_3 = 0.3;
    public static final Double MEDIUM_RATIO_3 = 0.3;
    public static final Double LOW_RATIO_3 = 0.4;
    //策略区间
    public static final double HIGH_RANGE_3 = 20;   // 0-20%
    public static final double MEDIUM_RANGE_3 = 40; // 20-40%
    public static final double LOW_RANGE_3 = 80;   // 40-80%   <100

    // 策略区间内下单次数
    public static final double HIGH_COUNT_3 = 4;  //5%下一次单
    public static final double MEDIUM_COUNT_3 = 4; //10% 下一次单
    public static final double LOW_COUNT_3 = 10;

    //offset 止盈点,  涨 2% 就卖掉.
    public static final BigDecimal SELL_OFFSET_3 = new BigDecimal("1.2");
    ///////////////////////////////////////////////////////////

}
