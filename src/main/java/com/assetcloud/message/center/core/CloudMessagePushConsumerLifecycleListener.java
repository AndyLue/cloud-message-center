package com.assetcloud.message.center.core;

import com.assetcloud.message.center.support.CloudMessageConsumerLifecycleListener;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;

public interface CloudMessagePushConsumerLifecycleListener extends CloudMessageConsumerLifecycleListener<DefaultMQPushConsumer> {
}
