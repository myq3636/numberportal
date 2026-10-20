package com.liugh.service.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

@Service
public class RedisService {
	@Resource
	private StringRedisTemplate stringRedisTemplate;
	public void setString(String key, String value){
		ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
		valueOperations.set(key, value);
	}
	
	public long setList(String key, String value){
		ListOperations<String, String> listOperations = stringRedisTemplate.opsForList();
		return listOperations.leftPush(key, value);
	}
	/**
	* get redis:list类型
	* @param key key
	* @param start start
	* @param end end
	* @return
	*/
	public String rightPop(String key){
		return stringRedisTemplate.opsForList().rightPop(key, 1, TimeUnit.SECONDS);
	}

}
