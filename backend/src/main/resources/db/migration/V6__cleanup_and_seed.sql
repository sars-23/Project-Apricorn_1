-- V6: Database Cleanup & Seeding Problems

-- 1. Reassign submissions from duplicate A+B problems to the canonical one
UPDATE submissions 
SET problem_id = (SELECT id FROM problems WHERE slug = 'a-b')
WHERE problem_id IN (SELECT id FROM problems WHERE slug IN ('a-b-1', 'a-b-2'));

-- 2. Delete tags associated with duplicate A+B problems (if any)
DELETE FROM problem_tags 
WHERE problem_id IN (SELECT id FROM problems WHERE slug IN ('a-b-1', 'a-b-2'));

-- 3. Delete test cases for duplicate A+B problems
DELETE FROM test_cases 
WHERE problem_id IN (SELECT id FROM problems WHERE slug IN ('a-b-1', 'a-b-2'));

-- 4. Delete duplicate A+B problems
DELETE FROM problems 
WHERE slug IN ('a-b-1', 'a-b-2');


-- Insert Problem: Even Odd
INSERT INTO problems (slug, title, difficulty, description, input_format, output_format, constraints, sample_input, sample_output, explanation, time_limit_ms, memory_limit_mb, created_by_id, created_at)
VALUES (
    'even-odd', 'Even Odd', 'EASY', 
    'Given an integer N, determine if it is even or odd.', 
    'A single integer N.', 
    'Print ''Even'' if N is even, otherwise ''Odd''.', 
    '-10^9 <= N <= 10^9', 
    '4', 
    'Even', 
    '4 is divisible by 2.', 
    2000, 256, 1, CURRENT_TIMESTAMP
);

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '4', 'Even', true, 1, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'even-odd';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '7', 'Odd', false, 2, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'even-odd';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '0', 'Even', false, 3, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'even-odd';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '-3', 'Odd', false, 4, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'even-odd';

-- Insert Problem: Reverse Number
INSERT INTO problems (slug, title, difficulty, description, input_format, output_format, constraints, sample_input, sample_output, explanation, time_limit_ms, memory_limit_mb, created_by_id, created_at)
VALUES (
    'reverse-number', 'Reverse Number', 'EASY', 
    'Given an integer N, reverse its digits. If the number is negative, keep the negative sign.', 
    'A single integer N.', 
    'The reversed integer.', 
    '-10^9 <= N <= 10^9', 
    '123', 
    '321', 
    '123 reversed is 321.', 
    2000, 256, 1, CURRENT_TIMESTAMP
);

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '123', '321', true, 1, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'reverse-number';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '-456', '-654', false, 2, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'reverse-number';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '120', '21', false, 3, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'reverse-number';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '0', '0', false, 4, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'reverse-number';

-- Insert Problem: Maximum of Three
INSERT INTO problems (slug, title, difficulty, description, input_format, output_format, constraints, sample_input, sample_output, explanation, time_limit_ms, memory_limit_mb, created_by_id, created_at)
VALUES (
    'max-of-three', 'Maximum of Three', 'EASY', 
    'Given three integers A, B, and C, find the maximum among them.', 
    'Three space-separated integers A, B, and C.', 
    'The maximum of the three integers.', 
    '-10^9 <= A, B, C <= 10^9', 
    '1 5 3', 
    '5', 
    '5 is the largest.', 
    2000, 256, 1, CURRENT_TIMESTAMP
);

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '1 5 3', '5', true, 1, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'max-of-three';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '-1 -5 -3', '-1', false, 2, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'max-of-three';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '0 0 0', '0', false, 3, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'max-of-three';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '10 10 5', '10', false, 4, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'max-of-three';

