package com.assetcloud.message.center.entity;

import org.apache.rocketmq.common.message.MessageExt;

public class CloudMessage extends MessageExt {

    private String messageBody;
    private String tags;

    @Override
    public String getTags() {
        return tags;
    }

    @Override
    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }
}
