package com.example.books.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * STARTER CODE — Complete this class for the Spring Security lab.
 *
 * TODO List:
 *  1. Add a @Bean method that returns a PasswordEncoder (which implementation?)
 *  2. Add a @Bean method that returns a UserDetailsService with 3 in-memory users:
 *       alice   / member123    / MEMBER
 *       bob     / librarian123 / LIBRARIAN
 *       carol   / admin123     / ADMIN
 *     (All passwords must be encoded using the PasswordEncoder bean)
 *  3. Add a @Bean method that returns a SecurityFilterChain.
 *     Inside HttpSecurity:
 *       - Disable CSRF
 *       - Apply the access rules from your access-control-matrix.md
 *       - Set session management to STATELESS
 *       - Enable HTTP Basic Auth
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // TODO: Bean 1 — PasswordEncoder

    // TODO: Bean 2 — UserDetailsService (inject PasswordEncoder as a parameter)

    // TODO: Bean 3 — SecurityFilterChain (inject HttpSecurity as a parameter)
    //       Signature: public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception { ... }
}
