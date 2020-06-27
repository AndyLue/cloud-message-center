package com.assetcloud.message.center.config;

public class CloudMessageConfigUtils {
    public static final String ROCKETMQ_TRANSACTION_ANNOTATION_PROCESSOR_BEAN_NAME =
        "org.springframework.rocketmq.spring.starter.internalRocketMQTransAnnotationProcessor";

    public static final String ROCKETMQ_TRANSACTION_DEFAULT_GLOBAL_NAME =
        "cloudmessage_transaction_default_global_name";

    public static final String ROCKETMQ_TEMPLATE_DEFAULT_GLOBAL_NAME =
            "cloudmessageTemplate";
}
