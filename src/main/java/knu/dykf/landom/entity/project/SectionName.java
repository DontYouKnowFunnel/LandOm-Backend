package knu.dykf.landom.entity.project;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "분석 섹션 이름")
public enum SectionName {
    HERO,
    PROBLEM,
    TARGET,
    USE_CASE,
    FEATURE,
    VALUE_PROP,
    SOCIAL_PROOF,
    PRICING,
    FAQ,
    CTA_SECTION,
    GENERIC
}
