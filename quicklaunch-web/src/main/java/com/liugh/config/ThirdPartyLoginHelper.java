package com.liugh.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.liugh.model.ThirdPartyUser;
import com.liugh.util.HttpUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 第三方登录辅助类
 *
 * @author liugh
 * @version 2018年7月25日 下午3:44:45
 */
public final class ThirdPartyLoginHelper {

    private static Logger logger = LoggerFactory.getLogger(ThirdPartyLoginHelper.class);
    

    /**
     * 获取QQ用户信息
     *
     * @param token
     * @param openid
     */
    public static final ThirdPartyUser getQQUserinfo(String token, String openid) throws Exception {
        ThirdPartyUser user = new ThirdPartyUser();
        String url = ResourcesConfig.THIRDPARTY.getString("getUserInfoURL_qq");
        url = url + "?format=json&access_token=" + token + "&oauth_consumer_key="
                + ResourcesConfig.THIRDPARTY.getString("app_id_qq") + "&openid=" + openid;
        String res = HttpUtil.get(url);
        JSONObject json = JSON.parseObject(res);
        if (json.getIntValue("ret") == 0) {
            user.setUsername(json.getString("nickname"));
            String img = json.getString("figureurl_qq_2");
            if (img == null || "".equals(img)) {
                img = json.getString("figureurl_qq_1");
            }
            user.setToken(token);
            user.setAvatarUrl(img);
            String sex = json.getString("gender");
            if ("女".equals(sex)) {
                user.setGender("0");
            } else {
                user.setGender("1");
            }
            user.setOpenid(openid);
        } else {
            throw new IllegalArgumentException(json.getString("msg"));
        }
        return user;
    }

    /** 获取微信用户信息 */
    public static final ThirdPartyUser getWxUserinfo(String token, String openid) throws Exception {
        ThirdPartyUser user = new ThirdPartyUser();
        String url = ResourcesConfig.THIRDPARTY.getString("getUserInfoURL_wx");
        url = url + "?access_token=" + token + "&openid=" + openid;
        String res = HttpUtil.get(url);
        JSONObject json = JSON.parseObject(res);
        if (json.getString("errcode") == null) {
            user.setUsername(json.getString("nickname"));
            String img = json.getString("headimgurl");
            if (img != null && !"".equals(img)) {
                user.setAvatarUrl(img);
            }
            String sex = json.getString("sex");
            if ("0".equals(sex)) {
                user.setGender("0");
            } else {
                user.setGender("1");
            }
            user.setToken(token);
            user.setOpenid(openid);
        } else {
            throw new IllegalArgumentException(json.getString("errmsg"));
        }
        return user;
    }

    /**
     * 获取新浪用户信息
     *
     * @param token
     * @param uid
     * @return
     */
    public static final ThirdPartyUser getSinaUserinfo(String token, String uid) throws Exception {

        String url = ResourcesConfig.THIRDPARTY.getString("getUserInfoURL_sina");
        url = url + "?access_token=" + token + "&uid=" + uid;
        String res = HttpUtil.get(url);
        JSONObject json = JSON.parseObject(res);
        String name = json.getString("name");
        String nickName = StringUtils.isBlank(json.getString("screen_name")) ? name : json.getString("screen_name");
        ThirdPartyUser user =ThirdPartyUser.builder().avatarUrl(json.getString("avatar_large")).username(nickName)
                .token(token).openid(uid).provider("sina").build();
        if ("f".equals(json.getString("gender"))) {
            user.setGender("0");
        } else {
            user.setGender("1");
        }
        return user;
    }

