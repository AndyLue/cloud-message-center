package com.assetcloud.message.center.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ExtCloudMessageTemplateConfiguration {
    String value() default "";

    String nameServer();

    String group() default "${cloudmessage.producer.group:}";
    int sendMessageTimeout() default -1;
    int compressMessageBodyThreshold() default -1;
    int retryTimesWhenSendFailed() default -1;
    int retryTimesWhenSendAsyncFailed() default -1;
    boolean retryNextServer() default false;
    int maxMessageSize() default -1;
    String accessKey() default "${cloudmessage.producer.accessKey:}";
    String secretKey() default "${cloudmessage.producer.secretKey:}";
    boolean enableMsgTrace() default true;
    String customizedTraceTopic() default "${cloudmessage.producer.customized-trace-topic:}";
}