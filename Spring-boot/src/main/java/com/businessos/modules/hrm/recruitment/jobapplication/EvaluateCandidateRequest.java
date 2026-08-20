package com.businessos.modules.hrm.recruitment.jobapplication;

import lombok.Getter;
import lombok.Setter;

/** All fields optional - a partial evaluation still produces a sensible overallScore (see RecruitmentServiceImpl.evaluate()). */
@Getter
@Setter
public class EvaluateCandidateRequest {
    private Integer scoreEducation;
    private Integer scoreExperience;
    private Integer scoreTechnicalSkills;
    private Integer scoreInterview;
    private Integer scoreCommunication;
}
