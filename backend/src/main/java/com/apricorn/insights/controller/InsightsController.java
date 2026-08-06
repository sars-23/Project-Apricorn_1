package com.apricorn.insights.controller;

import com.apricorn.insights.service.InsightsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InsightsController {
    private final InsightsService insightsService;

    public InsightsController(InsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @GetMapping("/insights")
    public String insights(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("insights", insightsService.getUserInsights(userDetails.getUsername()));
        return "pages/insights";
    }
}
