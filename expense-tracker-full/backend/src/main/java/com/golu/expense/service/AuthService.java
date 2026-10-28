
package com.golu.expense.service;

import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.golu.expense.dto.AuthResponse;
import com.golu.expense.dto.LoginRequest;
import com.golu.expense.dto.SignupRequest;
import com.golu.expense.dto.UserResponse;
import com.golu.expense.entity.ExpenseCategory;
import com.golu.expense.entity.User;
import com.golu.expense.exception.ApiException;
import com.golu.expense.repository.CategoryRepository;
import com.golu.expense.repository.UserRepository;
import com.golu.expense.security.JwtService;

@Service
public class AuthService {

    private final UserRepository users;
    private final CategoryRepository categories;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;

    public AuthService(
            UserRepository users,
            CategoryRepository categories,
            PasswordEncoder encoder,
            AuthenticationManager authManager,
            JwtService jwt) {

        this.users = users;
        this.categories = categories;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if (users.existsByEmail(email)) {
            throw ApiException.conflict(
                "An account with this email already exists."
            );
        }

        String passwordHash = encoder.encode(request.getPassword());

        User user = request.toEntity(passwordHash);
        users.save(user);

        List<String> defaultCategories = List.of(
            "Food & dining",
            "Transport",
            "Shopping",
            "Bills & utilities",
            "Entertainment",
            "Health",
            "Other"
        );

        for (String name : defaultCategories) {
            ExpenseCategory category = new ExpenseCategory();
            category.setName(name);
            category.setUser(user);
            categories.save(category);
        }

        return createResponse(user);
    }

    public AuthResponse login(LoginRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                email,
                request.getPassword()
            )
        );

        User user = users.findByEmail(email).orElseThrow();

        return createResponse(user);
    }

    private AuthResponse createResponse(User user) {

        String token = jwt.generateToken(user);

        UserResponse userResponse = UserResponse.fromEntity(user);

        return new AuthResponse(
            token,
            "Bearer",
            jwt.getExpirationMs(),
            userResponse
        );
    }
}
