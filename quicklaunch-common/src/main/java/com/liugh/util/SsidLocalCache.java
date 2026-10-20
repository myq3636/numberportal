package com.liugh.util;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class SsidLocalCache {
	 public static Cache<String, Object> cache = CacheBuilder.newBuilder()			              
			              .recordStats()
			              .build();
	 //key: filename, value:true
	 public static Map<String, String> fileSendingMap = new ConcurrentHashMap<String, String>();
	 
	 //routing flag <routing, true>
	 public static Cache<String, Object> routingFlagCache = CacheBuilder.newBuilder()			              
			 .expireAfterWrite(10, TimeUnit.MINUTES)
             .build();
}
