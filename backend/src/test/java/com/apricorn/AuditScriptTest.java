package com.apricorn;

import com.apricorn.problem.entity.Problem;
import com.apricorn.problem.repository.ProblemRepository;
import com.apricorn.submission.entity.Submission;
import com.apricorn.submission.entity.SubmissionStatus;
import com.apricorn.submission.entity.Verdict;
import com.apricorn.submission.service.SubmissionService;
import com.apricorn.user.entity.Role;
import com.apricorn.user.entity.User;
import com.apricorn.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class AuditScriptTest {

    @Autowired
    private ProblemRepository problemRepository;
    @Autowired
    private SubmissionService submissionService;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.apricorn.judge.service.JudgeService judgeService;
    @Autowired
    private com.apricorn.submission.repository.SubmissionRepository submissionRepository;

    @Test
    public void runAudit() throws Exception {
        // Self-seed admin user for H2 test DB (production PostgreSQL already has one via V6).
        User user = userRepository.findByUsername("admin").orElseGet(() -> {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@apricorn.com");
            admin.setPassword("$2a$12$placeholder_hash_for_test_only");
            admin.setRole(Role.ADMIN);
            return userRepository.saveAndFlush(admin);
        });

        List<Problem> problems = problemRepository.findAll();
        
        Map<String, String> solutions = Map.ofEntries(
            Map.entry("a-b", "class Solution { public int add(int a, int b) { return a+b; } }"),
            Map.entry("even-odd", "class Solution { public boolean isEven(int n) { return n%2==0; } }"),
            Map.entry("reverse-number", "class Solution { public int reverse(int n) { int res=0; while(n!=0){ res=res*10+n%10; n/=10; } return res; } }"),
            Map.entry("max-of-three", "class Solution { public int max(int a, int b, int c) { return Math.max(a, Math.max(b, c)); } }"),
            Map.entry("prime-check", "class Solution { public boolean isPrime(int n) { if(n<=1) return false; for(int i=2; i*i<=n; i++) { if(n%i==0) return false; } return true; } }"),
            Map.entry("fibonacci", "class Solution { public int fib(int n) { if(n<=1) return n; return fib(n-1)+fib(n-2); } }"),
            Map.entry("factorial", "class Solution { public long factorial(int n) { long res=1; for(int i=2; i<=n; i++) res*=i; return res; } }"),
            Map.entry("palindrome", "class Solution { public boolean isPalindrome(String s) { return new StringBuilder(s).reverse().toString().equals(s); } }"),
            Map.entry("two-sum", "class Solution { public int[] twoSum(int[] nums, int target) { for(int i=0;i<nums.length;i++) for(int j=i+1;j<nums.length;j++) if(nums[i]+nums[j]==target) return new int[]{i,j}; return new int[0]; } }"),
            Map.entry("binary-search", "class Solution { public int search(int[] nums, int target) { int l=0, r=nums.length-1; while(l<=r) { int m = l+(r-l)/2; if(nums[m]==target) return m; if(nums[m]<target) l=m+1; else r=m-1; } return -1; } }"),
            Map.entry("merge-intervals", "class Solution { public int[][] merge(int[][] intervals) { java.util.Arrays.sort(intervals, (a,b)->Integer.compare(a[0], b[0])); java.util.List<int[]> merged = new java.util.ArrayList<>(); for(int[] interval : intervals) { if(merged.isEmpty() || merged.get(merged.size()-1)[1] < interval[0]) { merged.add(interval); } else { merged.get(merged.size()-1)[1] = Math.max(merged.get(merged.size()-1)[1], interval[1]); } } return merged.toArray(new int[merged.size()][]); } }"),
            Map.entry("rotate-array", "class Solution { public int[] rotate(int[] nums, int k) { k %= nums.length; int[] res = new int[nums.length]; for(int i=0;i<nums.length;i++) res[(i+k)%nums.length] = nums[i]; return res; } }"),
            Map.entry("valid-parentheses", "class Solution { public boolean isValid(String s) { java.util.Stack<Character> st = new java.util.Stack<>(); for(char c : s.toCharArray()) { if(c=='('||c=='{'||c=='[') st.push(c); else { if(st.isEmpty()) return false; char top = st.pop(); if((c==')'&&top!='(')||(c=='}'&&top!='{')||(c==']'&&top!='[')) return false; } } return st.isEmpty(); } }"),
            Map.entry("longest-common-prefix", "class Solution { public String longestCommonPrefix(String[] strs) { if(strs.length==0) return \"\"; String prefix = strs[0]; for(int i=1;i<strs.length;i++) { while(strs[i].indexOf(prefix) != 0) { prefix = prefix.substring(0, prefix.length()-1); if(prefix.isEmpty()) return \"\"; } } return prefix; } }"),
            Map.entry("dijkstra", "class Solution { public int shortestPath(int n, int[][] edges) { int[] dist = new int[n+1]; java.util.Arrays.fill(dist, Integer.MAX_VALUE); dist[1] = 0; boolean[] vis = new boolean[n+1]; for(int i=1; i<=n; i++) { int u = -1; for(int j=1; j<=n; j++) { if(!vis[j] && (u==-1 || dist[j] < dist[u])) u = j; } if(dist[u] == Integer.MAX_VALUE) break; vis[u] = true; for(int[] e : edges) { if(e[0] == u && dist[u] + e[2] < dist[e[1]]) dist[e[1]] = dist[u] + e[2]; if(e[1] == u && dist[u] + e[2] < dist[e[0]]) dist[e[0]] = dist[u] + e[2]; } } return dist[n] == Integer.MAX_VALUE ? -1 : dist[n]; } }")
        );

        for (Problem problem : problems) {
            if (!problem.isVisible()) continue;
            System.out.println("EVALUATING: " + problem.getSlug());
            String code = solutions.get(problem.getSlug());
            if (code == null) {
                System.out.println("NO CODE FOR: " + problem.getSlug());
                continue;
            }

            Submission sub = new Submission();
            sub.setProblem(problem);
            sub.setUser(user);
            sub.setSourceCode(code);
            sub.setLanguage(com.apricorn.submission.entity.Language.JAVA);
            sub.setStatus(SubmissionStatus.PENDING);
            sub = submissionRepository.save(sub);

            judgeService.evaluate(sub.getId());

            int attempts = 0;
            while (attempts < 50) {
                sub = submissionRepository.findById(sub.getId()).get();
                if (sub.getStatus() == SubmissionStatus.COMPLETED || sub.getVerdict() != null) {
                    break;
                }
                Thread.sleep(500);
                attempts++;
            }

            System.out.println("RESULT: " + problem.getSlug() + " -> " + sub.getVerdict() + " | TIME: " + sub.getExecutionTimeMs() + "ms");
            if (sub.getVerdict() != Verdict.ACCEPTED) {
                System.out.println("FAILED! Error details: " + sub.getErrorMessage());
            }
            assertThat(sub.getVerdict()).isEqualTo(Verdict.ACCEPTED);
        }
    }
}
