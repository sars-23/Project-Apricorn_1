-- Fix all remaining unmigrated test cases for canonical problems
UPDATE test_cases SET input = '[2, 3]', expected_output = '5' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'a-b') AND input = '2 3';
UPDATE test_cases SET input = '[-5, 10]', expected_output = '5' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'a-b') AND input = '-5 10';
UPDATE test_cases SET input = '[1000000000, 1000000000]', expected_output = '2000000000' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'a-b') AND input = '1000000000 1000000000';
UPDATE test_cases SET input = '[-1000000000, -1000000000]', expected_output = '-2000000000' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'a-b') AND input = '-1000000000 -1000000000';

UPDATE test_cases SET input = '[3]', expected_output = 'false' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'even-odd') AND input = '3';
UPDATE test_cases SET input = '[0]', expected_output = 'true' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'even-odd') AND input = '0';
