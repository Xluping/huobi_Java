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


    @Override
    public void execute(JobExecutionContext context) {
        filter();
    }

    /**
     * 二次过滤
     */
    public static ConcurrentHashMap<String, Spot> filter() {
        AtomicBoolean qualified = new AtomicBoolean(false);
        ConcurrentHashMap<String, Spot> finalSymbolMap = new ConcurrentHashMap<>();
        // 第二次过滤symbol, 获取每个symbol 的 closePrice 数组,
        SpotRandom.getSymbolList().forEach(symbolStr -> {
            List<Candlestick> klineList = CurrentAPI.getApiInstance().getMarketClient().getCandlestick(CandlestickRequest.builder()
                    .symbol(symbolStr)
                    .interval(SpotRandom.getCandlestickIntervalEnum())
                    .size(SpotRandom.getNumberOfCandlestick())
                    .build());
            // 按照过去30分钟蜡烛图来看,筛选出连续4*30分钟 下跌的symbol
            StringBuilder sb = new StringBuilder();
            BigDecimal basePrice = klineList.get(0).getClose();
            sb.append(basePrice.toString()).append(" --> ");

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
        });

        return finalSymbolMap;
    }

}
