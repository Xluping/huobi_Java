package com.huobi.constant;

import java.math.BigDecimal;

public class Constants {

    public static final String API_KEY = "cbcbf07e-6c66e380-vf25treb80-6c28c";
    public static final String SECRET_KEY = "";

    // wechat pusher

    public static final String WX_PUSHER_TOKEN = "";
    public static final String WX_PUSHER_URL = "http://wxpusher.zjiecode.com/api/send/message";

    /////////////////////////高频//////////////////////////////////
    public static final Double HIGH_RATIO_1 = 0.6;
    public static final Double MEDIUM_RATIO_1 = 0.2;
    public static final Double LOW_RATIO_1 = 0.2;

    //策略区间 30%以内

    public static final double HIGH_RANGE_1 = 10;   // 0-10%
    public static final double MEDIUM_RANGE_1 = 20; // 10-20%
    public static final double LOW_RANGE_1 = 30;   // 20-30%   <100

    // 策略区间内下单次数
    public static final double HIGH_COUNT_1 = 40; // 0.25%
    public static final double MEDIUM_COUNT_1 = 20; // 0.5%
    public static final double LOW_COUNT_1 = 10; // 1%

    //offset 止盈点,  涨 2% 就卖掉.
    public static final BigDecimal SELL_OFFSET_1 = new BigDecimal("1.02");
    ///////////////////////////////////////////////////////////


    /////////////////////////稳健//////////////////////////////////
    public static final Double HIGH_RATIO_2 = 0.3;
    public static final Double MEDIUM_RATIO_2 = 0.5;
    public static final Double LOW_RATIO_2 = 0.2;
    //策略区间 40%以内
    public static final double HIGH_RANGE_2 = 10;   // 0-10%
    public static final double MEDIUM_RANGE_2 = 30; // 10-30%
    public static final double LOW_RANGE_2 = 40;   // 30-40%   <100

    // 策略区间内下单次数
    public static final double HIGH_COUNT_2 = 10;  //1%
    public static final double MEDIUM_COUNT_2 = 40; //0.5%
    public static final double LOW_COUNT_2 = 10;    //1%

    //offset 止盈点,  涨 2% 就卖掉.
    public static final BigDecimal SELL_OFFSET_2 = new BigDecimal("1.05");
    ///////////////////////////////////////////////////////////


    /////////////////////////保守//////////////////////////////////
    public static final Double HIGH_RATIO_3 = 0.2;
    public static final Double MEDIUM_RATIO_3 = 0.2;
    public static final Double LOW_RATIO_3 = 0.6;
    //策略区间 30%以内
    public static final double HIGH_RANGE_3 = 10;   // 0-10%
    public static final double MEDIUM_RANGE_3 = 15; // 10-15%
    public static final double LOW_RANGE_3 = 35;   // 15-35%   <100

    // 策略区间内下单次数
    public static final double HIGH_COUNT_3 = 10;  //1%下一次单
    public static final double MEDIUM_COUNT_3 = 10; //0.5% 下一次单
    public static final double LOW_COUNT_3 = 20;   //1% 下一次单

    //offset 止盈点,  涨 2% 就卖掉.
    public static final BigDecimal SELL_OFFSET_3 = new BigDecimal("1.1");
    ///////////////////////////////////////////////////////////

}
