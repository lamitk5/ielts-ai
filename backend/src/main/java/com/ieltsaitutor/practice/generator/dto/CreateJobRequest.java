package com.ieltsaitutor.practice.generator.dto;

import java.util.UUID;

import com.ieltsaitutor.rag.domain.Skill;

public record CreateJobRequest(
        UUID sourceId,
        UUID blueprintId,
        Skill skill,
        String domainTopic) {

    public CreateJobRequest {
        if (skill == null) skill = Skill.READING;
    }
}
