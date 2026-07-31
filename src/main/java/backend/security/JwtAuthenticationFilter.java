package backend.security;

import backend.entity.User;
import backend.repository.UserRepository;
import backend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Không có JWT thì cho đi tiếp
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);

        try {

            String email = jwtService.extractUsername(jwt);

            if (email != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                User user = userRepository.findByEmail(email).orElse(null);

                if (user != null) {

                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();

                    switch (user.getRoleId()) {
                        case 1 ->
                                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

                        case 2 ->
                                authorities.add(new SimpleGrantedAuthority("ROLE_TEACHER"));

                        default ->
                                authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
                    }

                    System.out.println("========== JWT AUTH ==========");
                    System.out.println("EMAIL      : " + email);
                    System.out.println("USER ID    : " + user.getId());
                    System.out.println("FULL NAME  : " + user.getFullName());
                    System.out.println("ROLE ID    : " + user.getRoleId());
                    System.out.println("AUTHORITIES: " + authorities);
                    System.out.println("==============================");

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    user,          // <-- Lưu User thay vì String email
                                    null,
                                    authorities
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

        } catch (Exception ex) {
            System.out.println("JWT Authentication Error: " + ex.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}