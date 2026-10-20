package com.liugh.rcs.channel.infobip;

import org.springframework.stereotype.Component;

import com.liugh.util.HttpUtil;

@Component
public class DefaultInfobipHttpExecutor implements InfobipHttpExecutor {

    @Override
    public String post(String url, String authorization, String body) {
        return HttpUtil.postJsonWithAuthorization(url, body, authorization);
    }
}
