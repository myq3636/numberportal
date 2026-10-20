/**
 * <p> Title: </p>
 * <p> Description: </p>
 * @author Allen Wang
 * @date Apr 1, 2016
 */
package com.liugh.service.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CountryInfoDictCacheManager<K,V> {
	
	private static final Logger LOG = LoggerFactory.getLogger(CountryInfoDictCacheManager.class);
	
	
	private final Map<K, V> cache;
	
	@SuppressWarnings("rawtypes")
	private static CountryInfoDictCacheManager instance = null;
	
	private static final Object syncLock = new Object();
	
	private CountryInfoDictCacheManager() {
		cache = new ConcurrentHashMap<K, V>();
	}
	
	@SuppressWarnings("rawtypes")
	public static CountryInfoDictCacheManager getInstance() {
		if(instance == null) {
			synchronized(syncLock) {
				if(instance == null)
					instance = new CountryInfoDictCacheManager();
			}
		}
		return instance;
	}
	
	public void put(K cacheKey, V cacheValue) {
		cache.put(cacheKey, cacheValue);
	}
	
	public V get(K cacheKey) {
		return cache.get(cacheKey);
	}
	
	public synchronized void clear() {
		cache.clear();
	}
	
	public void evict(K cacheKey) {
		if(cache.containsKey(cacheKey)) {
			cache.remove(cacheKey);
		}
	}

}
