package com.assetcloud.message.center.autoconfigure;

import com.assetcloud.message.center.annotation.ConsumeMode;
import com.assetcloud.message.center.annotation.MessageModel;
import com.assetcloud.message.center.annotation.EnableCloudMessageListener;
import com.assetcloud.message.center.core.CloudMessageListener;
import com.assetcloud.message.center.support.DefaultCloudMessageListenerContainer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.AccessChannel;
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
import java.util.concurrent.atomic.AtomicLong;


@Configuration
public class ListenerContainerConfiguration implements ApplicationContextAware, SmartInitializingSingleton {
    private final static Logger log = LoggerFactory.getLogger(ListenerContainerConfiguration.class);

    private ConfigurableApplicationContext applicationContext;

    private AtomicLong counter = new AtomicLong(0);

    private StandardEnvironment environment;

    private CloudMessageProperties cloudMessageProperties;

    private ObjectMapper objectMapper;

    public ListenerContainerConfiguration(ObjectMapper rocketMQMessageObjectMapper,
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
        Map<String, Object> beans = this.applicationContext.getBeansWithAnnotation(EnableCloudMessageListener.class);

        if (Objects.nonNull(beans)) {
            beans.forEach(this::registerContainer);
        }
    }

    private void registerContainer(String beanName, Object bean) {
        Class<?> clazz = AopProxyUtils.ultimateTargetClass(bean);

        if (!CloudMessageListener.class.isAssignableFrom(bean.getClass())) {
            throw new IllegalStateException(clazz + " is not instance of " + CloudMessageListener.class.getName());
        }

        EnableCloudMessageListener annotation = clazz.getAnnotation(EnableCloudMessageListener.class);
        validate(annotation);

        String containerBeanName = String.format("%s_%s", DefaultCloudMessageListenerContainer.class.getName(),
            counter.incrementAndGet());
        GenericApplicationContext genericApplicationContext = (GenericApplicationContext) applicationContext;

        genericApplicationContext.registerBean(containerBeanName, DefaultCloudMessageListenerContainer.class,
            () -> createRocketMQListenerContainer(containerBeanName, bean, annotation));
        DefaultCloudMessageListenerContainer container = genericApplicationContext.getBean(containerBeanName,
            DefaultCloudMessageListenerContainer.class);
        if (!container.isRunning()) {
            try {
                container.start();
            } catch (Exception e) {
                log.error("Started container failed. {}", container, e);
                throw new RuntimeException(e);
            }
        }

        log.info("Register the listener to container, listenerBeanName:{}, containerBeanName:{}", beanName, containerBeanName);
    }

    private DefaultCloudMessageListenerContainer createRocketMQListenerContainer(String name, Object bean, EnableCloudMessageListener annotation) {
        DefaultCloudMessageListenerContainer container = new DefaultCloudMessageListenerContainer();

        String nameServer = environment.resolvePlaceholders(annotation.nameServer());
        nameServer = StringUtils.isEmpty(nameServer) ? cloudMessageProperties.getNameServer() : nameServer;
        String accessChannel = environment.resolvePlaceholders(annotation.accessChannel());
        container.setNameServer(nameServer);
        if (!StringUtils.isEmpty(accessChannel)) {
            container.setAccessChannel(AccessChannel.valueOf(accessChannel));
        }
        container.setTopic(environment.resolvePlaceholders(annotation.topic()));
        container.setConsumerGroup(environment.resolvePlaceholders(annotation.consumerGroup()));
        container.setRocketMQMessageListener(annotation);
        container.setCloudMessageListener((CloudMessageListener) bean);
        container.setObjectMapper(objectMapper);
        container.setName(name);  // REVIEW ME, use the same clientId or multiple?

        return container;
    }

    private void validate(EnableCloudMessageListener annotation) {
        if (annotation.consumeMode() == ConsumeMode.ORDERLY &&
            annotation.messageModel() == MessageModel.BROADCASTING) {
            throw new BeanDefinitionValidationException(
                "Bad annotation definition in @RocketMQMessageListener, messageModel BROADCASTING does not support ORDERLY message!");
        }
    }
}
