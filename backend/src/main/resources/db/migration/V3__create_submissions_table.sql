CREATE TABLE submissions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    language VARCHAR(20) NOT NULL,
    source_code TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    verdict VARCHAR(30),
    execution_time_ms INT,
    memory_used_kb INT,
    passed_test_cases INT NOT NULL DEFAULT 0,
    total_test_cases INT NOT NULL DEFAULT 0,
    error_message TEXT,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_submissions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_submissions_problem FOREIGN KEY (problem_id) REFERENCES problems(id) ON DELETE CASCADE,
    CONSTRAINT chk_submissions_language CHECK (language IN ('JAVA', 'CPP', 'PYTHON', 'JAVASCRIPT')),
    CONSTRAINT chk_submissions_status CHECK (status IN ('PENDING', 'QUEUED', 'RUNNING', 'COMPLETED')),
    CONSTRAINT chk_submissions_verdict CHECK (verdict IN (
        'ACCEPTED', 'WRONG_ANSWER', 'COMPILATION_ERROR', 'RUNTIME_ERROR',
        'TIME_LIMIT_EXCEEDED', 'MEMORY_LIMIT_EXCEEDED', 'INTERNAL_ERROR'
    ))
);

CREATE INDEX idx_submissions_user_id ON submissions (user_id);
CREATE INDEX idx_submissions_problem_id ON submissions (problem_id);
CREATE INDEX idx_submissions_status ON submissions (status);
CREATE INDEX idx_submissions_verdict ON submissions (verdict);
CREATE INDEX idx_submissions_submitted_at ON submissions (submitted_at);
CREATE INDEX idx_submissions_user_problem ON submissions (user_id, problem_id);
