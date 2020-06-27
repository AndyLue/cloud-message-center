package com.assetcloud.message.center.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableCloudMessageListener {

    String NAME_SERVER_PLACEHOLDER = "${cloudmessage.name-server:}";
    String ACCESS_KEY_PLACEHOLDER = "${cloudmessage.consumer.access-key:}";
    String SECRET_KEY_PLACEHOLDER = "${cloudmessage.consumer.secret-key:}";
    String TRACE_TOPIC_PLACEHOLDER = "${cloudmessage.consumer.customized-trace-topic:}";
    String ACCESS_CHANNEL_PLACEHOLDER = "${cloudmessage.access-channel:}";
    String consumerGroup();

    String topic();

    SelectorType selectorType() default SelectorType.TAG;

    String selectorExpression() default "*";

    ConsumeMode consumeMode() default ConsumeMode.CONCURRENTLY;

    MessageModel messageModel() default MessageModel.CLUSTERING;

    int consumeThreadMax() default 64;

    long consumeTimeout() default 30000L;

    String accessKey() default ACCESS_KEY_PLACEHOLDER;

    String secretKey() default SECRET_KEY_PLACEHOLDER;

    boolean enableMsgTrace() default true;

    String customizedTraceTopic() default TRACE_TOPIC_PLACEHOLDER;

    String nameServer() default NAME_SERVER_PLACEHOLDER;

    String accessChannel() default ACCESS_CHANNEL_PLACEHOLDER;
}
