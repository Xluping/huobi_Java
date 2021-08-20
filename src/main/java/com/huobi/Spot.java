package com.huobi;

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 8/19/21 1:35 PM
 */
public class Spot {
    // BTC/USDT
    private Long accountId;
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

    public Spot() {

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

    @Override
    public String toString() {
        return "Spot{" +
                "accountId=" + accountId +
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
                '}';
    }
}
