package com.assetcloud.message.center.autoconfigure;

import com.assetcloud.message.center.config.CloudMessageConfigUtils;
import com.assetcloud.message.center.config.CloudMessageTransactionAnnotationProcessor;
import com.assetcloud.message.center.config.TransactionHandlerRegistry;
import com.assetcloud.message.center.core.CloudMessageTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.client.MQAdmin;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Role;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

@Configuration
@EnableConfigurationProperties(CloudMessageProperties.class)
@ConditionalOnClass({ MQAdmin.class, ObjectMapper.class })
@ConditionalOnProperty(prefix = "cloudmessage", value = "name-server", matchIfMissing = true)
@Import({ JacksonFallbackConfiguration.class, ListenerContainerConfiguration.class, ExtProducerResetConfiguration.class })
@AutoConfigureAfter(JacksonAutoConfiguration.class)
public class CloudMessageAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(CloudMessageAutoConfiguration.class);

    @Autowired
    private Environment environment;

    @PostConstruct
    public void checkProperties() {
        String nameServer = environment.getProperty("cloudmessage.name-server", String.class);
        log.debug("cloudmessage.nameServer = {}", nameServer);
        if (nameServer == null) {
            log.warn("The necessary spring property 'rocketmq.name-server' is not defined, all rockertmq beans creation are skipped!");
        }
    }


    @Bean
    @ConditionalOnMissingBean(DefaultMQProducer.class)
    @ConditionalOnProperty(prefix = "cloudmessage", value = {"name-server", "producer.group"})
    public DefaultMQProducer defaultMQProducer(CloudMessageProperties cloudMessageProperties) {
        CloudMessageProperties.Producer producerConfig = cloudMessageProperties.getProducer();
        String nameServer = cloudMessageProperties.getNameServer();
        String groupName = producerConfig.getGroup();
        Assert.hasText(nameServer, "[rocketmq.name-server] must not be null");
        Assert.hasText(groupName, "[rocketmq.producer.group] must not be null");

        String accessChannel = cloudMessageProperties.getAccessChannel();

        DefaultMQProducer producer;
        String ak = cloudMessageProperties.getProducer().getAccessKey();
        String sk = cloudMessageProperties.getProducer().getSecretKey();
        if (!StringUtils.isEmpty(ak) && !StringUtils.isEmpty(sk)) {
            producer = new DefaultMQProducer(groupName, new AclClientRPCHook(new SessionCredentials(ak, sk)),
                cloudMessageProperties.getProducer().isEnableMsgTrace(),
                cloudMessageProperties.getProducer().getCustomizedTraceTopic());
            producer.setVipChannelEnabled(false);
        } else {
            producer = new DefaultMQProducer(groupName, cloudMessageProperties.getProducer().isEnableMsgTrace(),
                cloudMessageProperties.getProducer().getCustomizedTraceTopic());
        }

        producer.setNamesrvAddr(nameServer);
        if (!StringUtils.isEmpty(accessChannel)) {
            producer.setAccessChannel(AccessChannel.valueOf(accessChannel));
        }
        producer.setSendMsgTimeout(producerConfig.getSendMessageTimeout());
        producer.setRetryTimesWhenSendFailed(producerConfig.getRetryTimesWhenSendFailed());
        producer.setRetryTimesWhenSendAsyncFailed(producerConfig.getRetryTimesWhenSendAsyncFailed());
        producer.setMaxMessageSize(producerConfig.getMaxMessageSize());
        producer.setCompressMsgBodyOverHowmuch(producerConfig.getCompressMessageBodyThreshold());
        producer.setRetryAnotherBrokerWhenNotStoreOK(producerConfig.isRetryNextServer());

        return producer;
    }

    @Bean(destroyMethod = "destroy")
    @ConditionalOnBean(DefaultMQProducer.class)
    @ConditionalOnMissingBean(name = CloudMessageConfigUtils.ROCKETMQ_TEMPLATE_DEFAULT_GLOBAL_NAME)
    public CloudMessageTemplate rocketMQTemplate(DefaultMQProducer mqProducer, ObjectMapper rocketMQMessageObjectMapper) {
        CloudMessageTemplate cloudMessageTemplate = new CloudMessageTemplate();
        cloudMessageTemplate.setProducer(mqProducer);
        cloudMessageTemplate.setObjectMapper(rocketMQMessageObjectMapper);
        return cloudMessageTemplate;
    }

    @Bean
    @ConditionalOnBean(name = CloudMessageConfigUtils.ROCKETMQ_TEMPLATE_DEFAULT_GLOBAL_NAME)
    @ConditionalOnMissingBean(TransactionHandlerRegistry.class)
    public TransactionHandlerRegistry transactionHandlerRegistry(@Qualifier(CloudMessageConfigUtils.ROCKETMQ_TEMPLATE_DEFAULT_GLOBAL_NAME)
                                                                         CloudMessageTemplate template) {
        return new TransactionHandlerRegistry(template);
    }

    @Bean(name = CloudMessageConfigUtils.ROCKETMQ_TRANSACTION_ANNOTATION_PROCESSOR_BEAN_NAME)
    @ConditionalOnBean(TransactionHandlerRegistry.class)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static CloudMessageTransactionAnnotationProcessor transactionAnnotationProcessor(
        TransactionHandlerRegistry transactionHandlerRegistry) {
        return new CloudMessageTransactionAnnotationProcessor(transactionHandlerRegistry);
    }

}
