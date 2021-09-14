package com.huobi;

import com.huobi.client.AccountClient;
import com.huobi.client.req.account.SubAccountUpdateRequest;
import com.huobi.constant.HuobiOptions;
import com.huobi.constant.enums.AccountUpdateModeEnum;
import com.huobi.model.account.Account;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @program: huobi-client
 * @package: com.huobi
 * @author: Luping
 * @create: 9/13/21 10:45 AM
 */
@Slf4j
public class Test {
    public static void main(String[] args) {
        AccountClient accountService = CurrentAPI.getApiInstance().getAccountClient();
        accountService.subAccountsUpdate(SubAccountUpdateRequest.builder()
                .accountUpdateMode(AccountUpdateModeEnum.ACCOUNT_CHANGE).build(), event -> {
            log.info("====== Test-main: {} ======", event.toString());

        });


    }
}
