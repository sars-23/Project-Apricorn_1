-- V10: Function Contract Metadata and Testcase JSONB Migration

-- 1. Add Function Contract Metadata columns
ALTER TABLE problems ADD COLUMN method_name VARCHAR(255);
ALTER TABLE problems ADD COLUMN return_type VARCHAR(255);
ALTER TABLE problems ADD COLUMN parameter_metadata JSONB;
ALTER TABLE problems ADD COLUMN starter_code TEXT;
ALTER TABLE problems ADD COLUMN is_visible BOOLEAN DEFAULT TRUE;

-- 2. Hide lru-cache
UPDATE problems SET is_visible = false WHERE slug = 'lru-cache';

-- 3. Migrate canonical problems (metadata + testcases)

-- a-b
UPDATE problems SET 
    method_name = 'add', return_type = 'int',
    parameter_metadata = '[{"name": "a", "type": "int"}, {"name": "b", "type": "int"}]'::jsonb,
    starter_code = 'class Solution {
    public int add(int a, int b) {
        
    }
}'
WHERE slug = 'a-b';
UPDATE test_cases SET input = '[2, 3]', expected_output = '5' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'a-b') AND order_index = 1;

-- even-odd
UPDATE problems SET 
    method_name = 'isEven', return_type = 'boolean',
    parameter_metadata = '[{"name": "n", "type": "int"}]'::jsonb,
    starter_code = 'class Solution {
    public boolean isEven(int n) {
        
    }
}'
WHERE slug = 'even-odd';
UPDATE test_cases SET input = '[4]', expected_output = 'true' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'even-odd') AND order_index = 1;
UPDATE test_cases SET input = '[7]', expected_output = 'false' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'even-odd') AND order_index = 2;
UPDATE test_cases SET input = '[0]', expected_output = 'true' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'even-odd') AND order_index = 3;
UPDATE test_cases SET input = '[-3]', expected_output = 'false' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'even-odd') AND order_index = 4;

-- reverse-number
UPDATE problems SET 
    method_name = 'reverse', return_type = 'int',
    parameter_metadata = '[{"name": "n", "type": "int"}]'::jsonb,
    starter_code = 'class Solution {
    public int reverse(int n) {
        
    }
}'
WHERE slug = 'reverse-number';
UPDATE test_cases SET input = '[123]', expected_output = '321' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'reverse-number') AND order_index = 1;
UPDATE test_cases SET input = '[-456]', expected_output = '-654' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'reverse-number') AND order_index = 2;
UPDATE test_cases SET input = '[120]', expected_output = '21' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'reverse-number') AND order_index = 3;
UPDATE test_cases SET input = '[0]', expected_output = '0' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'reverse-number') AND order_index = 4;

-- max-of-three
UPDATE problems SET 
    method_name = 'max', return_type = 'int',
    parameter_metadata = '[{"name": "a", "type": "int"}, {"name": "b", "type": "int"}, {"name": "c", "type": "int"}]'::jsonb,
    starter_code = 'class Solution {
    public int max(int a, int b, int c) {
        
    }
}'
WHERE slug = 'max-of-three';
UPDATE test_cases SET input = '[1, 5, 3]', expected_output = '5' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'max-of-three') AND order_index = 1;
UPDATE test_cases SET input = '[-1, -5, -3]', expected_output = '-1' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'max-of-three') AND order_index = 2;
UPDATE test_cases SET input = '[4, 4, 4]', expected_output = '4' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'max-of-three') AND order_index = 3;
UPDATE test_cases SET input = '[10, -10, 0]', expected_output = '10' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'max-of-three') AND order_index = 4;

-- prime-check
UPDATE problems SET 
    method_name = 'isPrime', return_type = 'boolean',
    parameter_metadata = '[{"name": "n", "type": "int"}]'::jsonb,
    starter_code = 'class Solution {
    public boolean isPrime(int n) {
        
    }
}'
WHERE slug = 'prime-check';
UPDATE test_cases SET input = '[7]', expected_output = 'true' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'prime-check') AND order_index = 1;
UPDATE test_cases SET input = '[12]', expected_output = 'false' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'prime-check') AND order_index = 2;
UPDATE test_cases SET input = '[1]', expected_output = 'false' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'prime-check') AND order_index = 3;
UPDATE test_cases SET input = '[2]', expected_output = 'true' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'prime-check') AND order_index = 4;

