package com.huobi;

import com.huobi.client.AccountClient;
import com.huobi.client.GenericClient;
import com.huobi.client.MarketClient;
import com.huobi.client.TradeClient;
import com.huobi.constant.HuobiOptions;

/**
 * @program: huobi-client
 * @package: com.huobi.utils
 * @author: Luping
 * @create: 8/19/21 2:37 PM
 */
public final class CurrentAPI {
    private static volatile CurrentAPI currentAPI;
    private static AccountClient accountClient;
    private static GenericClient genericClient;
    private static MarketClient marketClient;
    private static TradeClient tradeClient;


    public CurrentAPI(String apiKey, String secretKey) {
        HuobiOptions options = HuobiOptions.builder()
                .apiKey(Constants.API_KEY)
                .secretKey(Constants.SECRET_KEY)
                .build();
        accountClient = AccountClient.create(options);
        genericClient = GenericClient.create(HuobiOptions.builder().build());
        marketClient = MarketClient.create(new HuobiOptions());
        tradeClient = TradeClient.create(options);
    }

    public static CurrentAPI getApiInstance() {
        if (currentAPI == null) {
            synchronized (CurrentAPI.class) {
                if (currentAPI == null) {
                    currentAPI = new CurrentAPI(Constants.API_KEY, Constants.SECRET_KEY);
                }
            }
        }

        return currentAPI;
    }

    public AccountClient getAccountClient() {
        return CurrentAPI.accountClient;
    }

    public GenericClient getGenericClient() {
        return CurrentAPI.genericClient;
    }


    public MarketClient getMarketClient() {
        return CurrentAPI.marketClient;
    }

    public TradeClient getTradeClient() {
        return CurrentAPI.tradeClient;
    }

}
