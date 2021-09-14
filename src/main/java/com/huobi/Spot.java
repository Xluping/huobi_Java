package com.huobi;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 8/19/21 1:35 PM
 * 自定义spot 类, 大多数 字段来自 Symbol
 */
@Data
public class Spot {
    /**
     * 策略相关的属性
     */
    // BTC/USDT
    private Long accountId;
    private String symbol;
    private String baseCurrency; // BTC
    private String quoteCurrency;// USDT
    private BigDecimal doublePrice;
    private BigDecimal triplePrice;
    private BigDecimal totalBalance;// 分配到的总仓位  5:3:2
    private BigDecimal highStrategyBalance; // 高频总仓位
    private BigDecimal mediumStrategyBalance; // 稳健总仓位
    private BigDecimal lowStrategyBalance;// 保守总仓位
    private BigDecimal portionHigh;  // 高频每次补仓的仓位
    private BigDecimal portionMedium;// 稳健每次补仓的仓位
    private BigDecimal portionLow;  // 保守每次补仓的仓位


    /**
     * 下单相关的属性
     */
    private BigDecimal startPrice;
    private BigDecimal orderValue;
    private BigDecimal orderAmount;
    private int pricePrecision;
    private int amountPrecision;
    private BigDecimal limitOrderMinOrderAmt; //限价单最小下单量 ，以基础币种为单位
    private BigDecimal sellMarketMinOrderAmt; // 市价卖单 最小下单量，以基础币种为单位
    private BigDecimal minOrderValue; //交易对限价单和市价买单最小下单金额 ，以计价币种为单位
    private BigDecimal stopPrice; //止盈价格
}
