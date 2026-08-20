package com.businessos.core.scheduler;

import com.businessos.enums.NotificationType;
import com.businessos.modules.hrm.performance.PerformanceReview;
import com.businessos.modules.hrm.performance.PerformanceReviewRepository;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.shared.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PerformanceReview.reviewPeriodEnd is captured when a review is created but,
 * unlike almost every other deadline in this codebase, nothing ever flagged one
 * passing with the review still not finalised - a "review every 6 months"
 * policy had nothing in the system enforcing or even flagging it, entirely
 * tracked outside the app.
 */
@Component
@RequiredArgsConstructor
public class PerformanceReviewOverdueScheduler {

    // Grace period after the review period ends before nagging - the manager
    // needs time to actually conduct the review, not get flagged the day after.
    private static final int GRACE_DAYS = 14;

    private final PerformanceReviewRepository reviewRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void remindOverdueReviews() {
        LocalDate cutoff = LocalDate.now().minusDays(GRACE_DAYS);

        List<PerformanceReview> overdue = reviewRepository.findNewlyOverdue(cutoff);
        for (PerformanceReview review : overdue) {
            review.setOverdueReminderSentAt(LocalDateTime.now());

            if (review.getReviewedBy() == null || review.getReviewedBy().getUser() == null) continue;

            notificationService.send(CreateNotificationRequest.of(
                    NotificationType.PERFORMANCE_REVIEW_OVERDUE,
                    "Performance review overdue",
                    review.getEmployee().getFullName() + "'s review period ended "
                            + review.getReviewPeriodEnd() + " and it still isn't finalised.",
                    "/hrm/performance/" + review.getId(),
                    review.getReviewedBy().getUser().getId(),
                    review.getCompany().getId()));
        }
    }
}
