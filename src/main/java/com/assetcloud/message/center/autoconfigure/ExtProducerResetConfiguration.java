package com.assetcloud.message.center.autoconfigure;

import com.assetcloud.message.center.annotation.ExtCloudMessageTemplateConfiguration;
import com.assetcloud.message.center.core.CloudMessageTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;


@Configuration
public class ExtProducerResetConfiguration implements ApplicationContextAware, SmartInitializingSingleton {
    private final static Logger log = LoggerFactory.getLogger(ExtProducerResetConfiguration.class);

    private ConfigurableApplicationContext applicationContext;

    private StandardEnvironment environment;

    private CloudMessageProperties cloudMessageProperties;

    private ObjectMapper objectMapper;

    public ExtProducerResetConfiguration(ObjectMapper rocketMQMessageObjectMapper,
                                             StandardEnvironment environment,
                                             CloudMessageProperties cloudMessageProperties) {
        this.objectMapper = rocketMQMessageObjectMapper;
        this.environment = environment;
        this.cloudMessageProperties = cloudMessageProperties;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, Object> beans = this.applicationContext.getBeansWithAnnotation(ExtCloudMessageTemplateConfiguration.class);

        if (Objects.nonNull(beans)) {
            beans.forEach(this::registerTemplate);
        }
    }

    private void registerTemplate(String beanName, Object bean) {
        Class<?> clazz = AopProxyUtils.ultimateTargetClass(bean);

        if (!CloudMessageTemplate.class.isAssignableFrom(bean.getClass())) {
            throw new IllegalStateException(clazz + " is not instance of " + CloudMessageTemplate.class.getName());
        }

        ExtCloudMessageTemplateConfiguration annotation = clazz.getAnnotation(ExtCloudMessageTemplateConfiguration.class);
        GenericApplicationContext genericApplicationContext = (GenericApplicationContext) applicationContext;
        validate(annotation, genericApplicationContext);

        DefaultMQProducer mqProducer = createProducer(annotation);
        // Set instanceName same as the beanName
        mqProducer.setInstanceName(beanName);
        try {
            mqProducer.start();
        } catch (MQClientException e) {
            throw new BeanDefinitionValidationException(String.format("Failed to startup MQProducer for RocketMQTemplate {}",
                    beanName), e);
        }
        CloudMessageTemplate cloudMessageTemplate = (CloudMessageTemplate) bean;
        cloudMessageTemplate.setProducer(mqProducer);
        cloudMessageTemplate.setObjectMapper(objectMapper);


        log.info("Set real producer to :{} {}", beanName, annotation.value());
    }

    private DefaultMQProducer createProducer(ExtCloudMessageTemplateConfiguration annotation) {
        DefaultMQProducer producer = null;

        CloudMessageProperties.Producer producerConfig = cloudMessageProperties.getProducer();
        if (producerConfig == null) {
            producerConfig = new CloudMessageProperties.Producer();
        }
        String nameServer = environment.resolvePlaceholders(annotation.nameServer());
        String groupName = environment.resolvePlaceholders(annotation.group());
        groupName = StringUtils.isEmpty(groupName) ? producerConfig.getGroup() : groupName;

        String ak = environment.resolvePlaceholders(annotation.accessKey());
        ak = StringUtils.isEmpty(ak) ? producerConfig.getAccessKey() : annotation.accessKey();
        String sk = environment.resolvePlaceholders(annotation.secretKey());
        sk = StringUtils.isEmpty(sk) ? producerConfig.getSecretKey() : annotation.secretKey();
        String customizedTraceTopic = environment.resolvePlaceholders(annotation.customizedTraceTopic());
        customizedTraceTopic = StringUtils.isEmpty(customizedTraceTopic) ? producerConfig.getCustomizedTraceTopic() : customizedTraceTopic;

        if (!StringUtils.isEmpty(ak) && !StringUtils.isEmpty(sk)) {
            producer = new DefaultMQProducer(groupName, new AclClientRPCHook(new SessionCredentials(ak, sk)),
                    annotation.enableMsgTrace(), customizedTraceTopic);
            producer.setVipChannelEnabled(false);
        } else {
            producer = new DefaultMQProducer(groupName, annotation.enableMsgTrace(), customizedTraceTopic);
        }

        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(annotation.sendMessageTimeout() == -1 ? producerConfig.getSendMessageTimeout() : annotation.sendMessageTimeout());
        producer.setRetryTimesWhenSendFailed(annotation.retryTimesWhenSendAsyncFailed() == -1 ? producerConfig.getRetryTimesWhenSendFailed() : annotation.retryTimesWhenSendAsyncFailed());
        producer.setRetryTimesWhenSendAsyncFailed(annotation.retryTimesWhenSendAsyncFailed() == -1 ? producerConfig.getRetryTimesWhenSendAsyncFailed() : annotation.retryTimesWhenSendAsyncFailed());
        producer.setMaxMessageSize(annotation.maxMessageSize() == -1 ? producerConfig.getMaxMessageSize() : annotation.maxMessageSize());
        producer.setCompressMsgBodyOverHowmuch(annotation.compressMessageBodyThreshold() == -1 ? producerConfig.getCompressMessageBodyThreshold() : annotation.compressMessageBodyThreshold());
        producer.setRetryAnotherBrokerWhenNotStoreOK(annotation.retryNextServer());

        return producer;
    }

    private void validate(ExtCloudMessageTemplateConfiguration annotation, GenericApplicationContext genericApplicationContext) {
        if (genericApplicationContext.isBeanNameInUse(annotation.value())) {
            throw new BeanDefinitionValidationException(String.format("Bean {} has been used in Spring Application Context, " +
                            "please check the @ExtRocketMQTemplateConfiguration",
                    annotation.value()));
        }

        if (cloudMessageProperties.getNameServer() == null ||
                cloudMessageProperties.getNameServer().equals(environment.resolvePlaceholders(annotation.nameServer()))) {
            throw new BeanDefinitionValidationException(
                    "Bad annotation definition in @ExtRocketMQTemplateConfiguration, nameServer property is same with " +
                            "global property, please use the default RocketMQTemplate!");
        }
    }
}