-- Insert Problem: Prime Check
INSERT INTO problems (slug, title, difficulty, description, input_format, output_format, constraints, sample_input, sample_output, explanation, time_limit_ms, memory_limit_mb, created_by_id, created_at)
VALUES (
    'prime-check', 'Prime Check', 'EASY', 
    'Given an integer N, check if it is a prime number.', 
    'A single integer N.', 
    'Print ''Yes'' if N is prime, otherwise ''No''.', 
    '1 <= N <= 10^9', 
    '7', 
    'Yes', 
    '7 has no divisors other than 1 and itself.', 
    2000, 256, 1, CURRENT_TIMESTAMP
);

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '7', 'Yes', true, 1, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'prime-check';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '10', 'No', false, 2, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'prime-check';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '1', 'No', false, 3, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'prime-check';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '2', 'Yes', false, 4, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'prime-check';

-- Insert Problem: Fibonacci Number
INSERT INTO problems (slug, title, difficulty, description, input_format, output_format, constraints, sample_input, sample_output, explanation, time_limit_ms, memory_limit_mb, created_by_id, created_at)
VALUES (
    'fibonacci', 'Fibonacci Number', 'EASY', 
    'Given an integer N, find the N-th Fibonacci number. Assume F(0) = 0 and F(1) = 1.', 
    'A single integer N.', 
    'The N-th Fibonacci number.', 
    '0 <= N <= 30', 
    '5', 
    '5', 
    'F(5) = F(4) + F(3) = 3 + 2 = 5.', 
    2000, 256, 1, CURRENT_TIMESTAMP
);

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '5', '5', true, 1, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'fibonacci';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '0', '0', false, 2, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'fibonacci';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '1', '1', false, 3, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'fibonacci';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '10', '55', false, 4, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'fibonacci';

-- Insert Problem: Factorial
INSERT INTO problems (slug, title, difficulty, description, input_format, output_format, constraints, sample_input, sample_output, explanation, time_limit_ms, memory_limit_mb, created_by_id, created_at)
VALUES (
    'factorial', 'Factorial', 'EASY', 
    'Given an integer N, calculate its factorial (N!).', 
    'A single integer N.', 
    'The factorial of N.', 
    '0 <= N <= 20', 
    '5', 
    '120', 
    '5! = 5 * 4 * 3 * 2 * 1 = 120.', 
    2000, 256, 1, CURRENT_TIMESTAMP
);

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '5', '120', true, 1, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'factorial';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '0', '1', false, 2, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'factorial';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '1', '1', false, 3, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'factorial';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '10', '3628800', false, 4, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'factorial';

