package com.apricorn.journey.controller;

import com.apricorn.journey.service.JourneyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/problems")
public class JourneyController {
    private final JourneyService journeyService;

    public JourneyController(JourneyService journeyService) {
        this.journeyService = journeyService;
    }

    @GetMapping("/{slug}/journey")
    public String solutionJourney(
            @PathVariable String slug,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        model.addAttribute("journey", journeyService.getSolutionJourney(userDetails.getUsername(), slug));
        return "pages/journey";
    }
}