-- fibonacci
UPDATE problems SET 
    method_name = 'fib', return_type = 'int',
    parameter_metadata = '[{"name": "n", "type": "int"}]'::jsonb,
    starter_code = 'class Solution {
    public int fib(int n) {
        
    }
}'
WHERE slug = 'fibonacci';
UPDATE test_cases SET input = '[5]', expected_output = '5' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'fibonacci') AND order_index = 1;
UPDATE test_cases SET input = '[0]', expected_output = '0' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'fibonacci') AND order_index = 2;
UPDATE test_cases SET input = '[1]', expected_output = '1' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'fibonacci') AND order_index = 3;
UPDATE test_cases SET input = '[10]', expected_output = '55' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'fibonacci') AND order_index = 4;

-- factorial
UPDATE problems SET 
    method_name = 'factorial', return_type = 'long',
    parameter_metadata = '[{"name": "n", "type": "int"}]'::jsonb,
    starter_code = 'class Solution {
    public long factorial(int n) {
        
    }
}'
WHERE slug = 'factorial';
UPDATE test_cases SET input = '[5]', expected_output = '120' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'factorial') AND order_index = 1;
UPDATE test_cases SET input = '[0]', expected_output = '1' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'factorial') AND order_index = 2;
UPDATE test_cases SET input = '[1]', expected_output = '1' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'factorial') AND order_index = 3;
UPDATE test_cases SET input = '[10]', expected_output = '3628800' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'factorial') AND order_index = 4;

-- palindrome
UPDATE problems SET 
    method_name = 'isPalindrome', return_type = 'boolean',
    parameter_metadata = '[{"name": "s", "type": "String"}]'::jsonb,
    starter_code = 'class Solution {
    public boolean isPalindrome(String s) {
        
    }
}'
WHERE slug = 'palindrome';
UPDATE test_cases SET input = '["racecar"]', expected_output = 'true' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'palindrome') AND order_index = 1;
UPDATE test_cases SET input = '["hello"]', expected_output = 'false' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'palindrome') AND order_index = 2;
UPDATE test_cases SET input = '["a"]', expected_output = 'true' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'palindrome') AND order_index = 3;
UPDATE test_cases SET input = '["noon"]', expected_output = 'true' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'palindrome') AND order_index = 4;

-- two-sum
UPDATE problems SET 
    method_name = 'twoSum', return_type = 'int[]',
    parameter_metadata = '[{"name": "nums", "type": "int[]"}, {"name": "target", "type": "int"}]'::jsonb,
    starter_code = 'class Solution {
    public int[] twoSum(int[] nums, int target) {
        
    }
}'
WHERE slug = 'two-sum';
UPDATE test_cases SET input = '[[2, 7, 11, 15], 9]', expected_output = '[0, 1]' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'two-sum') AND order_index = 1;
UPDATE test_cases SET input = '[[3, 2, 4], 6]', expected_output = '[1, 2]' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'two-sum') AND order_index = 2;
UPDATE test_cases SET input = '[[2, 3], 5]', expected_output = '[0, 1]' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'two-sum') AND order_index = 3;
UPDATE test_cases SET input = '[[1, 2, 3, 4, 6], 10]', expected_output = '[3, 4]' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'two-sum') AND order_index = 4;

-- binary-search
UPDATE problems SET 
    method_name = 'search', return_type = 'int',
    parameter_metadata = '[{"name": "nums", "type": "int[]"}, {"name": "target", "type": "int"}]'::jsonb,
    starter_code = 'class Solution {
    public int search(int[] nums, int target) {
        
    }
}'
WHERE slug = 'binary-search';
UPDATE test_cases SET input = '[[-1, 0, 3, 5, 9, 12], 9]', expected_output = '4' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'binary-search') AND order_index = 1;
UPDATE test_cases SET input = '[[-1, 0, 3, 5, 9, 12], 2]', expected_output = '-1' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'binary-search') AND order_index = 2;
UPDATE test_cases SET input = '[[5], 5]', expected_output = '0' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'binary-search') AND order_index = 3;
UPDATE test_cases SET input = '[[5], -5]', expected_output = '-1' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'binary-search') AND order_index = 4;