    /**
     * 获取QQ的认证token和用户OpenID
     *
     * @param code
     * @param host
     * @return
     */
    public static final Map<String, String> getQQTokenAndOpenid(String code, String host) throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        // 获取令牌
        String tokenUrl = ResourcesConfig.THIRDPARTY.getString("accessTokenURL_qq");
        tokenUrl = tokenUrl + "?grant_type=authorization_code&client_id=" + ResourcesConfig.THIRDPARTY.getString("app_id_qq")
        + "&client_secret=" + ResourcesConfig.THIRDPARTY.getString("app_key_qq") + "&code=" + code
        + "&redirect_uri=http://" + host + ResourcesConfig.THIRDPARTY.getString("redirect_url_qq");
        String tokenRes = HttpUtil.get(tokenUrl);
        if (tokenRes != null && tokenRes.indexOf("access_token") > -1) {
            Map<String, String> tokenMap = toMap(tokenRes);
            map.put("access_token", tokenMap.get("access_token"));
            // 获取QQ用户的唯一标识openID
            String openIdUrl = ResourcesConfig.THIRDPARTY.getString("getOpenIDURL_qq");
            openIdUrl = openIdUrl + "?access_token=" + tokenMap.get("access_token");
            String openIdRes = HttpUtil.get(openIdUrl);
            int i = openIdRes.indexOf("(");
            int j = openIdRes.indexOf(")");
            openIdRes = openIdRes.substring(i + 1, j);
            JSONObject openidObj = JSON.parseObject(openIdRes);
            map.put("openId", openidObj.getString("openid"));
        } else {
            throw new IllegalArgumentException(ResourcesConfig.getMessage("THIRDPARTY.LOGIN.NOTOKEN", "QQ"));
        }
        return map;
    }

    /**
     * 获取微信的认证token和用户OpenID
     *
     * @param code
     * @param host
     * @return
     */
    public static final Map<String, String> getWxTokenAndOpenid(String code, String host) throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        // 获取令牌
        String tokenUrl = ResourcesConfig.THIRDPARTY.getString("accessTokenURL_wx");
        tokenUrl = tokenUrl + "?appid=" + ResourcesConfig.THIRDPARTY.getString("app_id_wx") + "&secret="
                + ResourcesConfig.THIRDPARTY.getString("app_key_wx") + "&code=" + code + "&grant_type=authorization_code";
        String tokenRes = HttpUtil.get(tokenUrl);
        if (tokenRes != null && tokenRes.indexOf("access_token") > -1) {
            Map<String, String> tokenMap = toMap(tokenRes);
            map.put("access_token", tokenMap.get("access_token"));
            // 获取微信用户的唯一标识openid
            map.put("openId", tokenMap.get("openid"));
        } else {
            throw new IllegalArgumentException(ResourcesConfig.getMessage("THIRDPARTY.LOGIN.NOTOKEN", "weixin"));
        }
        return map;
    }

    /**
     * 获取新浪登录认证token和用户id
     *
     * @param code
     * @param host
     * @return
     */
    public static final JSONObject getSinaTokenAndUid(String code, String host) {
        JSONObject json = null;
        try {
            // 获取令牌
            String tokenUrl = ResourcesConfig.THIRDPARTY.getString("accessTokenURL_sina");
            
            Map<String, String> map = new HashMap<String, String>();  
            map.put("client_id", ResourcesConfig.THIRDPARTY.getString("app_id_sina"));
            map.put("client_secret", ResourcesConfig.THIRDPARTY.getString("app_key_sina"));
            map.put("grant_type", "authorization_code");
            map.put("redirect_uri", "http://" + host + ResourcesConfig.THIRDPARTY.getString("redirect_url_sina"));
            map.put("code", code);        
            String tokenRes = HttpUtil.post(tokenUrl, map, null);
            // String tokenRes = httpClient(tokenUrl);
            // {"access_token":"2.00AvYzKGWraycB344b3eb242NUbiQB","remind_in":"157679999","expires_in":157679999,"uid":"5659232590"}
            if (tokenRes != null && tokenRes.indexOf("access_token") > -1) {
                json = JSON.parseObject(tokenRes);
            } else {
                throw new IllegalArgumentException(ResourcesConfig.getMessage("THIRDPARTY.LOGIN.NOTOKEN", "sina"));
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        return json;
    }    
    
    public static final int rigster2BillingSystem(String customerName, String email, String host) {
        JSONObject json = null;
        try {           
        	Map<String, String> map = new HashMap<String, String>();  
            map.put("customer_name", customerName);
            map.put("contact_mail_address", email);
            map.put("subscript_service", "smsa2p");
            String response = HttpUtil.post(host, map, null);
            // String tokenRes = httpClient(tokenUrl);
            // {"access_token":"2.00AvYzKGWraycB344b3eb242NUbiQB","remind_in":"157679999","expires_in":157679999,"uid":"5659232590"}
            logger.info("register billing system response is:"+response);
            if (response != null && response.indexOf("ssid") > -1) {
                json = JSON.parseObject(response);
                return Integer.parseInt(String.valueOf(json.get("ssid")));
            } else {
                throw new IllegalArgumentException(ResourcesConfig.getMessage("THIRDPARTY.LOGIN.NOTOKEN", "sina"));
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        return 0;
    }
    
    

    /**
     * 将格式为s1&s2&s3...的字符串转化成Map集合
     *
     * @param str
     * @return
     */
    private static final Map<String, String> toMap(String str) {
        Map<String, String> map = new HashMap<String, String>();
        String[] strs = str.split("&");
        for (String str2 : strs) {
            String[] ss = str2.split("=");
            map.put(ss[0], ss[1]);
        }
        return map;
    }

    public static String getRedirectUrl(String host , String type) {
        String url = "";
        url = ResourcesConfig.THIRDPARTY.getString("authorizeURL_" + type);
        if ("wx".equals(type)) {
            url = url + "?appid=" + ResourcesConfig.THIRDPARTY.getString("app_id_" + type) + "&redirect_uri=http://" + host
                    + ResourcesConfig.THIRDPARTY.getString("redirect_url_" + type) + "&response_type=code&scope="
                    + ResourcesConfig.THIRDPARTY.getString("scope_" + type) + "&state=fhmj";
        } else {
            url = url + "?client_id=" + ResourcesConfig.THIRDPARTY.getString("app_id_" + type) + "&response_type=code&scope="
                    + ResourcesConfig.THIRDPARTY.getString("scope_" + type) + "&redirect_uri=http://" + host
                    + ResourcesConfig.THIRDPARTY.getString("redirect_url_" + type);
        }
        return url;
    }
    
    public static void main(String[] args) {
		String a = "8a6919fc";
		System.out.println(Long.parseLong(a, 16));
	}
}
