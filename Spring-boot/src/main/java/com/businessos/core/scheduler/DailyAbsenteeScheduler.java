package com.businessos.core.scheduler;

import com.businessos.modules.hrm.attendance.attendance.AbsenteeMarkingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;


@Slf4j
@Component
@RequiredArgsConstructor
public class DailyAbsenteeScheduler {

    /** How many days back each run re-checks, to fill gaps left by downtime. */
    private static final int BACKFILL_DAYS = 45;

    private final AbsenteeMarkingService absenteeMarkingService;

    @Scheduled(cron = "0 0 23 * * *")
    public void markAbsentees() {
        LocalDate today = LocalDate.now();
        int created = 0;

        for (int daysAgo = BACKFILL_DAYS; daysAgo >= 0; daysAgo--) {
            created += absenteeMarkingService.markAllCompaniesForDate(today.minusDays(daysAgo));
        }

        if (created > 0) {
            log.info("DailyAbsenteeScheduler created {} ABSENT record(s) over the last {} days",
                    created, BACKFILL_DAYS);
        }
    }
}
