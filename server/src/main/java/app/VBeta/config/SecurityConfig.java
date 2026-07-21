package app.VBeta.config;

import app.VBeta.config.security.FirebaseAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * {@code SecurityConfig} defines HTTP security policy for the API.
 * <p>
 * It configures stateless token-based authentication, enables CORS integration,
 * and inserts the {@link FirebaseAuthFilter} into Spring Security's filter chain.
 */
@Configuration
public class SecurityConfig {

    /**
     * Builds the primary {@link SecurityFilterChain} for API requests.
     * <p>
     * This chain disables CSRF for stateless API usage, permits selected public endpoints,
     * and requires authentication for all other routes.
     *
     * @param http Spring Security HTTP builder
     * @param firebaseAuthFilter Firebase token authentication filter
     * @return configured security filter chain
     * @throws Exception when the security chain cannot be built
     */
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
                        .requestMatchers("/search/**").permitAll()

                        // Everything else requires authentication
                        .anyRequest().authenticated())
                .addFilterBefore(firebaseAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
