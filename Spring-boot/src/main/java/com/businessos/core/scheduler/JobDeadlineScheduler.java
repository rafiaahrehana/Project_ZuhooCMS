package com.businessos.core.scheduler;


import com.businessos.modules.hrm.recruitment.jobpost.JobPostingRepository;
import com.businessos.enums.JobPostingStatus;
import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor

public class JobDeadlineScheduler {

    private final JobPostingRepository jobPostingRepository;

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void closeExpiredJobPostings() {
        int count = jobPostingRepository.closeExpiredPostings(
            LocalDate.now(),
            JobPostingStatus.OPEN,
            JobPostingStatus.CLOSED
        );
        if (count > 0) {
            
        }
    }
}
