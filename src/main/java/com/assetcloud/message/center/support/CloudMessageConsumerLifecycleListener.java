package com.assetcloud.message.center.support;

public interface CloudMessageConsumerLifecycleListener<T> {
    void prepareStart(final T consumer);
}
