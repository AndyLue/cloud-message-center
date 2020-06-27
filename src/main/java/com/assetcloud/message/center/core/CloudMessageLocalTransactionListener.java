package com.assetcloud.message.center.core;

import org.springframework.messaging.Message;

public interface CloudMessageLocalTransactionListener {
    CloudMessageLocalTransactionState executeLocalTransaction(final Message msg, final Object arg);

    CloudMessageLocalTransactionState checkLocalTransaction(final Message msg);
}
