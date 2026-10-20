package com.liugh.service.entity;

import java.util.Collection;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("rawtypes")
public class LinkList extends LinkedList {
    private static final long serialVersionUID = -7908614322974917670L;
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkList.class);

    public LinkList() {
        super();
    }

    /**
     * put
     * 
     * @param obj
     *            Object
     */
    @SuppressWarnings("unchecked")
    public synchronized void put(Object obj) {
        if (size() == 0) {
            add(obj);
            notifyAll();
        } else {
            addLast(obj);
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized void putAll(Collection obj) {
        if (size() == 0) {
            addAll(obj);
            notifyAll();
        } else {
            addAll(obj);
        }
    }

    /**
     * get
     * 
     * @return Object
     */
    public synchronized Object get() {
        long waittime = 500;
        try {
            if (size() == 0) {
                wait(waittime);
            }
            if (size() > 0) {
                return removeFirst();
            }
        } catch (NoSuchElementException e) {
            LOGGER.warn("NoElementException in LinkList", e);
        } catch (Exception e) {
            LOGGER.warn("Exception", e);
        }
        return null;
    }

    /**
     * get size
     * 
     * @return int
     */
    public synchronized int getSize() {
        return size();
    }

}
