package com.huobi;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SymbolPartionEnum {

    //     *  main 主板-主区
    //     *  innovation 创业板
    //     *  potentials 观察区
    //     *  pioneer  新币
    MAIN(1, "main"),
    INNOVATION(2, "innovation"),
    POTENTIALS(3, "potentials"),
    PIONEER(4, "pioneer");
    private int id;
    private String name;
}
