package com.huobi;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatusEnum {
    FILLED("filled"),
    CANCELED("canceled");
    private String name;

}
