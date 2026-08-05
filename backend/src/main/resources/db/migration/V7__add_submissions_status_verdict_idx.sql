-- V7: Add Indexes for Problem Status and Acceptance Rate Queries

CREATE INDEX idx_submissions_user_prob_verdict_status ON submissions (user_id, problem_id, status, verdict);
CREATE INDEX idx_submissions_prob_status_verdict ON submissions (problem_id, status, verdict);
