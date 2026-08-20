package com.businessos.modules.hrm.recruitment.skill;

import com.businessos.modules.hrm.recruitment.candidate.CandidateRepository;
import com.businessos.modules.hrm.recruitment.jobpost.JobPostingRepository;
import com.businessos.modules.hrm.recruitment.talentpool.TalentPoolRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Skill-tag autocomplete for the SkillTagInput component - no fabricated
 * global taxonomy exists, so suggestions are pooled from skill tags already
 * typed elsewhere in this company's own recruitment data (Candidate.skills,
 * TalentPoolCandidate.skills, JobPosting.requiredSkills/preferredSkills).
 * The pool self-builds: it gets more useful the more recruiters use it.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recruitment/skills")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class RecruitmentSkillController {

    private static final int MAX_SUGGESTIONS = 10;

    private final CandidateRepository candidateRepository;
    private final TalentPoolRepository talentPoolRepository;
    private final JobPostingRepository jobPostingRepository;
    private final SecurityUtil securityUtil;

    @GetMapping("/suggestions")
    @Transactional(readOnly = true)
    public ResponseEntity<List<String>> suggestions(@RequestParam(required = false) String q) {
        Long companyId = securityUtil.getCurrentCompanyId();
        if (companyId == null) throw new BadRequestException("No company context");

        Set<String> pool = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        candidateRepository.findByCompanyId(companyId).forEach(c -> addSkills(pool, c.getSkills()));
        talentPoolRepository.findByCompanyId(companyId).forEach(c -> addSkills(pool, c.getSkills()));
        jobPostingRepository.findByCompanyId(companyId).forEach(j -> {
            addSkills(pool, j.getRequiredSkills());
            addSkills(pool, j.getPreferredSkills());
        });

        String query = q == null ? "" : q.trim().toLowerCase();
        List<String> matches = pool.stream()
            .filter(s -> query.isEmpty() || s.toLowerCase().contains(query))
            .limit(MAX_SUGGESTIONS)
            .toList();
        return ResponseEntity.ok(matches);
    }

    private void addSkills(Set<String> pool, String csv) {
        if (csv == null || csv.isBlank()) return;
        for (String skill : csv.split(",")) {
            String trimmed = skill.trim();
            if (!trimmed.isEmpty()) pool.add(trimmed);
        }
    }
}
