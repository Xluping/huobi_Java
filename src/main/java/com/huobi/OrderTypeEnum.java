package com.huobi;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderTypeEnum {
    BUY_MARKET("buy-market"),
    BUY_LIMIT("buy-limit"),
    SELL_MARKET("sell-market"),
    SELL_LIMIT("sell-limit");

    private String name;

}
