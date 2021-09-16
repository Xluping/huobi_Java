# v1
<li>1. 目前只支持以 USDT 计价的币种
<li>2. 高频,稳健,保守 三档区间的 USDT 配比 8:1:1,
    补仓区间30%,50%,70%,
    策略区间内下单次数, 
    止盈点位等,都可在 Constants 中修改
<li>3. 买入的时候 传USDT, 卖出的时候传基础币的数量(比如 1 个 BTC) 
<li>4. 下单时,有最小下单量的限制. 下单后将订单号存到 map, 定时任务轮巡订单状态, 订单成交后,挂卖单(sell limit)
<li>5. 做不同的币种,需要再不同的文件夹下执行 java -jar ****.jar  避免日志混淆.
<li>6. 修改Constants,实现不同的下单策略
 三个区间内的资金配比,总和为 1
 高频: 资金集中在 HIGH_RATIO
 稳健: 资金集中在 MEDIUM_RATIO
 保守: 资金集中在 LOW_RATIO
 HIGH_RATIO = 0.8;
 MEDIUM_RATIO = 0.1;
 LOW_RATIO = 0.1;
 
 HIGH_RANGE 下跌区间, 
 HIGH_COUNT 补仓次数
 SELL_OFFSET 止盈点
 假设: 
 HIGH_RANGE=50 表示币价在当前价格 与 下跌 50%的区间内下单
 HIGH_COUNT=10 表示在上面区间内,下单次数
   50/10 = 5 次, 下跌 50%的过程中,下单频率为每跌 5% 补仓一次
 SELL_OFFSET=1.02 表示在每次下单的价格基础上,涨 2% 就卖掉.
 <li>7. 高频策略: 做单频率快,有波动就有收益,适合横盘行情
        稳健策略: 做单频率适中,适合涨跌幅较小的行情
        保守策略: 适合大涨大跌的
        
 <li>8.  startup.sh  在服务器运行时用不同的 jar 包,选择不同的端口号
 
# #!/bin/bash
 cd ht/
 java -jar -Dserver.port=8080 ht.jar &
 
 cd ../ont/
 java -jar -Dserver.port=8081 ont.jar &
  
<li>9. 启动时,市场价下单
<li>10. <br>
root/<br>
    -- cp.sh<br>
    -- huobi-command-util.jar<br>
    -- serverStartup.sh<br>
    -- jars <br>
        -- m1/m1-jar-with-dependencies<br>
        -- m2/m2-jar-with-dependencies<br>
        -- ...<br>
<li>11.  
本地运行: nohup java -jar spot-jar-with-dependencies.jar spot usdt
server 运行:<br>
    1. stopall
    2. upload spot-jar-with-dependencies.jar to /root/ <br>
    3. sh cp.sh <br>
    4. if  huobi-command-util.jar is not running, start it: (alias  startutil) <br>
    
<li> //todo: websocket 获得最新价格,放到kafka上, spot1 spot2 ... 进行消费
<li> //todo: 持久化存储订单数据

# v2
websocket 价格监听,订单状态监听.

# v3
1. 按板块筛选
2. 按照过去 * 个小时的收盘价选择, 持续跌的纳入考虑 (获取到的数据是以新加坡时间为准)
3. 平均分配可用余额
4. x 小时内,不重复下单.