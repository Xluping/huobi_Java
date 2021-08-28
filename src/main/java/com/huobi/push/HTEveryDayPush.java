package com.huobi.push;

import com.huobi.HuobiUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 8/27/21 10:42 PM
 */
public class HTEveryDayPush implements Job {
    private Long spotAccountId = 14086863L;
    private String baseCurrency = "ht";
    private String quoteCurrency = "usdt";

    public HTEveryDayPush() {
    }


    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String msg = HuobiUtil.getBalance4Push(spotAccountId, baseCurrency, quoteCurrency);
        HuobiUtil.weChatPusher(msg, 2);
    }

    public void setSpotAccountId(Long spotAccountId) {
        this.spotAccountId = spotAccountId;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public void setQuoteCurrency(String quoteCurrency) {
        this.quoteCurrency = quoteCurrency;
    }
}
