package com.huobi;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 8/19/21 1:35 PM
 */
@Data
public class Spot {
    // BTC/USDT
    private Long accountId;
    private String symbol;
    private String baseCurrency; // BTC
    private String quoteCurrency;// USDT
    private BigDecimal startPrice;
    private BigDecimal doublePrice;
    private BigDecimal triplePrice;
    private int pricePrecision;
    private int amountPrecision;
    private BigDecimal totalBalance;// 分配到的总仓位  5:3:2
    private BigDecimal highStrategyBalance; // 高频仓位
    private BigDecimal mediumStrategyBalance; // 稳健仓位
    private BigDecimal lowStrategyBalance;// 保守仓位
    private int strategyOffset; // 策略分配: 高频 0-20%  稳健 20-50% 保守 50%+
    private int orderOffset; //下单间隔
    private BigDecimal limitOrderMinOrderAmt; //限价单最小下单量 ，以基础币种为单位
    private BigDecimal sellMarketMinOrderAmt; // 市价卖单 最小下单量，以基础币种为单位
    private BigDecimal minOrderValue; //交易对限价单和市价买单最小下单金额 ，以计价币种为单位
    private BigDecimal portionHigh;
    private BigDecimal portionMedium;
    private BigDecimal portionLow;
}
