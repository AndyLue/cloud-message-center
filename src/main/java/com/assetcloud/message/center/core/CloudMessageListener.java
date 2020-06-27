package com.assetcloud.message.center.core;

public interface CloudMessageListener<T> {
    void onMessage(T message);
}
