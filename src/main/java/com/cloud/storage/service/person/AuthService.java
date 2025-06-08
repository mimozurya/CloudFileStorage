package com.cloud.storage.service.person;

import com.cloud.storage.dto.ErrorResponse;
import com.cloud.storage.dto.person.PersonRequest;
import com.cloud.storage.dto.person.PersonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;

    public ResponseEntity<PersonResponse> login(PersonRequest person, HttpServletRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(person.username(), person.password()));

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            HttpSession session = createSession(request, securityContext);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE,
                            String.format("JSESSIONID=%s; Path=/; HttpOnly; SameSite=Lax; Max-Age=%d",
                                    session.getId(), 3600))
                    .body(new PersonResponse(person.username()));
        } catch (AuthenticationException e) {
            throw new AuthenticationException("Authentication failed") {};
        }
    }

    public ResponseEntity<?> logout(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("No active session found"));
        }

        deleteSession(request);
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, "JSESSIONID=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax")
                .body(new PersonResponse(authentication.getName()));
    }

    private HttpSession createSession(HttpServletRequest request, SecurityContext securityContext) {
        HttpSession session = request.getSession(true);
        session.setMaxInactiveInterval(360);
        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);

        return session;
    }

    private void deleteSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
