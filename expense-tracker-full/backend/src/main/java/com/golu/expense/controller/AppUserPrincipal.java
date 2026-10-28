package com.golu.expense.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.golu.expense.exception.ApiException;
import com.golu.expense.repository.UserRepository;

@Component
public class AppUserPrincipal {

    private final UserRepository users;

    public AppUserPrincipal(UserRepository users) {
        this.users = users;
    }

    public Long id(Authentication auth) {
        String email = ((UserDetails) auth.getPrincipal()).getUsername();
        return users.findByEmail(email).map(u -> u.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found."));
    }
}
