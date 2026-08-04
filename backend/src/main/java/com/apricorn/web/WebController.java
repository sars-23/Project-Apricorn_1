package com.apricorn.web;

import com.apricorn.analytics.service.AnalyticsService;
import com.apricorn.problem.service.ProblemService;
import com.apricorn.problem.service.TagService;
import com.apricorn.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebController {

    private final AnalyticsService analyticsService;
    private final UserService userService;
    private final ProblemService problemService;
    private final TagService tagService;

    public WebController(AnalyticsService analyticsService, UserService userService, ProblemService problemService,
            TagService tagService) {
        this.analyticsService = analyticsService;
        this.userService = userService;
        this.problemService = problemService;
        this.tagService = tagService;
    }

    @GetMapping("/")
    public String home() {
        return "pages/home";
    }

    @GetMapping("/login")
    public String login() {
        return "pages/login";
    }

    @GetMapping("/register")
    public String register() {
        return "pages/register";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("dashboard", analyticsService.getUserDashboard(userDetails.getUsername()));
        return "pages/dashboard";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("profile", userService.getCurrentUser(userDetails.getUsername()));
        model.addAttribute("recentSubmissions",
                analyticsService.getUserDashboard(userDetails.getUsername()).recentSubmissions());
        return "pages/profile";
    }

    @GetMapping("/problems")
    public String problems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        model.addAttribute("problemPage", problemService.getProblems(null, null, null, page, size, null));
        model.addAttribute("tags", tagService.getAllTags());
        return "pages/problems";
    }

    @GetMapping("/problems/{slug}")
    public String workspace(@org.springframework.web.bind.annotation.PathVariable String slug, Model model) {
        model.addAttribute("problem", problemService.getProblemBySlug(slug));
        return "pages/workspace";
    }
}
