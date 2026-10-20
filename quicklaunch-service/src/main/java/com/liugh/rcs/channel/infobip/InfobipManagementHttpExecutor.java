package com.liugh.rcs.channel.infobip;

/**
 * HTTP boundary for Infobip RCS service-management APIs.
 */
public interface InfobipManagementHttpExecutor {

    String request(String method, String url, String authorization, String body);

    String upload(String url, String authorization, String fileName,
            String contentType, byte[] content);
}
