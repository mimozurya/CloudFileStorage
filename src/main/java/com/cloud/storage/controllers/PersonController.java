package com.cloud.storage.controllers;

import com.cloud.storage.dto.person.PersonRequest;
import com.cloud.storage.dto.person.PersonResponse;
import com.cloud.storage.services.person.RegistrationService;
import com.cloud.storage.services.resource.FileService;
import com.cloud.storage.services.person.PersonService;
import com.cloud.storage.services.person.AuthService;
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

    @PostMapping("/auth/sign-in")
    public ResponseEntity<PersonResponse> signIn(@RequestBody PersonRequest personRequest,
                                                 HttpServletRequest httpRequest) {
        return authService.login(personRequest, httpRequest);
    }

    @PostMapping("/auth/sign-out")
    public ResponseEntity<PersonResponse> signOut(HttpServletRequest httpRequest) {
        return authService.logout(httpRequest);
    }

    @GetMapping("/user/me")
    public ResponseEntity<PersonResponse> getCurrentPerson() {
        return personService.getPersonBySession();
    }
}
