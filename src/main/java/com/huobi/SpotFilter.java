package com.huobi;

import com.huobi.client.req.market.CandlestickRequest;
import com.huobi.model.market.Candlestick;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 9/15/21 11:34 AM
 */
public class SpotFilter implements Job {

    static Logger log = LoggerFactory.getLogger(SpotFilter.class);
    /**
     * timeLimit 小时内不重复下单
     */
    private static final double TIME_LIMIT = 3.0;


    /**
     * 二次过滤
     * K线周期以 新加坡时间 为基准
     */
    public static ConcurrentHashMap<String, Spot> filter(int apiCode) {
        log.info("====== SpotFilter-filter  : 开始过滤 ======");
        ConcurrentHashMap<String, Long> orderHistory = SpotRandom.getOrderHistory();
        AtomicBoolean qualified = new AtomicBoolean(false);
        ConcurrentHashMap<String, Spot> finalSymbolMap = new ConcurrentHashMap<>();
        // 第二次过滤symbol, 获取每个symbol 的 closePrice 数组,
        SpotRandom.getSymbolList().forEach(symbolStr -> {
            // 找不到的话,返回null
            Long buyTime = orderHistory.get(symbolStr);
            Long currentTime = System.currentTimeMillis();
            if (!withinTimeLimit(buyTime, currentTime, TIME_LIMIT)) { //最近没下过单
                orderHistory.remove(symbolStr);
                List<Candlestick> klineList = CurrentAPI.getApiInstance(apiCode).getMarketClient().getCandlestick(CandlestickRequest.builder()
                        .symbol(symbolStr)
                        .interval(SpotRandom.getCandlestickIntervalEnum())
                        .size(SpotRandom.getNumberOfCandlestick())
                        .build());
                // 按照过去30分钟蜡烛图来看,筛选出连续4*30分钟 下跌的symbol
                StringBuilder sb = new StringBuilder();
                BigDecimal basePrice = klineList.get(0).getClose();
                sb.append(basePrice.toString()).append(" --> ");
                // 从K线上判断是否符合要求
                for (int i = 1; i < klineList.size(); i++) {
                    sb.append(klineList.get(i).getClose());

                    if (basePrice.compareTo(klineList.get(i).getClose()) < 0) {

                        qualified.set(false);
                        break;
                    }
                    if (i != klineList.size() - 1) {
                        sb.append(" --> ");
                    }
                    qualified.set(true);

                    basePrice = klineList.get(i).getClose();
                }

                log.info("====== SpotFilter.filter: symbol: {}, closePrice: {}, qualified: {} ======", symbolStr, sb.toString(), qualified);
                if (qualified.get()) {
                    finalSymbolMap.put(symbolStr, new Spot());
                }
                qualified.set(false);
            } else {
                log.info("====== SpotFilter-filter : {} 距上次下单时间不超过 {} 小时, SKIP ======", symbolStr, TIME_LIMIT);
            }

        });

        return finalSymbolMap;
    }

    @Override
    public void execute(JobExecutionContext context) {
        filter(0);
    }

    /**
     * @return 判断时间是否在 timeLimit 之内
     */
    public static boolean withinTimeLimit(Long buyTime, Long currentTime, double timeLimit) {
        if (buyTime == null || currentTime == null) {
            return false;
        }
        long diff = currentTime - buyTime;

        if (diff < 0) {

            return false;

        }
        double result = diff * 1.0 / (1000 * 60 * 60);

        return result <= timeLimit;

    }
}
