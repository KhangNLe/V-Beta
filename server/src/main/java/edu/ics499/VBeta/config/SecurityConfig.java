package edu.ics499.VBeta.config;

import edu.ics499.VBeta.config.security.FirebaseAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, FirebaseAuthFilter firebaseAuthFilter)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // We want to be stateless since we're using Firebase token auth, so we don't need HTTP sessions at all
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health").permitAll() // Allow unauthenticated access to the health endpoint for monitoring tools like Heroku and uptime robots to check if the server is running
                        .requestMatchers("/api/v1/meta").permitAll()
                        // guest or public read only browsing
                        .requestMatchers("/home/wall-sections").permitAll()
                        .requestMatchers("/home/wall-sections/*/problems").permitAll()
                        .requestMatchers("/home/wall-sections/*/problems/*").permitAll()

                        // Everything else requires authentication
                        .anyRequest().authenticated())
                .addFilterBefore(firebaseAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
