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


    public Spot() {

    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
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

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }

    public BigDecimal getHighStrategyBalance() {
        return highStrategyBalance;
    }

    public void setHighStrategyBalance(BigDecimal highStrategyBalance) {
        this.highStrategyBalance = highStrategyBalance;
    }

    public BigDecimal getMediumStrategyBalance() {
        return mediumStrategyBalance;
    }

    public void setMediumStrategyBalance(BigDecimal mediumStrategyBalance) {
        this.mediumStrategyBalance = mediumStrategyBalance;
    }

    public BigDecimal getLowStrategyBalance() {
        return lowStrategyBalance;
    }

    public void setLowStrategyBalance(BigDecimal lowStrategyBalance) {
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

    public BigDecimal getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(BigDecimal minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    public BigDecimal getPortionHigh() {
        return portionHigh;
    }

    public void setPortionHigh(BigDecimal portionHigh) {
        this.portionHigh = portionHigh;
    }

    public BigDecimal getPortionMedium() {
        return portionMedium;
    }

    public void setPortionMedium(BigDecimal portionMedium) {
        this.portionMedium = portionMedium;
    }

    public BigDecimal getPortionLow() {
        return portionLow;
    }

    public void setPortionLow(BigDecimal portionLow) {
        this.portionLow = portionLow;
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
                ", portionHigh=" + portionHigh +
                ", portionMedium=" + portionMedium +
                ", portionLow=" + portionLow +
                '}';
    }
}
