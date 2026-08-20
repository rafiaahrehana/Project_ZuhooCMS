package com.businessos.core.scheduler;

import com.businessos.modules.hrm.announcement.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Announcement.scheduledAt existed on nothing before this - HR could draft a
 * holiday notice Friday, but someone had to remember to click Publish Monday
 * morning. Runs every 15 minutes (finer-grained than most schedulers here,
 * since a 9am-scheduled announcement landing at 9:45 defeats the point).
 */
@Component
@RequiredArgsConstructor
public class AnnouncementScheduledPublishScheduler {

    private final AnnouncementService announcementService;

    @Scheduled(cron = "0 */15 * * * *")
    public void publishDue() {
        announcementService.publishDueScheduled();
    }
}
