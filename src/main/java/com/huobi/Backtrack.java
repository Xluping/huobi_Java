package com.huobi;

import com.huobi.client.req.market.SubMarketTradeRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * @author: luping
 * @date: 9/13/21
 * @project_name: huobi-client
 * @package_name: com.huobi
 * 获取实时成交价格,用来回测
 **/
@Slf4j
public class Backtrack {
    static String symbol = "csprusdt";
    static int API_CODE = 1000;

    public static void main(String[] args) {
        getPrice();
    }

    @SneakyThrows
    public static void getPrice() {

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("./logs/price.txt", true)), 1024);

        CurrentAPI.getApiInstance(API_CODE).getMarketClient().subMarketTrade(SubMarketTradeRequest.builder().symbol(symbol).build(), (marketTradeEvent) -> {
            marketTradeEvent.getList().forEach(marketTrade -> {
                log.info("======Backtrack.getPrice : {} ======", marketTrade.getPrice());
                try {
                    bw.write(marketTrade.getPrice().toString());
                    bw.newLine();

                } catch (IOException e) {
                    log.error("======Backtrack.getPrice : {} ======", e.getMessage());
                }
            });

            try {
                bw.flush();
            } catch (IOException e) {
                log.error("======Backtrack.getPrice : {} ======", e.getMessage());
            }

        });

    }


}
