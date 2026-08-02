package com.apricorn.problem.service;

import com.apricorn.exception.DuplicateTagException;
import com.apricorn.exception.TagNotFoundException;
import com.apricorn.problem.dto.CreateTagRequest;
import com.apricorn.problem.dto.TagResponse;
import com.apricorn.problem.entity.Problem;
import com.apricorn.problem.entity.Tag;
import com.apricorn.problem.mapper.TagMapper;
import com.apricorn.problem.repository.TagRepository;
import com.apricorn.util.SlugUtils;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagServiceImpl implements TagService {
    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    public TagServiceImpl(TagRepository tagRepository, TagMapper tagMapper) {
        this.tagRepository = tagRepository;
        this.tagMapper = tagMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags() {
        return tagRepository.findAll().stream()
                .sorted(Comparator.comparing(Tag::getName))
                .map(tagMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TagResponse createTag(CreateTagRequest request) {
        String name = request.name().trim();
        String slug = SlugUtils.toSlug(name);

        if (tagRepository.existsByName(name) || tagRepository.existsBySlug(slug)) {
            throw new DuplicateTagException("Tag with name or slug already exists: " + name);
        }

        Tag tag = new Tag();
        tag.setName(name);
        tag.setSlug(slug);

        Tag saved = tagRepository.save(tag);
        return tagMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new TagNotFoundException("Tag not found with id: " + id));

        for (Problem problem : tag.getProblems()) {
            problem.getTags().remove(tag);
        }
        tag.getProblems().clear();

        tagRepository.delete(tag);
    }
}