-- Insert Problem: Palindrome String
INSERT INTO problems (slug, title, difficulty, description, input_format, output_format, constraints, sample_input, sample_output, explanation, time_limit_ms, memory_limit_mb, created_by_id, created_at)
VALUES (
    'palindrome', 'Palindrome String', 'EASY', 
    'Given a string S, check if it is a palindrome.', 
    'A single string S.', 
    'Print ''Yes'' if S is a palindrome, otherwise ''No''.', 
    '1 <= |S| <= 10^5, S contains only lowercase English letters.', 
    'racecar', 
    'Yes', 
    '''racecar'' reads the same forwards and backwards.', 
    2000, 256, 1, CURRENT_TIMESTAMP
);

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, 'racecar', 'Yes', true, 1, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'palindrome';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, 'hello', 'No', false, 2, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'palindrome';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, 'a', 'Yes', false, 3, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'palindrome';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, 'abba', 'Yes', false, 4, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'palindrome';

-- Insert Problem: Two Sum
INSERT INTO problems (slug, title, difficulty, description, input_format, output_format, constraints, sample_input, sample_output, explanation, time_limit_ms, memory_limit_mb, created_by_id, created_at)
VALUES (
    'two-sum', 'Two Sum', 'MEDIUM', 
    'Given an array of integers and a target sum, find two numbers in the array that add up to the target. Print their 0-based indices.', 
    'The first line contains N and Target. The second line contains N space-separated integers.', 
    'Two space-separated indices.', 
    '2 <= N <= 10^5', 
    '4 9
2 7 11 15', 
    '0 1', 
    'arr[0] + arr[1] = 2 + 7 = 9.', 
    2000, 256, 1, CURRENT_TIMESTAMP
);

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '4 9
2 7 11 15', '0 1', true, 1, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'two-sum';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '3 6
3 2 4', '1 2', false, 2, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'two-sum';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '2 5
2 3', '0 1', false, 3, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'two-sum';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '5 10
1 2 3 4 6', '3 4', false, 4, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'two-sum';

-- Insert Problem: Binary Search
INSERT INTO problems (slug, title, difficulty, description, input_format, output_format, constraints, sample_input, sample_output, explanation, time_limit_ms, memory_limit_mb, created_by_id, created_at)
VALUES (
    'binary-search', 'Binary Search', 'MEDIUM', 
    'Given a sorted array of N integers and a target value, return the index if the target is found. If not, return -1.', 
    'The first line contains N and Target. The second line contains N sorted integers.', 
    'The index of the target or -1.', 
    '1 <= N <= 10^5', 
    '6 9
-1 0 3 5 9 12', 
    '4', 
    '9 is at index 4.', 
    2000, 256, 1, CURRENT_TIMESTAMP
);

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '6 9
-1 0 3 5 9 12', '4', true, 1, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'binary-search';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '6 2
-1 0 3 5 9 12', '-1', false, 2, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'binary-search';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '1 5
5', '0', false, 3, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'binary-search';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '2 3
1 3', '1', false, 4, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'binary-search';

-- Insert Problem: Merge Intervals
INSERT INTO problems (slug, title, difficulty, description, input_format, output_format, constraints, sample_input, sample_output, explanation, time_limit_ms, memory_limit_mb, created_by_id, created_at)
VALUES (
    'merge-intervals', 'Merge Intervals', 'MEDIUM', 
    'Given an array of intervals where intervals[i] = [start_i, end_i], merge all overlapping intervals.', 
    'The first line contains N. The next N lines each contain start_i and end_i.', 
    'The merged intervals, each on a new line.', 
    '1 <= N <= 10^4', 
    '4
1 3
2 6
8 10
15 18', 
    '1 6
8 10
15 18', 
    'Intervals [1,3] and [2,6] overlap, so merge them into [1,6].', 
    2000, 256, 1, CURRENT_TIMESTAMP
);

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '4
1 3
2 6
8 10
15 18', '1 6
8 10
15 18', true, 1, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'merge-intervals';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '2
1 4
4 5', '1 5', false, 2, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'merge-intervals';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '1
1 1', '1 1', false, 3, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'merge-intervals';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '3
1 4
2 3
3 5', '1 5', false, 4, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'merge-intervals';

-- Insert Problem: Rotate Array
INSERT INTO problems (slug, title, difficulty, description, input_format, output_format, constraints, sample_input, sample_output, explanation, time_limit_ms, memory_limit_mb, created_by_id, created_at)
VALUES (
    'rotate-array', 'Rotate Array', 'MEDIUM', 
    'Given an integer array nums, rotate the array to the right by k steps, where k is non-negative.', 
    'First line N and k. Second line N integers.', 
    'The rotated array.', 
    '1 <= N <= 10^5', 
    '7 3
1 2 3 4 5 6 7', 
    '5 6 7 1 2 3 4', 
    'Rotate 1 step: [7,1,2,3,4,5,6]. Rotate 3 steps: [5,6,7,1,2,3,4].', 
    2000, 256, 1, CURRENT_TIMESTAMP
);

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '7 3
1 2 3 4 5 6 7', '5 6 7 1 2 3 4', true, 1, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'rotate-array';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '4 2
-1 -100 3 99', '3 99 -1 -100', false, 2, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'rotate-array';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '1 0
1', '1', false, 3, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'rotate-array';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '2 3
1 2', '2 1', false, 4, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'rotate-array';

-- Insert Problem: Valid Parentheses
INSERT INTO problems (slug, title, difficulty, description, input_format, output_format, constraints, sample_input, sample_output, explanation, time_limit_ms, memory_limit_mb, created_by_id, created_at)
VALUES (
    'valid-parentheses', 'Valid Parentheses', 'MEDIUM', 
    'Given a string containing just the characters ''('', '')'', ''{'', ''}'', ''['' and '']'', determine if the input string is valid.', 
    'A single string.', 
    'Print ''Yes'' or ''No''.', 
    '1 <= |S| <= 10^4', 
    '()[]{}', 
    'Yes', 
    'All brackets are closed correctly.', 
    2000, 256, 1, CURRENT_TIMESTAMP
);

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '()[]{}', 'Yes', true, 1, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'valid-parentheses';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '(]', 'No', false, 2, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'valid-parentheses';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '([)]', 'No', false, 3, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'valid-parentheses';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '{[]}', 'Yes', false, 4, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'valid-parentheses';

-- Insert Problem: Longest Common Prefix
INSERT INTO problems (slug, title, difficulty, description, input_format, output_format, constraints, sample_input, sample_output, explanation, time_limit_ms, memory_limit_mb, created_by_id, created_at)
VALUES (
    'longest-common-prefix', 'Longest Common Prefix', 'MEDIUM', 
    'Write a function to find the longest common prefix string amongst an array of strings.', 
    'First line N. Next N lines each contain a string.', 
    'The longest common prefix, or ''NONE'' if empty.', 
    '1 <= N <= 200', 
    '3
flower
flow
flight', 
    'fl', 
    '''fl'' is the common prefix.', 
    2000, 256, 1, CURRENT_TIMESTAMP
);

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '3
flower
flow
flight', 'fl', true, 1, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'longest-common-prefix';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '3
dog
racecar
car', 'NONE', false, 2, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'longest-common-prefix';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '1
apple', 'apple', false, 3, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'longest-common-prefix';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '2
cat
cat', 'cat', false, 4, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'longest-common-prefix';

