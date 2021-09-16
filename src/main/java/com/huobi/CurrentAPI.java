package com.huobi;

import com.huobi.client.AccountClient;
import com.huobi.client.GenericClient;
import com.huobi.client.MarketClient;
import com.huobi.client.TradeClient;
import com.huobi.constant.HuobiOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @program: huobi-client
 * @package: com.huobi.utils
 * @author: Luping
 * @create: 8/19/21 2:37 PM
 */

public final class CurrentAPI {

    private static volatile CurrentAPI currentAPI_default;
    private static volatile CurrentAPI currentAPI_0;
    private static volatile CurrentAPI currentAPI_1;
    private static volatile CurrentAPI currentAPI_2;
    private static volatile CurrentAPI currentAPI_3;
    private static AccountClient accountClient;
    private static GenericClient genericClient;
    private static MarketClient marketClient;
    private static TradeClient tradeClient;
    private static Logger log = LoggerFactory.getLogger(CurrentAPI.class);

    public CurrentAPI(String apiKey, String secretKey) {
        HuobiOptions options = HuobiOptions.builder()
                .apiKey(apiKey)
                .secretKey(secretKey)
                .build();
        accountClient = AccountClient.create(options);
        genericClient = GenericClient.create(HuobiOptions.builder().build());
        marketClient = MarketClient.create(new HuobiOptions());
        tradeClient = TradeClient.create(options);
    }

    public static CurrentAPI getApiInstance(int currentStrategy) {
        CurrentAPI currentAPI;
        switch (currentStrategy) {
            case 0:
                if (currentAPI_0 == null) {
                    synchronized (CurrentAPI.class) {
                        if (currentAPI_0 == null) {
                            currentAPI_0 = new CurrentAPI(Constants.API_KEY_0, Constants.SECRET_KEY_0);
                        }
                    }
                }
                currentAPI = currentAPI_0;
                break;
            case 1:
                if (currentAPI_1 == null) {
                    synchronized (CurrentAPI.class) {
                        if (currentAPI_1 == null) {
                            currentAPI_1 = new CurrentAPI(Constants.API_KEY_1, Constants.SECRET_KEY_1);
                        }
                    }
                }
                currentAPI = currentAPI_1;
                break;
            case 2:
                if (currentAPI_2 == null) {
                    synchronized (CurrentAPI.class) {
                        if (currentAPI_2 == null) {
                            currentAPI_2 = new CurrentAPI(Constants.API_KEY_2, Constants.SECRET_KEY_2);
                        }
                    }
                }

                currentAPI = currentAPI_2;
                break;
            case 3:
                if (currentAPI_3 == null) {
                    synchronized (CurrentAPI.class) {
                        if (currentAPI_3 == null) {
                            currentAPI_3 = new CurrentAPI(Constants.API_KEY_3, Constants.SECRET_KEY_3);
                        }
                    }
                }

                currentAPI = currentAPI_3;
                break;
            default:
                if (currentAPI_default == null) {
                    synchronized (CurrentAPI.class) {
                        if (currentAPI_default == null) {
                            currentAPI_default = new CurrentAPI(Constants.API_KEY, Constants.SECRET_KEY);
                        }
                    }
                }

                currentAPI = currentAPI_default;
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
