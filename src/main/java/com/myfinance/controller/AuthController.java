package com.myfinance.controller;

import com.myfinance.dto.RegistrationRequest;
import com.myfinance.exception.BusinessValidationException;
import com.myfinance.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String home() { return "redirect:/dashboard"; }

    @GetMapping("/login")
    public String login() { return "auth/login"; }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registrationRequest", new RegistrationRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registrationRequest") RegistrationRequest request,
                               BindingResult result, Model model) {
        if (result.hasErrors()) return "auth/register";
        try {
            userService.register(request);
            return "redirect:/login?registered";
        } catch (BusinessValidationException ex) {
            model.addAttribute("registrationError", ex.getMessage());
            model.addAttribute("fieldErrors", ex.getFieldErrors());
            return "auth/register";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("registrationError", ex.getMessage());
            return "auth/register";
        }
    }
}
