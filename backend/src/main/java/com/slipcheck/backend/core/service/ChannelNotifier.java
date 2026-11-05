package com.slipcheck.backend.core.service;

import com.slipcheck.backend.core.model.Channel;

/**
 * A channel that can deliver an outgoing text message (push). Implemented by channel
 * adapters (Telegram client; LINE client only when push is enabled). WEB has no
 * implementation — web results are pulled from the dashboard.
 */
public interface ChannelNotifier {

    Channel channel();

    void send(String externalUserId, String text);
}