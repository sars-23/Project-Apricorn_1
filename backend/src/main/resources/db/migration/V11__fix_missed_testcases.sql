-- Fix missed test cases in V10
UPDATE test_cases SET input = '[0, 0]', expected_output = '0' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'a-b') AND order_index = 2;
UPDATE test_cases SET input = '[-5, 5]', expected_output = '0' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'a-b') AND order_index = 3;
UPDATE test_cases SET input = '[10, -5]', expected_output = '5' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'a-b') AND order_index = 4;

-- check even-odd
UPDATE test_cases SET input = '[3]', expected_output = 'false' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'even-odd') AND order_index = 2;
UPDATE test_cases SET input = '[0]', expected_output = 'true' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'even-odd') AND order_index = 3;

-- Any other missed? 
-- The test suite CanonicalContractIntegrityTest will fail if there are any others, but let's check max_element, array_sum.
UPDATE test_cases SET input = '[[5, 1, 9, 3]]', expected_output = '9' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'max-element') AND order_index = 2;
UPDATE test_cases SET input = '[[1, 2, 3, 4]]', expected_output = '10' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'array-sum') AND order_index = 2;
