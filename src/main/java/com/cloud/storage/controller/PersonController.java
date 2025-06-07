package com.cloud.storage.controller;

import com.cloud.storage.dto.person.PersonRequest;
import com.cloud.storage.dto.person.PersonResponse;
import com.cloud.storage.service.person.AuthService;
import com.cloud.storage.service.person.PersonService;
import com.cloud.storage.service.person.RegistrationService;
import com.cloud.storage.service.resource.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "User Controller", description = "A controller for working with personal data and sessions")
public class PersonController {
    private final PersonService personService;
    private final FileService fileService;
    private final AuthService authService;
    private final RegistrationService registrationService;

    @Operation(summary = "User Registration", description = "Returns registration user response")
    @PostMapping("/auth/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<?> signUp(@RequestBody PersonRequest personRequest,
                                    HttpServletRequest httpRequest) throws Exception {
        ResponseEntity<?> response = registrationService.registration(personRequest, httpRequest);

        if (response.getStatusCode().is2xxSuccessful()) {
            Integer userId = personService.getPersonById();
            fileService.createUserBucket(userId);
        }

        return response;
    }

    @Operation(summary = "User Login", description = "Returns login user response")
    @PostMapping("/auth/sign-in")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<PersonResponse> signIn(@RequestBody PersonRequest personRequest,
                                                 HttpServletRequest httpRequest) {
        return authService.login(personRequest, httpRequest);
    }

    @Operation(summary = "User Logout", description = "Returns logout user response")
    @PostMapping("/auth/sign-out")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<?> signOut(HttpServletRequest httpRequest) {
        return authService.logout(httpRequest);
    }

    @Operation(summary = "User information", description = "Returns user information from session")
    @GetMapping("/user/me")
    @ResponseStatus(HttpStatus.OK)
    public PersonResponse getCurrentPerson() {
        return personService.getPersonBySession();
    }
}
