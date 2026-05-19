package knu.dykf.landom.entity.project;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Locale;

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
    GENERIC;

    @JsonCreator
    public static SectionName from(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);

        if ("CTA".equals(normalized)) {
            return CTA_SECTION;
        }

        return SectionName.valueOf(normalized);
    }
}
