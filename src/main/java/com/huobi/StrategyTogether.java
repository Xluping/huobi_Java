package com.huobi;

import com.huobi.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 8/19/21 2:08 PM
 * <p>
 * 0%-20%区间 分成 10 份下单
 */
public class StrategyTogether extends BaseStrategy {
    Logger logger = LoggerFactory.getLogger(StrategyTogether.class);
    private Spot spot;
    private double portionHigh;
    private double portionMedium;
    private double portionLow;

    public StrategyTogether() {
    }


    public void setSpot(Spot spot) {
        this.spot = spot;
        this.portionHigh = spot.getHighStrategyBalance() / Constants.HIGH_COUNT;
        this.portionMedium = spot.getMediumStrategyBalance() / Constants.MEDIUM_COUNT;
        this.portionLow = spot.getLowStrategyBalance() / Constants.LOW_COUNT;
    }

    public synchronized void launch(BigDecimal usdtBalance) {
        HuobiUtil.weChatPusher("策略启动: " + spot.toString(), 1);
        logger.error(spot.toString());
        logger.error("====== 策略启动 ======");
        logger.error("====== 高频策略每次补仓份额: " + portionHigh + " USDT ======");
        logger.error("====== 稳健策略每次补仓份额: " + portionMedium + " USDT ======");
        logger.error("====== 保守策略每次补仓份额: " + portionLow + " USDT ======");
        BigDecimal currentTradPrice = HuobiUtil.getCurrentTradPrice(spot.getSymbol());
        logger.error("====== launch: " + currentTradPrice + "======");
        StrategyCommon.calculateBuyPriceList(currentTradPrice, spot.getPricePrecision());

        // 启动后,根据当前价格下单 buy .
        BigDecimal usdt = new BigDecimal(portionHigh);
        if (usdtBalance.compareTo(usdt) >= 0) {
            SpotBuyer.setInsufficientFound(false);
            StrategyCommon.placeBuyOrder(spot, currentTradPrice, usdt);
        } else {
            logger.error("====== launch: 所剩 usdt 余额不足,等待卖单成交 " + usdtBalance.toString() + " ======");
            SpotBuyer.setInsufficientFound(true);
        }

    }

}
