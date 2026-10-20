package com.liugh.rcs.channel.infobip;

public interface InfobipHttpExecutor {

    String post(String url, String authorization, String body);
}
