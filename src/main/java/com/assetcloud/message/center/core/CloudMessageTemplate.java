package com.assetcloud.message.center.core;

import com.assetcloud.message.center.config.CloudMessageConfigUtils;
import com.assetcloud.message.center.support.CloudMessageUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.client.producer.selector.SelectMessageQueueByHash;
import org.apache.rocketmq.remoting.RPCHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@SuppressWarnings({"WeakerAccess", "unused"})
public class CloudMessageTemplate extends AbstractMessageSendingTemplate<String> implements InitializingBean, DisposableBean {
    private static final  Logger log = LoggerFactory.getLogger(CloudMessageTemplate.class);

    private DefaultMQProducer producer;

    private ObjectMapper objectMapper;

    private String charset = "UTF-8";

    private MessageQueueSelector messageQueueSelector = new SelectMessageQueueByHash();

    private final Map<String, TransactionMQProducer> cache = new ConcurrentHashMap<>(); //only put TransactionMQProducer by now!!!

    public DefaultMQProducer getProducer() {
        return producer;
    }

    public void setProducer(DefaultMQProducer producer) {
        this.producer = producer;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public MessageQueueSelector getMessageQueueSelector() {
        return messageQueueSelector;
    }

    public void setMessageQueueSelector(MessageQueueSelector messageQueueSelector) {
        this.messageQueueSelector = messageQueueSelector;
    }

    public SendResult syncSend(String destination, Message<?> message) {
        return syncSend(destination, message, producer.getSendMsgTimeout());
    }

    public SendResult syncSend(String destination, Message<?> message, long timeout) {
        return syncSend(destination, message, timeout, 0);
    }

    public SendResult syncSend(String destination, Collection<Message<?>> messages, long timeout) {
        if (Objects.isNull(messages) || messages.size() == 0) {
            log.error("syncSend with batch failed. destination:{}, messages is empty ", destination);
            throw new IllegalArgumentException("`messages` can not be empty");
        }

        try {
            long now = System.currentTimeMillis();
            Collection<org.apache.rocketmq.common.message.Message> rmqMsgs = new ArrayList<>();
            org.apache.rocketmq.common.message.Message rocketMsg;
            for (Message<?> msg:messages) {
                if (Objects.isNull(msg) || Objects.isNull(msg.getPayload())) {
                    log.warn("Found a message empty in the batch, skip it");
                    continue;
                }
                rocketMsg = CloudMessageUtil.convertToRocketMessage(objectMapper, charset, destination, msg);
                rmqMsgs.add(rocketMsg);
            }

            SendResult sendResult = producer.send(rmqMsgs, timeout);
            long costTime = System.currentTimeMillis() - now;
            log.debug("send messages cost: {} ms, msgId:{}", costTime, sendResult.getMsgId());
            return sendResult;
        } catch (Exception e) {
            log.error("syncSend with batch failed. destination:{}, messages.size:{} ", destination, messages.size());
            throw new MessagingException(e.getMessage(), e);
        }
    }

    public SendResult syncSend(String destination, Message<?> message, long timeout, int delayLevel) {
        if (Objects.isNull(message) || Objects.isNull(message.getPayload())) {
            log.error("syncSend failed. destination:{}, message is null ", destination);
            throw new IllegalArgumentException("`message` and `message.payload` cannot be null");
        }

        try {
            long now = System.currentTimeMillis();
            org.apache.rocketmq.common.message.Message rocketMsg = CloudMessageUtil.convertToRocketMessage(objectMapper,
                charset, destination, message);
            if (delayLevel > 0) {
                rocketMsg.setDelayTimeLevel(delayLevel);
            }
            SendResult sendResult = producer.send(rocketMsg, timeout);
            long costTime = System.currentTimeMillis() - now;
            log.debug("send message cost: {} ms, msgId:{}", costTime, sendResult.getMsgId());
            return sendResult;
        } catch (Exception e) {
            log.error("syncSend failed. destination:{}, message:{} ", destination, message);
            throw new MessagingException(e.getMessage(), e);
        }
    }

    public SendResult syncSend(String destination, Object payload) {
        return syncSend(destination, payload, producer.getSendMsgTimeout());
    }

    public SendResult syncSend(String destination, Object payload, long timeout) {
        Message<?> message = this.doConvert(payload, null, null);
        return syncSend(destination, message, timeout);
    }

    public SendResult syncSendOrderly(String destination, Message<?> message, String hashKey) {
        return syncSendOrderly(destination, message, hashKey, producer.getSendMsgTimeout());
    }

    public SendResult syncSendOrderly(String destination, Message<?> message, String hashKey, long timeout) {
        if (Objects.isNull(message) || Objects.isNull(message.getPayload())) {
            log.error("syncSendOrderly failed. destination:{}, message is null ", destination);
            throw new IllegalArgumentException("`message` and `message.payload` cannot be null");
        }

        try {
            long now = System.currentTimeMillis();
            org.apache.rocketmq.common.message.Message rocketMsg = CloudMessageUtil.convertToRocketMessage(objectMapper,
                charset, destination, message);
            SendResult sendResult = producer.send(rocketMsg, messageQueueSelector, hashKey, timeout);
            long costTime = System.currentTimeMillis() - now;
            log.debug("send message cost: {} ms, msgId:{}", costTime, sendResult.getMsgId());
            return sendResult;
        } catch (Exception e) {
            log.error("syncSendOrderly failed. destination:{}, message:{} ", destination, message);
            throw new MessagingException(e.getMessage(), e);
        }
    }

    public SendResult syncSendOrderly(String destination, Object payload, String hashKey) {
        return syncSendOrderly(destination, payload, hashKey, producer.getSendMsgTimeout());
    }

    public SendResult syncSendOrderly(String destination, Object payload, String hashKey, long timeout) {
        Message<?> message = this.doConvert(payload, null, null);
        return syncSendOrderly(destination, message, hashKey, producer.getSendMsgTimeout());
    }
    public void asyncSend(String destination, Message<?> message, SendCallback sendCallback, long timeout, int delayLevel) {
        if (Objects.isNull(message) || Objects.isNull(message.getPayload())) {
            log.error("asyncSend failed. destination:{}, message is null ", destination);
            throw new IllegalArgumentException("`message` and `message.payload` cannot be null");
        }

        try {
            org.apache.rocketmq.common.message.Message rocketMsg = CloudMessageUtil.convertToRocketMessage(objectMapper,
                charset, destination, message);
            if (delayLevel > 0) {
                rocketMsg.setDelayTimeLevel(delayLevel);
            }
            producer.send(rocketMsg, sendCallback, timeout);
        } catch (Exception e) {
            log.info("asyncSend failed. destination:{}, message:{} ", destination, message);
            throw new MessagingException(e.getMessage(), e);
        }
    }
    public void asyncSend(String destination, Message<?> message, SendCallback sendCallback, long timeout) {
        asyncSend(destination,message,sendCallback,timeout,0);
    }

    public void asyncSend(String destination, Message<?> message, SendCallback sendCallback) {
        asyncSend(destination, message, sendCallback, producer.getSendMsgTimeout());
    }

    public void asyncSend(String destination, Object payload, SendCallback sendCallback, long timeout) {
        Message<?> message = this.doConvert(payload, null, null);
        asyncSend(destination, message, sendCallback, timeout);
    }

    public void asyncSend(String destination, Object payload, SendCallback sendCallback) {
        asyncSend(destination, payload, sendCallback, producer.getSendMsgTimeout());
    }

    public void asyncSendOrderly(String destination, Message<?> message, String hashKey, SendCallback sendCallback,
                                 long timeout) {
        if (Objects.isNull(message) || Objects.isNull(message.getPayload())) {
            log.error("asyncSendOrderly failed. destination:{}, message is null ", destination);
            throw new IllegalArgumentException("`message` and `message.payload` cannot be null");
        }

        try {
            org.apache.rocketmq.common.message.Message rocketMsg = CloudMessageUtil.convertToRocketMessage(objectMapper,
                charset, destination, message);
            producer.send(rocketMsg, messageQueueSelector, hashKey, sendCallback, timeout);
        } catch (Exception e) {
            log.error("asyncSendOrderly failed. destination:{}, message:{} ", destination, message);
            throw new MessagingException(e.getMessage(), e);
        }
    }

    public void asyncSendOrderly(String destination, Message<?> message, String hashKey, SendCallback sendCallback) {
        asyncSendOrderly(destination, message, hashKey, sendCallback, producer.getSendMsgTimeout());
    }

    public void asyncSendOrderly(String destination, Object payload, String hashKey, SendCallback sendCallback) {
        asyncSendOrderly(destination, payload, hashKey, sendCallback, producer.getSendMsgTimeout());
    }

    public void asyncSendOrderly(String destination, Object payload, String hashKey, SendCallback sendCallback,
                                 long timeout) {
        Message<?> message = this.doConvert(payload, null, null);
        asyncSendOrderly(destination, message, hashKey, sendCallback, timeout);
    }

    public void sendOneWay(String destination, Message<?> message) {
        if (Objects.isNull(message) || Objects.isNull(message.getPayload())) {
            log.error("sendOneWay failed. destination:{}, message is null ", destination);
            throw new IllegalArgumentException("`message` and `message.payload` cannot be null");
        }

        try {
            org.apache.rocketmq.common.message.Message rocketMsg = CloudMessageUtil.convertToRocketMessage(objectMapper,
                charset, destination, message);
            producer.sendOneway(rocketMsg);
        } catch (Exception e) {
            log.error("sendOneWay failed. destination:{}, message:{} ", destination, message);
            throw new MessagingException(e.getMessage(), e);
        }
    }

    public void sendOneWay(String destination, Object payload) {
        Message<?> message = this.doConvert(payload, null, null);
        sendOneWay(destination, message);
    }

    public void sendOneWayOrderly(String destination, Message<?> message, String hashKey) {
        if (Objects.isNull(message) || Objects.isNull(message.getPayload())) {
            log.error("sendOneWayOrderly failed. destination:{}, message is null ", destination);
            throw new IllegalArgumentException("`message` and `message.payload` cannot be null");
        }

        try {
            org.apache.rocketmq.common.message.Message rocketMsg = CloudMessageUtil.convertToRocketMessage(objectMapper,
                charset, destination, message);
            producer.sendOneway(rocketMsg, messageQueueSelector, hashKey);
        } catch (Exception e) {
            log.error("sendOneWayOrderly failed. destination:{}, message:{}", destination, message);
            throw new MessagingException(e.getMessage(), e);
        }
    }

    public void sendOneWayOrderly(String destination, Object payload, String hashKey) {
        Message<?> message = this.doConvert(payload, null, null);
        sendOneWayOrderly(destination, message, hashKey);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (producer != null) {
            producer.start();
        }
    }

    @Override
    protected void doSend(String destination, Message<?> message) {
        SendResult sendResult = syncSend(destination, message);
        log.debug("send message to `{}` finished. result:{}", destination, sendResult);
    }



    @Override
    protected Message<?> doConvert(Object payload, Map<String, Object> headers, MessagePostProcessor postProcessor) {
        String content;
        if (payload instanceof String) {
            content = (String) payload;
        } else {
            // If payload not as string, use objectMapper change it.
            try {
                content = objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException e) {
                log.error("convert payload to String failed. payload:{}", payload);
                throw new RuntimeException("convert to payload to String failed.", e);
            }
        }

        MessageBuilder<?> builder = MessageBuilder.withPayload(content);
        if (headers != null) {
            builder.copyHeaders(headers);
        }
        builder.setHeaderIfAbsent(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN);

        Message<?> message = builder.build();
        if (postProcessor != null) {
            message = postProcessor.postProcessMessage(message);
        }
        return message;
    }

    @Override
    public void destroy() {
        if (Objects.nonNull(producer)) {
            producer.shutdown();
        }

        for (Map.Entry<String, TransactionMQProducer> kv : cache.entrySet()) {
            if (Objects.nonNull(kv.getValue())) {
                kv.getValue().shutdown();
            }
        }
        cache.clear();
    }

    private String getTxProducerGroupName(String name) {
        return name == null ? CloudMessageConfigUtils.ROCKETMQ_TRANSACTION_DEFAULT_GLOBAL_NAME : name;
    }

    private TransactionMQProducer stageMQProducer(String name) throws MessagingException {
        name = getTxProducerGroupName(name);

        TransactionMQProducer cachedProducer = cache.get(name);
        if (cachedProducer == null) {
            throw new MessagingException(
                String.format("Can not found MQProducer '%s' in cache! please define @RocketMQLocalTransactionListener class or invoke createOrGetStartedTransactionMQProducer() to create it firstly", name));
        }

        return cachedProducer;
    }

    public TransactionSendResult sendMessageInTransaction(final String txProducerGroup, final String destination, final Message<?> message, final Object arg) throws MessagingException {
        try {
            TransactionMQProducer txProducer = this.stageMQProducer(txProducerGroup);
            org.apache.rocketmq.common.message.Message rocketMsg = CloudMessageUtil.convertToRocketMessage(objectMapper,
                charset, destination, message);
            return txProducer.sendMessageInTransaction(rocketMsg, arg);
        } catch (MQClientException e) {
            throw CloudMessageUtil.convert(e);
        }
    }

    public void removeTransactionMQProducer(String txProducerGroup) throws MessagingException {
        txProducerGroup = getTxProducerGroupName(txProducerGroup);
        if (cache.containsKey(txProducerGroup)) {
            DefaultMQProducer cachedProducer = cache.get(txProducerGroup);
            cachedProducer.shutdown();
            cache.remove(txProducerGroup);
        }
    }

    public boolean createAndStartTransactionMQProducer(String txProducerGroup,
                                                       CloudMessageLocalTransactionListener transactionListener,
                                                       ExecutorService executorService, RPCHook rpcHook) throws MessagingException {
        txProducerGroup = getTxProducerGroupName(txProducerGroup);
        if (cache.containsKey(txProducerGroup)) {
            log.info(String.format("get TransactionMQProducer '%s' from cache", txProducerGroup));
            return false;
        }

        TransactionMQProducer txProducer = createTransactionMQProducer(txProducerGroup, transactionListener, executorService, rpcHook);
        try {
            txProducer.start();
            cache.put(txProducerGroup, txProducer);
        } catch (MQClientException e) {
            throw CloudMessageUtil.convert(e);
        }

        return true;
    }

    private TransactionMQProducer createTransactionMQProducer(String name,
                                                              CloudMessageLocalTransactionListener transactionListener,
                                                              ExecutorService executorService, RPCHook rpcHook) {
        Assert.notNull(producer, "Property 'producer' is required");
        Assert.notNull(transactionListener, "Parameter 'transactionListener' is required");
        TransactionMQProducer txProducer;
        if (Objects.nonNull(rpcHook)) {
            txProducer = new TransactionMQProducer(name, rpcHook);
            txProducer.setVipChannelEnabled(false);
            txProducer.setInstanceName(CloudMessageUtil.getInstanceName(rpcHook, name));
        } else {
            txProducer = new TransactionMQProducer(name);
        }
        txProducer.setTransactionListener(CloudMessageUtil.convert(transactionListener));

        txProducer.setNamesrvAddr(producer.getNamesrvAddr());
        if (executorService != null) {
            txProducer.setExecutorService(executorService);
        }

        txProducer.setSendMsgTimeout(producer.getSendMsgTimeout());
        txProducer.setRetryTimesWhenSendFailed(producer.getRetryTimesWhenSendFailed());
        txProducer.setRetryTimesWhenSendAsyncFailed(producer.getRetryTimesWhenSendAsyncFailed());
        txProducer.setMaxMessageSize(producer.getMaxMessageSize());
        txProducer.setCompressMsgBodyOverHowmuch(producer.getCompressMsgBodyOverHowmuch());
        txProducer.setRetryAnotherBrokerWhenNotStoreOK(producer.isRetryAnotherBrokerWhenNotStoreOK());

        return txProducer;
    }
}