-- Insert Problem: LRU Cache (Simulation)
INSERT INTO problems (slug, title, difficulty, description, input_format, output_format, constraints, sample_input, sample_output, explanation, time_limit_ms, memory_limit_mb, created_by_id, created_at)
VALUES (
    'lru-cache', 'LRU Cache (Simulation)', 'HARD', 
    'Simulate an LRU cache. Given capacity C and Q queries of format ''PUT key val'' or ''GET key''.', 
    'First line C and Q. Next Q lines contain queries.', 
    'For each GET, print the value or -1.', 
    '1 <= C, Q <= 1000', 
    '2 5
PUT 1 1
PUT 2 2
GET 1
PUT 3 3
GET 2', 
    '1
-1', 
    'After PUT 3, key 2 is evicted.', 
    2000, 256, 1, CURRENT_TIMESTAMP
);

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '2 5
PUT 1 1
PUT 2 2
GET 1
PUT 3 3
GET 2', '1
-1', true, 1, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'lru-cache';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '1 3
PUT 1 1
PUT 2 2
GET 1', '-1', false, 2, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'lru-cache';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '2 4
PUT 1 1
GET 1
PUT 1 2
GET 1', '1
2', false, 3, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'lru-cache';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '2 2
GET 1
GET 2', '-1
-1', false, 4, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'lru-cache';

-- Insert Problem: Shortest Path (Dijkstra)
INSERT INTO problems (slug, title, difficulty, description, input_format, output_format, constraints, sample_input, sample_output, explanation, time_limit_ms, memory_limit_mb, created_by_id, created_at)
VALUES (
    'dijkstra', 'Shortest Path (Dijkstra)', 'HARD', 
    'Given a weighted directed graph with N nodes and M edges, find the shortest path from node 1 to node N.', 
    'First line N and M. Next M lines: u v w.', 
    'Shortest path distance or -1.', 
    '1 <= N <= 1000, 1 <= M <= 10^4', 
    '3 3
1 2 2
2 3 1
1 3 5', 
    '3', 
    'Path 1->2->3 has weight 3.', 
    2000, 256, 1, CURRENT_TIMESTAMP
);

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '3 3
1 2 2
2 3 1
1 3 5', '3', true, 1, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'dijkstra';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '3 1
1 2 1', '-1', false, 2, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'dijkstra';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '2 1
1 2 10', '10', false, 3, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'dijkstra';

INSERT INTO test_cases (problem_id, input, expected_output, is_sample, order_index, created_at)
SELECT id, '4 4
1 2 1
2 3 1
3 4 1
1 4 5', '3', false, 4, CURRENT_TIMESTAMP
FROM problems WHERE slug = 'dijkstra';
