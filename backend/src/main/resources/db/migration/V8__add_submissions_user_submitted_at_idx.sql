-- V8: Add Index for Submissions User and SubmittedAt Queries

CREATE INDEX idx_submissions_user_submitted_at ON submissions (user_id, submitted_at DESC);
