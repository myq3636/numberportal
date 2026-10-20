package com.liugh.service.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.liugh.entity.SmsCdrInfo;
import com.liugh.service.entity.LinkList;
@Component
public abstract class ClientManager extends TimerTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientManager.class);
    protected LinkList messageList = null;
    protected Map<String, List<PeeringTcpThread>> clientPool = null;
    protected Timer timer = new Timer();
    protected int interval = 5 * 60 * 1000;
    protected String currentSendMsgPool = "master";

    public ClientManager() {
        messageList = new LinkList();
        clientPool = new HashMap<String, List<PeeringTcpThread>>();
        timer.schedule(this, interval, interval);
    }

    /**
     * 
     * @param message
     *            Message
     */
    public synchronized void put(SmsCdrInfo sms) {
        try {
            while (messageList.size() > 10000) {
                wait(1000);
            }
            // log.info(message,"message is put into "+this.getClass().getSimpleName());
            messageList.put(sms);

            LOGGER.info("put message into message buffer, size is:{} message id is:{}"
                    ,messageList.size(),sms.getMsgId());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("put message into message List:{}",
                        sms.toString());
            }
            notifyAll();
        } catch (Exception e) {
            LOGGER.error("put rcc message into message list exception", e);
        }
    }

    /**
     * 
     * @return Message
     */
    public SmsCdrInfo get() {
        return (SmsCdrInfo) messageList.get();
    }

    /**
     * valid client
     */
    private synchronized void validClient() {
        Iterator<List<PeeringTcpThread>> iterator = clientPool.values().iterator();
        while (iterator.hasNext()) {
        	List<PeeringTcpThread> threads =  iterator.next();
        	if (threads!=null && !threads.isEmpty()) {
        		for (TTSClient client: threads) {
        			if (!client.isRunning()) {
        				LOGGER.warn("valid Client start....");
                        client.stop();
                        client.start();
                    }
				}        		
			}
            
        }
    }

    public void run() {
        validClient();
    }

    public abstract void init();

	public synchronized String getCurrentSendMsgPool() {
		return currentSendMsgPool;
	}

	public synchronized void setCurrentSendMsgPool(String currentSendMsgPool) {
		this.currentSendMsgPool = currentSendMsgPool;
	}        

}
