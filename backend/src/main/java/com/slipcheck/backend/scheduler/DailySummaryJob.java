package com.slipcheck.backend.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.slipcheck.backend.config.SlipCheckProperties;
import com.slipcheck.backend.core.model.Channel;
import com.slipcheck.backend.core.service.NotificationRouter;
import com.slipcheck.backend.core.service.SummaryService;
import com.slipcheck.backend.core.service.SummaryService.DailySummary;
import com.slipcheck.backend.i18n.MessageFormatter;
import com.slipcheck.backend.persistence.entity.ChannelIdentityEntity;
import com.slipcheck.backend.persistence.repo.ChannelIdentityRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Daily summary push (spec F5). Runs hourly and sends at the configured local hour
 * (default 21:00) to identities that opted in (summary_push=true). Whether a channel can
 * actually push is enforced by the notifier (LINE push is off unless line.push-enabled).
 */
@Component
public class DailySummaryJob {

    private static final Logger log = LoggerFactory.getLogger(DailySummaryJob.class);

    private final ChannelIdentityRepository identities;
    private final SummaryService summaryService;
    private final MessageFormatter formatter;
    private final NotificationRouter notifications;
    private final SlipCheckProperties properties;

    public DailySummaryJob(ChannelIdentityRepository identities, SummaryService summaryService,
                           MessageFormatter formatter, NotificationRouter notifications,
                           SlipCheckProperties properties) {
        this.identities = identities;
        this.summaryService = summaryService;
        this.formatter = formatter;
        this.notifications = notifications;
        this.properties = properties;
    }

    @Scheduled(cron = "0 5 * * * *") // five minutes past each hour
    public void run() {
        for (ChannelIdentityEntity identity : identities.findBySummaryPushTrue()) {
            try {
                ZoneId zone = summaryService.sellerZone(identity.getSellerId());
                if (ZonedDateTime.now(zone).getHour() != properties.summary().sendHourLocal()) {
                    continue;
                }
                DailySummary summary = summaryService.compile(identity.getSellerId(), LocalDate.now(zone));
                String text = formatter.dailySummary(summary.date(), summary.verified(),
                        summary.totalBaht(), summary.review(), summary.duplicates(),
                        summary.rejected(), identity.getLanguage());
                notifications.sendText(Channel.valueOf(identity.getChannel()),
                        identity.getExternalUserId(), text);
            } catch (Exception e) {
                log.warn("daily summary push failed for identity {}", identity.getId(), e);
            }
        }
    }
}
