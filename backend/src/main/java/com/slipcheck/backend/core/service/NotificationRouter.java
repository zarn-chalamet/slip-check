package com.slipcheck.backend.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.slipcheck.backend.config.SlipCheckProperties;
import com.slipcheck.backend.core.model.Channel;
import com.slipcheck.backend.core.model.SlipResult;
import com.slipcheck.backend.i18n.MessageFormatter;

import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Delivers follow-up messages after retries finish. Only sends on channels listed in
 * slipcheck.retry.followup-channels (default WEB, TELEGRAM); LINE follow-ups additionally
 * require line.push-enabled=true inside the LINE notifier itself.
 *
 * This is the composition point where result codes become text (via MessageFormatter) and
 * reach a channel — business rules stay in SlipService.
 */
@Component
public class NotificationRouter {

    private static final Logger log = LoggerFactory.getLogger(NotificationRouter.class);

    private final Map<Channel, ChannelNotifier> notifiers = new EnumMap<>(Channel.class);
    private final MessageFormatter formatter;
    private final SlipCheckProperties properties;

    public NotificationRouter(List<ChannelNotifier> available, MessageFormatter formatter,
                              SlipCheckProperties properties) {
        available.forEach(n -> notifiers.put(n.channel(), n));
        this.formatter = formatter;
        this.properties = properties;
    }

    public void notifyResult(Channel channel, String externalUserId, String language, ZoneId zone,
                             SlipResult result) {
        if (!properties.retry().followupChannels().contains(channel)) {
            return;
        }
        ChannelNotifier notifier = notifiers.get(channel);
        if (notifier == null) {
            return; // e.g. WEB: the dashboard shows the result; nothing to push
        }
        String text = formatter.forResult(result, language, zone);
        sendText(channel, externalUserId, text);
    }

    public void sendText(Channel channel, String externalUserId, String text) {
        ChannelNotifier notifier = notifiers.get(channel);
        if (notifier == null) {
            return;
        }
        try {
            notifier.send(externalUserId, text);
        } catch (Exception e) {
            log.warn("push to {} failed", channel, e);
        }
    }
}
