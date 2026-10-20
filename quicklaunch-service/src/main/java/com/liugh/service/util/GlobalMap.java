package com.liugh.service.util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.SmsCdrInfo;
import com.liugh.rcs.service.RcsRoutingSnapshot;

public class GlobalMap {
	
	public static Map<String, AdminInfo> admin = new ConcurrentHashMap<String, AdminInfo>();
	public static Map<String, AdminInfo> rcsadmin = new ConcurrentHashMap<String, AdminInfo>();
	public static Map<String, AdminInfo> adminToken = new ConcurrentHashMap<String, AdminInfo>();
	public static Table<Integer, String, Integer> routingTable = HashBasedTable.create();
	/** RCS 专用权重路由快照（volatile，整体重建原子替换；SMS 仍用 routingTable）。 */
	public static volatile RcsRoutingSnapshot rcsRoutingSnapshot;
	public static long smsCount;
	public static Map<String, Long> clientCount = new ConcurrentHashMap<String, Long>();
	public static Map<String, Long> clientLastSendTime = new ConcurrentHashMap<String, Long>();
	public static long lastSendClient;
	public static int lastSendClientIndex;
	public static int cmiSMSCount=0;
	public static Set<Long> waClients = new HashSet();
	
	/**
     * 不需要延迟处理(泛型的方式封装)
     * @return
     */
    public static  LoadingCache<String , SmsCdrInfo> ttsMsgCache= CacheBuilder
          .newBuilder()
          .maximumSize(100000)
          .weakKeys()
          .softValues()
          .refreshAfterWrite(120, TimeUnit.SECONDS)
          .expireAfterWrite(100, TimeUnit.MINUTES)        
					/*
					 * .removalListener(new RemovalListener<K, V>(){
					 * 
					 * @Override public void onRemoval(RemovalNotification<K, V> rn) {
					 * System.out.println(rn.getKey()+"被移除"); }})
					 */
          .build(new CacheLoader<String, SmsCdrInfo>(){
              @Override
              public SmsCdrInfo load(String s) throws Exception {
                  return null;
              }
          }); 
    
    public static synchronized int getCMICount() {
    	cmiSMSCount++;
    	if(cmiSMSCount == 65536) {
    		cmiSMSCount = 1;
    	}
    	return cmiSMSCount;
    }
	
}
