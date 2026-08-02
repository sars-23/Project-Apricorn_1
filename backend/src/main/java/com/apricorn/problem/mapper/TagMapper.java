package com.apricorn.problem.mapper;

import com.apricorn.problem.dto.TagResponse;
import com.apricorn.problem.entity.Tag;
import org.springframework.stereotype.Component;

@Component
public class TagMapper {
    public TagResponse toResponse(Tag tag) {
        if (tag == null) return null;
        return new TagResponse(
                tag.getId(),
                tag.getName(),
                tag.getSlug(),
                tag.getCreatedAt()
        );
    }
}
