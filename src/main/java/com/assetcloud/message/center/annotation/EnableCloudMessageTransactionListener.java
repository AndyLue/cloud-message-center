package com.assetcloud.message.center.annotation;

import com.assetcloud.message.center.config.CloudMessageConfigUtils;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface EnableCloudMessageTransactionListener {

    String txProducerGroup() default CloudMessageConfigUtils.ROCKETMQ_TRANSACTION_DEFAULT_GLOBAL_NAME;

    int corePoolSize() default 1;

    int maximumPoolSize() default 1;

    long keepAliveTime() default 1000 * 60; //60ms

    int blockingQueueSize() default 2000;

    String accessKey() default "${cloudmessage.producer.access-key}";

    String secretKey() default "${cloudmessage.producer.secret-key}";
}
