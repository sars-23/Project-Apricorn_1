package com.apricorn.problem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.apricorn.exception.DuplicateTagException;
import com.apricorn.exception.TagNotFoundException;
import com.apricorn.problem.dto.CreateTagRequest;
import com.apricorn.problem.dto.TagResponse;
import com.apricorn.problem.entity.Tag;
import com.apricorn.problem.mapper.TagMapper;
import com.apricorn.problem.repository.TagRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TagServiceTest {
    private TagRepository tagRepository;
    private TagService tagService;

    @BeforeEach
    void setUp() {
        tagRepository = mock(TagRepository.class);
        tagService = new TagServiceImpl(tagRepository, new TagMapper());
    }

    @Test
    void createsTagSuccessfully() {
        when(tagRepository.existsByName("Dynamic Programming")).thenReturn(false);
        when(tagRepository.existsBySlug("dynamic-programming")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> {
            Tag t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TagResponse response = tagService.createTag(new CreateTagRequest("Dynamic Programming"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Dynamic Programming");
        assertThat(response.slug()).isEqualTo("dynamic-programming");
    }

    @Test
    void rejectsDuplicateTag() {
        when(tagRepository.existsByName("Array")).thenReturn(true);

        assertThatThrownBy(() -> tagService.createTag(new CreateTagRequest("Array")))
                .isInstanceOf(DuplicateTagException.class);
    }

    @Test
    void deletesExistingTag() {
        Tag tag = new Tag();
        tag.setId(1L);
        tag.setName("Array");

        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));

        tagService.deleteTag(1L);

        verify(tagRepository).delete(tag);
    }

    @Test
    void throwsNotFoundOnDeletingNonExistentTag() {
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.deleteTag(99L))
                .isInstanceOf(TagNotFoundException.class);
    }
}
