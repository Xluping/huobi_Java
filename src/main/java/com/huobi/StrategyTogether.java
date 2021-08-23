package com.huobi;

import com.huobi.constant.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
    private BigDecimal portionHigh;
    private BigDecimal portionMedium;
    private BigDecimal portionLow;

    public StrategyTogether() {
    }


    public void setSpot(Spot spot) {
        this.spot = spot;
        this.portionHigh = spot.getHighStrategyBalance().divide(new BigDecimal(String.valueOf(Constants.HIGH_COUNT)), RoundingMode.HALF_UP);
        this.portionHigh = portionHigh.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);

        this.portionMedium = spot.getMediumStrategyBalance().divide(new BigDecimal(String.valueOf(Constants.MEDIUM_COUNT)), RoundingMode.HALF_UP);
        this.portionMedium = portionMedium.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);

        this.portionLow = spot.getLowStrategyBalance().divide(new BigDecimal(String.valueOf(Constants.LOW_COUNT)), RoundingMode.HALF_UP);
        this.portionLow = portionLow.setScale(spot.getPricePrecision(), RoundingMode.HALF_UP);

    }

    public synchronized void launch(BigDecimal usdtBalance) {
        StrategyCommon.reset();
        HuobiUtil.weChatPusher("策略启动: " + spot.toString(), 1);
        logger.error(spot.toString());
        logger.error("====== StrategyTogether-策略启动 ======");
        logger.error("====== StrategyTogether-高频策略每次补仓份额: " + portionHigh + " USDT ======");
        logger.error("====== StrategyTogether-稳健策略每次补仓份额: " + portionMedium + " USDT ======");
        logger.error("====== StrategyTogether-保守策略每次补仓份额: " + portionLow + " USDT ======");
        BigDecimal currentTradPrice = HuobiUtil.getCurrentTradPrice(spot.getSymbol());
        logger.error("====== StrategyTogether-launch: " + currentTradPrice + "======");
        StrategyCommon.calculateBuyPriceList(currentTradPrice, spot.getPricePrecision());

        // 启动后,根据当前价格下单 buy .
        if (usdtBalance.compareTo(portionHigh) >= 0) {
            SpotBuyerLocal.setInsufficientFound(false);
            StrategyCommon.placeBuyOrder(spot, currentTradPrice, portionHigh);
        } else {
            logger.error("====== StrategyTogether-launch: 所剩 usdt 余额不足,等待卖单成交 " + usdtBalance.toString() + " ======");
            SpotBuyerLocal.setInsufficientFound(true);
        }

    }

}
