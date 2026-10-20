package com.liugh.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.liugh.entity.AdminInfo;
import com.liugh.service.IAdminInfoService;
import com.liugh.service.ISmsCdrInfoService;

@Component
public class TTSManager extends ClientManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(TTSManager.class);
	private static TTSManager ttsManager = null;
	@Autowired
	IAdminInfoService adminInfoService;
	@Autowired
    private RedisService redisService;
	@Autowired
	private ISmsCdrInfoService smsCdrInfoService;
	/*
	 * private TTSManager() { super(); init(); }
	 */

	public void init() {
		LOGGER.info("ttsManager into load adming:" + adminInfoService);
		try {
			List<AdminInfo> admins = adminInfoService.selectList(new EntityWrapper<AdminInfo>().eq("source", 4));
			if (admins != null && !admins.isEmpty()) {
				for (AdminInfo ttsVendor : admins) {
					String threadNum = ttsVendor.getImgBase64();
					
					int num = 1;
					if (StringUtils.isNotBlank(threadNum)) {
						try {
							num = Integer.parseInt(threadNum.trim());
						} catch (Exception e) {
							num = 1;
						}
					}
					LOGGER.debug("ttsVendor num: {},{}", ttsVendor.getUsername(), num);
					for (int i = 0; i < num; i++) {
						startClient(ttsVendor);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("TTSManager init error: ", e);
		}
		

	}

	public void startClient(AdminInfo admin) {
		String host = admin.getUrl();
		int port = Integer.parseInt(admin.getPorts());
		String password = admin.getToken();
		PeeringTcpThread client = new PeeringTcpThread(host, port, password, 31, redisService, smsCdrInfoService);
		client.setClientManager(this);
		client.start();
		List<PeeringTcpThread> list = clientPool.get("tts");
		if (list == null) {
			list = new ArrayList<PeeringTcpThread>();
			clientPool.put("tts", list);
		}
		list.add(client);
	}

	/**
	 * 
	 * @return GMDManager
	 *//*
		 * public static TTSManager getInstance() { if (ttsManager == null) { ttsManager
		 * = new TTSManager(); } return ttsManager; }
		 */

}
