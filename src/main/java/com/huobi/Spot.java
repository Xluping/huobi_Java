package com.huobi;

import java.math.BigDecimal;

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 8/19/21 1:35 PM
 */
public class Spot {
    // BTC/USDT
    private Long accountId;
    private String symbol;
    private String baseCurrency; // BTC
    private String quoteCurrency;// USDT
    private int pricePrecision;
    private int amountPrecision;
    private double totalBalance;// 分配到的总仓位  5:3:2
    private double highStrategyBalance; // 高频仓位
    private double mediumStrategyBalance; // 稳健仓位
    private double lowStrategyBalance;// 保守仓位
    private int strategyOffset; // 策略分配: 高频 0-20%  稳健 20-50% 保守 50%+
    private int orderOffset; //下单间隔
    private BigDecimal limitOrderMinOrderAmt; //限价单最小下单量 ，以基础币种为单位
    private BigDecimal sellMarketMinOrderAmt; // 市价卖单 最小下单量，以基础币种为单位
    private BigDecimal minOrderValue; //交易对限价单和市价买单最小下单金额 ，以计价币种为单位


    public Spot() {

    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    public void setQuoteCurrency(String quoteCurrency) {
        this.quoteCurrency = quoteCurrency;
    }


    public double getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(double totalBalance) {
        this.totalBalance = totalBalance;
    }

    public double getHighStrategyBalance() {
        return highStrategyBalance;
    }

    public void setHighStrategyBalance(double highStrategyBalance) {
        this.highStrategyBalance = highStrategyBalance;
    }

    public double getMediumStrategyBalance() {
        return mediumStrategyBalance;
    }

    public void setMediumStrategyBalance(double mediumStrategyBalance) {
        this.mediumStrategyBalance = mediumStrategyBalance;
    }

    public int getPricePrecision() {
        return pricePrecision;
    }

    public void setPricePrecision(int pricePrecision) {
        this.pricePrecision = pricePrecision;
    }

    public int getAmountPrecision() {
        return amountPrecision;
    }

    public void setAmountPrecision(int amountPrecision) {
        this.amountPrecision = amountPrecision;
    }

    public double getLowStrategyBalance() {
        return lowStrategyBalance;
    }

    public void setLowStrategyBalance(double lowStrategyBalance) {
        this.lowStrategyBalance = lowStrategyBalance;
    }

    public int getStrategyOffset() {
        return strategyOffset;
    }

    public void setStrategyOffset(int strategyOffset) {
        this.strategyOffset = strategyOffset;
    }

    public int getOrderOffset() {
        return orderOffset;
    }

    public void setOrderOffset(int orderOffset) {
        this.orderOffset = orderOffset;
    }

    public BigDecimal getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(BigDecimal minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    public BigDecimal getLimitOrderMinOrderAmt() {
        return limitOrderMinOrderAmt;
    }

    public void setLimitOrderMinOrderAmt(BigDecimal limitOrderMinOrderAmt) {
        this.limitOrderMinOrderAmt = limitOrderMinOrderAmt;
    }

    public BigDecimal getSellMarketMinOrderAmt() {
        return sellMarketMinOrderAmt;
    }

    public void setSellMarketMinOrderAmt(BigDecimal sellMarketMinOrderAmt) {
        this.sellMarketMinOrderAmt = sellMarketMinOrderAmt;
    }

    @Override
    public String toString() {
        return "Spot{" +
                "accountId=" + accountId +
                ", symbol='" + symbol + '\'' +
                ", baseCurrency='" + baseCurrency + '\'' +
                ", quoteCurrency='" + quoteCurrency + '\'' +
                ", pricePrecision=" + pricePrecision +
                ", amountPrecision=" + amountPrecision +
                ", totalBalance=" + totalBalance +
                ", highStrategyBalance=" + highStrategyBalance +
                ", mediumStrategyBalance=" + mediumStrategyBalance +
                ", lowStrategyBalance=" + lowStrategyBalance +
                ", strategyOffset=" + strategyOffset +
                ", orderOffset=" + orderOffset +
                ", limitOrderMinOrderAmt=" + limitOrderMinOrderAmt +
                ", sellMarketMinOrderAmt=" + sellMarketMinOrderAmt +
                ", minOrderValue=" + minOrderValue +
                '}';
    }
}
