package com.businessos.modules.hrm.recruitment.ats;

import com.businessos.enums.AtsParseStatus;
import com.businessos.enums.EducationLevel;
import com.businessos.modules.hrm.recruitment.jobapplication.JobApplication;
import com.businessos.modules.hrm.recruitment.jobapplication.JobApplicationRepository;
import com.businessos.modules.hrm.recruitment.jobpost.JobPosting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Computes an automated ATS match score for a resume already stored by this
 * system's own upload endpoints - it never fetches an arbitrary external URL
 * (a candidate-supplied "resume link" could point anywhere, including an
 * internal network address), so it only ever reads local disk under
 * file.upload-dir, resolved from the filename segment of the stored URL.
 *
 * A signal only - see JobApplication's atsScore/ats* fields. Never gates a
 * status transition and never auto-rejects; the recruiter's own manual
 * Evaluate score (RecruitmentServiceImpl.evaluate) is unaffected by this.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CvScoringService {

    // Required Skills 40 / Experience 25 / Education 15 / Preferred Skills & Certifications 20 -
    // renormalized below over whichever categories the job posting actually defined.
    private static final double WEIGHT_REQUIRED_SKILLS = 0.40;
    private static final double WEIGHT_EXPERIENCE = 0.25;
    private static final double WEIGHT_EDUCATION = 0.15;
    private static final double WEIGHT_PREFERRED_SKILLS = 0.20;

    private static final Pattern EXPERIENCE_YEARS_PATTERN =
        Pattern.compile("(\\d{1,2})\\+?\\s*(?:years?|yrs?)", Pattern.CASE_INSENSITIVE);

    // Checked in this order (highest first) - the first level whose keywords appear anywhere wins.
    // Short 2-3 letter abbreviations (m.a, b.sc, ...) require the period(s) -
    // bare "MA"/"BA" collide with US state codes and other common tokens far
    // too often to trust without it; the full words stay period-optional.
    private static final Map<EducationLevel, Pattern> EDUCATION_KEYWORDS = new LinkedHashMap<>();
    static {
        EDUCATION_KEYWORDS.put(EducationLevel.PHD, Pattern.compile("\\b(ph\\.?d|doctorate)\\b", Pattern.CASE_INSENSITIVE));
        EDUCATION_KEYWORDS.put(EducationLevel.MASTER, Pattern.compile("\\b(master'?s?|m\\.sc|m\\.a\\.?|mba|m\\.eng)\\b", Pattern.CASE_INSENSITIVE));
        EDUCATION_KEYWORDS.put(EducationLevel.BACHELOR, Pattern.compile("\\b(bachelor'?s?|b\\.sc|b\\.a\\.?|b\\.eng|b\\.tech)\\b", Pattern.CASE_INSENSITIVE));
        EDUCATION_KEYWORDS.put(EducationLevel.DIPLOMA, Pattern.compile("\\bdiploma\\b", Pattern.CASE_INSENSITIVE));
    }

    private final JobApplicationRepository applicationRepository;
    private final CvTextExtractor textExtractor;

    // Self-injected (lazily, to break the circular reference) so the
    // afterCommit callback below calls scoreApplication() THROUGH the Spring
    // proxy rather than via a bare `this.` self-invocation, which would
    // silently skip both @Async and @Transactional - a plain call here looked
    // right but never actually ran off-thread, and the entity mutations were
    // never flushed since no transaction was ever opened for them.
    @Autowired
    @Lazy
    private CvScoringService self;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * What both apply() entry points actually call, right after save(). The
     * real scoring is @Async on a separate thread/transaction - dispatching
     * it immediately would race the caller's own save (that thread could
     * look the row up before this transaction has committed it), so this
     * defers to Spring's afterCommit synchronization when one is active.
     */
    public void scheduleAfterCommit(Long companyId, Long applicationId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    self.scoreApplication(companyId, applicationId);
                }
            });
        } else {
            self.scoreApplication(companyId, applicationId);
        }
    }

    /** companyId travels as a parameter rather than via SecurityUtil because this runs on a separate thread with no security context propagated to it. */
    @Async
    @Transactional
    public void scoreApplication(Long companyId, Long applicationId) {
        JobApplication application = applicationRepository.findByIdAndCompanyId(applicationId, companyId).orElse(null);
        if (application == null) {
            log.warn("ATS scoring skipped - application {} not found for company {}", applicationId, companyId);
            return;
        }

        JobPosting posting = application.getJobPosting();
        if (posting == null || !hasAnyRequirement(posting)) {
            application.setAtsParseStatus(AtsParseStatus.NOT_APPLICABLE);
            application.setAtsParsedAt(Instant.now());
            return;
        }

        String resumeUrl = application.getCandidate() != null ? application.getCandidate().getResumeUrl() : null;
        Path localPath = resolveOwnUploadPath(resumeUrl);
        if (localPath == null || !Files.isRegularFile(localPath)) {
            application.setAtsParseStatus(AtsParseStatus.NO_RESUME);
            application.setAtsParsedAt(Instant.now());
            return;
        }

        try {
            byte[] bytes = Files.readAllBytes(localPath);
            String text = textExtractor.extract(bytes, extensionOf(localPath.getFileName().toString()));
            applyScoring(application, posting, text);
            application.setAtsParseStatus(AtsParseStatus.SUCCESS);
        } catch (UnsupportedResumeFormatException ex) {
            application.setAtsParseStatus(AtsParseStatus.UNSUPPORTED_FORMAT);
        } catch (Exception ex) {
            log.warn("ATS scoring failed for application {}", applicationId, ex);
            application.setAtsParseStatus(AtsParseStatus.FAILED);
        }
        application.setAtsParsedAt(Instant.now());
    }

    private void applyScoring(JobApplication application, JobPosting posting, String text) {
        String lower = text.toLowerCase();

        List<String> requiredSkills = splitSkills(posting.getRequiredSkills());
        List<String> preferredSkills = splitSkills(posting.getPreferredSkills());

        List<String> matchedRequired = new ArrayList<>();
        List<String> missingRequired = new ArrayList<>();
        for (String skill : requiredSkills) {
            (containsSkill(lower, skill) ? matchedRequired : missingRequired).add(skill);
        }
        List<String> matchedPreferred = new ArrayList<>();
        for (String skill : preferredSkills) {
            if (containsSkill(lower, skill)) matchedPreferred.add(skill);
        }

        Integer extractedYears = extractMaxYears(text);
        EducationLevel extractedLevel = extractEducationLevel(text);

        application.setAtsMatchedRequiredSkills(join(matchedRequired));
        application.setAtsMissingRequiredSkills(join(missingRequired));
        application.setAtsMatchedPreferredSkills(join(matchedPreferred));
        application.setAtsExtractedExperienceYears(extractedYears);

        double weightedSum = 0;
        double totalWeight = 0;

        if (!requiredSkills.isEmpty()) {
            double score = (double) matchedRequired.size() / requiredSkills.size();
            weightedSum += WEIGHT_REQUIRED_SKILLS * score;
            totalWeight += WEIGHT_REQUIRED_SKILLS;
        }
        if (posting.getMinExperienceYears() != null) {
            // minExperienceYears == 0 means "no floor" - anyone meets it, and
            // dividing by zero would otherwise produce NaN and silently zero
            // the entire atsScore below.
            double score = posting.getMinExperienceYears() <= 0 ? 1.0
                : extractedYears == null ? 0.0
                : Math.min(1.0, extractedYears / (double) posting.getMinExperienceYears());
            weightedSum += WEIGHT_EXPERIENCE * score;
            totalWeight += WEIGHT_EXPERIENCE;
        }
        if (posting.getMinEducationLevel() != null) {
            boolean meets = extractedLevel != null && extractedLevel.ordinal() >= posting.getMinEducationLevel().ordinal();
            application.setAtsMeetsEducationRequirement(meets);
            weightedSum += WEIGHT_EDUCATION * (meets ? 1.0 : 0.0);
            totalWeight += WEIGHT_EDUCATION;
        } else {
            application.setAtsMeetsEducationRequirement(null);
        }
        if (!preferredSkills.isEmpty()) {
            double score = (double) matchedPreferred.size() / preferredSkills.size();
            weightedSum += WEIGHT_PREFERRED_SKILLS * score;
            totalWeight += WEIGHT_PREFERRED_SKILLS;
        }

        application.setAtsScore(totalWeight > 0 ? (int) Math.round((weightedSum / totalWeight) * 100) : null);
    }

    /** Word-boundary match where the skill's edges are alphanumeric; plain substring otherwise (covers "C++", "C#", ".NET"). */
    private boolean containsSkill(String lowerText, String skill) {
        String needle = skill.trim().toLowerCase();
        if (needle.isEmpty()) return false;
        boolean startsWord = Character.isLetterOrDigit(needle.charAt(0));
        boolean endsWord = Character.isLetterOrDigit(needle.charAt(needle.length() - 1));
        // \b alone treats a letter-to-symbol transition as a boundary, so a
        // short skill like "C" would match inside "C++" or "C#" - excluded
        // explicitly since those are different technologies from bare "C".
        String right = endsWord ? "\\b(?![+#])" : "";
        String pattern = (startsWord ? "\\b" : "") + Pattern.quote(needle) + right;
        return Pattern.compile(pattern).matcher(lowerText).find();
    }

    private Integer extractMaxYears(String text) {
        Matcher m = EXPERIENCE_YEARS_PATTERN.matcher(text);
        Integer max = null;
        while (m.find()) {
            int value = Integer.parseInt(m.group(1));
            if (max == null || value > max) max = value;
        }
        return max;
    }

    private EducationLevel extractEducationLevel(String text) {
        for (Map.Entry<EducationLevel, Pattern> entry : EDUCATION_KEYWORDS.entrySet()) {
            if (entry.getValue().matcher(text).find()) return entry.getKey();
        }
        return null;
    }

    private List<String> splitSkills(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    private String join(List<String> values) {
        return values.isEmpty() ? null : String.join(", ", values);
    }

    private boolean hasAnyRequirement(JobPosting posting) {
        return notBlank(posting.getRequiredSkills()) || notBlank(posting.getPreferredSkills())
            || posting.getMinExperienceYears() != null || posting.getMinEducationLevel() != null;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Never issues a network request - only ever reads local disk, resolved
     * from the filename segment of the URL (same path-traversal guard as
     * LocalFileStorageService.persist()). A resumeUrl that isn't one of our
     * own /uploads/ links (e.g. an external Drive/Dropbox link) resolves to
     * null here, same as no resume at all.
     */
    private Path resolveOwnUploadPath(String resumeUrl) {
        if (resumeUrl == null || !resumeUrl.contains("/uploads/")) return null;
        String filename = resumeUrl.substring(resumeUrl.lastIndexOf('/') + 1);
        if (filename.isBlank()) return null;
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path target = uploadPath.resolve(filename).normalize();
            if (!target.startsWith(uploadPath)) return null;
            return target;
        } catch (java.nio.file.InvalidPathException ex) {
            // A resumeUrl is free text (the "paste a link" fallback accepts
            // anything) - a filesystem-illegal filename segment used to throw
            // this uncaught, rolling back the transaction before atsParseStatus
            // was ever set and leaving the application stuck at PENDING forever.
            return null;
        }
    }

    private String extensionOf(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx).toLowerCase() : "";
    }
}
