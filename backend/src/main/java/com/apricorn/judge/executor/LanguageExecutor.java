package com.apricorn.judge.executor;

import com.apricorn.judge.config.JudgeProperties;
import com.apricorn.problem.entity.TestCase;
import com.apricorn.submission.entity.Language;
import com.apricorn.submission.entity.Submission;
import com.apricorn.submission.model.JudgeResult;
import java.util.List;

public interface LanguageExecutor {
    boolean supports(Language language);
    JudgeResult execute(Submission submission, List<TestCase> testCases, JudgeProperties properties);
}
