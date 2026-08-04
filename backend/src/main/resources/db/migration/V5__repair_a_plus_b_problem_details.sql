UPDATE problems
SET 
    input_format = 'Two space-separated integers A and B.',
    output_format = 'Print a single integer representing A + B.',
    constraints = '-1000000000 <= A, B <= 1000000000',
    sample_input = '2 3',
    sample_output = '5',
    explanation = '2 + 3 = 5.'
WHERE slug LIKE 'a-b%';

-- Insert missing test case 1
INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT p.id, '2 3', '5', true, 1, CURRENT_TIMESTAMP
FROM problems p
WHERE p.slug LIKE 'a-b%' AND NOT EXISTS (SELECT 1 FROM test_cases tc WHERE tc.problem_id = p.id);

-- Insert missing test case 2
INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT p.id, '-5 10', '5', true, 2, CURRENT_TIMESTAMP
FROM problems p
WHERE p.slug LIKE 'a-b%' AND (SELECT COUNT(*) FROM test_cases tc WHERE tc.problem_id = p.id) = 1;

-- Insert missing test case 3
INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT p.id, '1000000000 1000000000', '2000000000', false, 3, CURRENT_TIMESTAMP
FROM problems p
WHERE p.slug LIKE 'a-b%' AND (SELECT COUNT(*) FROM test_cases tc WHERE tc.problem_id = p.id) = 2;

-- Insert missing test case 4
INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT p.id, '-1000000000 -1000000000', '-2000000000', false, 4, CURRENT_TIMESTAMP
FROM problems p
WHERE p.slug LIKE 'a-b%' AND (SELECT COUNT(*) FROM test_cases tc WHERE tc.problem_id = p.id) = 3;
