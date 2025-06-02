package com.cloud.storage.controllers;

import com.cloud.storage.dto.person.PersonRequest;
import com.cloud.storage.dto.person.PersonResponse;
import com.cloud.storage.services.person.AuthService;
import com.cloud.storage.services.person.PersonService;
import com.cloud.storage.services.person.RegistrationService;
import com.cloud.storage.services.resource.FileService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PersonController {
    private final PersonService personService;
    private final FileService fileService;
    private final AuthService authService;
    private final RegistrationService registrationService;

    @Operation(summary = "User Registration", description = "Returns registration user response")
    @PostMapping("/auth/sign-up")
    public ResponseEntity<PersonResponse> signUp(@RequestBody PersonRequest personRequest,
                                                 HttpServletRequest httpRequest) throws Exception {
        ResponseEntity<PersonResponse> response = registrationService.registration(personRequest, httpRequest);

        if (response.getStatusCode().is2xxSuccessful()) {
            Integer userId = personService.getPersonById();
            fileService.createUserBucket(userId);
        }

        return response;
    }

    @Operation(summary = "User Login", description = "Returns login user response")
    @PostMapping("/auth/sign-in")
    public ResponseEntity<PersonResponse> signIn(@RequestBody PersonRequest personRequest,
                                                 HttpServletRequest httpRequest) {
        return authService.login(personRequest, httpRequest);
    }

    @Operation(summary = "User Logout", description = "Returns logout user response")
    @PostMapping("/auth/sign-out")
    public ResponseEntity<PersonResponse> signOut(HttpServletRequest httpRequest) {
        return authService.logout(httpRequest);
    }

    @Operation(summary = "User information", description = "Returns user information from session")
    @GetMapping("/user/me")
    public ResponseEntity<PersonResponse> getCurrentPerson() {
        return personService.getPersonBySession();
    }
}
