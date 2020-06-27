package com.assetcloud.message.center.config;

import com.assetcloud.message.center.core.CloudMessageLocalTransactionListener;
import org.apache.rocketmq.remoting.RPCHook;
import org.springframework.beans.factory.BeanFactory;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class TransactionHandler {
    private String name;
    private String beanName;
    private CloudMessageLocalTransactionListener bean;
    private BeanFactory beanFactory;
    private ThreadPoolExecutor checkExecutor;
    private RPCHook rpcHook;

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RPCHook getRpcHook() {
        return rpcHook;
    }

    public void setRpcHook(RPCHook rpcHook) {
        this.rpcHook = rpcHook;
    }

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public void setListener(CloudMessageLocalTransactionListener listener) {
        this.bean = listener;
    }

    public CloudMessageLocalTransactionListener getListener() {
        return this.bean;
    }

    public void setCheckExecutor(int corePoolSize, int maxPoolSize, long keepAliveTime, int blockingQueueSize) {
        this.checkExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize,
            keepAliveTime, TimeUnit.MILLISECONDS,
            new LinkedBlockingDeque<>(blockingQueueSize));
    }

    public ThreadPoolExecutor getCheckExecutor() {
        return checkExecutor;
    }
}
