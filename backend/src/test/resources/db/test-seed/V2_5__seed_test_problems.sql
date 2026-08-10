-- Seed canonical a-b problem (id=1) and testcases after V2 creates problems and test_cases tables.
-- This file exists ONLY in src/test/resources/db/test-seed/ and is ONLY loaded in the H2 test DB.

INSERT INTO problems (id, slug, title, difficulty, description, input_format, output_format, constraints, sample_input, sample_output, explanation, time_limit_ms, memory_limit_mb, created_by_id, created_at)
SELECT 1, 'a-b', 'A + B', 'EASY', 
    'Given two integers A and B, calculate their sum.', 
    'Two space-separated integers A and B.', 
    'Print a single integer representing A + B.', 
    '-10^9 <= A, B <= 10^9', 
    '2 3', '5', '2 + 3 = 5.', 3000, 256, 1, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM problems WHERE slug = 'a-b');

INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT 1, 1, '[2, 3]', '5', true, 1, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM test_cases WHERE problem_id = 1 AND order_index = 1);

INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT 2, 1, '[-5, 10]', '5', false, 2, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM test_cases WHERE problem_id = 1 AND order_index = 2);

INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT 3, 1, '[0, 0]', '0', false, 3, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM test_cases WHERE problem_id = 1 AND order_index = 3);

INSERT INTO test_cases (id, problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT 4, 1, '[100, -200]', '-100', false, 4, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM test_cases WHERE problem_id = 1 AND order_index = 4);

ALTER TABLE problems ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE test_cases ALTER COLUMN id RESTART WITH 1000;