-- merge-intervals
UPDATE problems SET 
    method_name = 'merge', return_type = 'int[][]',
    parameter_metadata = '[{"name": "intervals", "type": "int[][]"}]'::jsonb,
    starter_code = 'class Solution {
    public int[][] merge(int[][] intervals) {
        
    }
}'
WHERE slug = 'merge-intervals';
UPDATE test_cases SET input = '[[[1, 3], [2, 6], [8, 10], [15, 18]]]', expected_output = '[[1, 6], [8, 10], [15, 18]]' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'merge-intervals') AND order_index = 1;
UPDATE test_cases SET input = '[[[1, 4], [4, 5]]]', expected_output = '[[1, 5]]' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'merge-intervals') AND order_index = 2;
UPDATE test_cases SET input = '[[[1, 1]]]', expected_output = '[[1, 1]]' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'merge-intervals') AND order_index = 3;
UPDATE test_cases SET input = '[[[1, 4], [2, 3], [3, 5]]]', expected_output = '[[1, 5]]' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'merge-intervals') AND order_index = 4;

-- rotate-array
UPDATE problems SET 
    method_name = 'rotate', return_type = 'int[]',
    parameter_metadata = '[{"name": "nums", "type": "int[]"}, {"name": "k", "type": "int"}]'::jsonb,
    starter_code = 'class Solution {
    public int[] rotate(int[] nums, int k) {
        
    }
}'
WHERE slug = 'rotate-array';
UPDATE test_cases SET input = '[[1, 2, 3, 4, 5, 6, 7], 3]', expected_output = '[5, 6, 7, 1, 2, 3, 4]' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'rotate-array') AND order_index = 1;
UPDATE test_cases SET input = '[[-1, -100, 3, 99], 2]', expected_output = '[3, 99, -1, -100]' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'rotate-array') AND order_index = 2;
UPDATE test_cases SET input = '[[1, 2], 3]', expected_output = '[2, 1]' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'rotate-array') AND order_index = 3;
UPDATE test_cases SET input = '[[1], 0]', expected_output = '[1]' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'rotate-array') AND order_index = 4;

-- valid-parentheses
UPDATE problems SET 
    method_name = 'isValid', return_type = 'boolean',
    parameter_metadata = '[{"name": "s", "type": "String"}]'::jsonb,
    starter_code = 'class Solution {
    public boolean isValid(String s) {
        
    }
}'
WHERE slug = 'valid-parentheses';
UPDATE test_cases SET input = '["()[]{}"]', expected_output = 'true' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'valid-parentheses') AND order_index = 1;
UPDATE test_cases SET input = '["(]"]', expected_output = 'false' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'valid-parentheses') AND order_index = 2;
UPDATE test_cases SET input = '["([)]"]', expected_output = 'false' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'valid-parentheses') AND order_index = 3;
UPDATE test_cases SET input = '["{[]}"]', expected_output = 'true' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'valid-parentheses') AND order_index = 4;

-- longest-common-prefix
UPDATE problems SET 
    method_name = 'longestCommonPrefix', return_type = 'String',
    parameter_metadata = '[{"name": "strs", "type": "String[]"}]'::jsonb,
    starter_code = 'class Solution {
    public String longestCommonPrefix(String[] strs) {
        
    }
}'
WHERE slug = 'longest-common-prefix';
UPDATE test_cases SET input = '[["flower", "flow", "flight"]]', expected_output = '"fl"' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'longest-common-prefix') AND order_index = 1;
UPDATE test_cases SET input = '[["dog", "racecar", "car"]]', expected_output = '""' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'longest-common-prefix') AND order_index = 2;
UPDATE test_cases SET input = '[["a"]]', expected_output = '"a"' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'longest-common-prefix') AND order_index = 3;
UPDATE test_cases SET input = '[["ab", "a"]]', expected_output = '"a"' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'longest-common-prefix') AND order_index = 4;

-- dijkstra
UPDATE problems SET 
    method_name = 'shortestPath', return_type = 'int',
    parameter_metadata = '[{"name": "n", "type": "int"}, {"name": "edges", "type": "int[][]"}]'::jsonb,
    starter_code = 'class Solution {
    public int shortestPath(int n, int[][] edges) {
        
    }
}'
WHERE slug = 'dijkstra';
UPDATE test_cases SET input = '[3, [[1, 2, 2], [2, 3, 1], [1, 3, 5]]]', expected_output = '3' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'dijkstra') AND order_index = 1;
UPDATE test_cases SET input = '[3, [[1, 2, 1]]]', expected_output = '-1' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'dijkstra') AND order_index = 2;
UPDATE test_cases SET input = '[2, [[1, 2, 10]]]', expected_output = '10' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'dijkstra') AND order_index = 3;
UPDATE test_cases SET input = '[4, [[1, 2, 1], [2, 3, 1], [3, 4, 1], [1, 4, 5]]]', expected_output = '3' WHERE problem_id = (SELECT id FROM problems WHERE slug = 'dijkstra') AND order_index = 4;
