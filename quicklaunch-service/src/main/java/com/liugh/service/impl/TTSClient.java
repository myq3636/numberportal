package com.liugh.service.impl;

import org.springframework.stereotype.Component;

/**
 * 
 * gmd client thread
 */
public abstract class TTSClient implements java.lang.Runnable {
    protected boolean keepRunning;
    protected ClientManager clientManager = null;

    public TTSClient() {
    }

    public synchronized void start() {
        Thread thread = new Thread(this);
        keepRunning = true;
        thread.start();
    }

    public synchronized void start(String name) {
        Thread thread = new Thread(this);
        keepRunning = true;
        thread.setName(name);
        thread.start();
    }

    public synchronized void stop() {
        keepRunning = false;
    }

    public boolean isRunning() {
        return keepRunning;
    }

    public void setClientManager(ClientManager clientManager) {
        this.clientManager = clientManager;
    }
}