-- V9: Add Composite Index for Submissions User, Problem, and Chronological SubmittedAt Queries

CREATE INDEX idx_submissions_user_problem_submitted_at ON submissions (user_id, problem_id, submitted_at ASC);
