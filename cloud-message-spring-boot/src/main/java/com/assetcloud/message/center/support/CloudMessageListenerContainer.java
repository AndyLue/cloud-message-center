package com.assetcloud.message.center.support;

import com.assetcloud.message.center.core.CloudMessageListener;
import org.springframework.beans.factory.DisposableBean;

public interface CloudMessageListenerContainer extends DisposableBean {

    void setupMessageListener(CloudMessageListener<?> messageListener);
}
