package com.apricorn.problem.service;

import com.apricorn.problem.dto.CreateTagRequest;
import com.apricorn.problem.dto.TagResponse;
import java.util.List;

public interface TagService {
    List<TagResponse> getAllTags();
    TagResponse createTag(CreateTagRequest request);
    void deleteTag(Long id);
}
