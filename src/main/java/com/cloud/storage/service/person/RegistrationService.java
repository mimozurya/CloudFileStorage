package com.cloud.storage.service.person;

import com.cloud.storage.dto.person.PersonRequest;
import com.cloud.storage.dto.person.PersonResponse;
import com.cloud.storage.model.Person;
import com.cloud.storage.repository.PersonRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final PersonRepository personRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public ResponseEntity<?> registration(PersonRequest person, HttpServletRequest request) {
        if (personRepository.existsByUsername(person.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new PersonResponse("Person is already exist"));
        }

        Person newPerson = new Person(person.username(), passwordEncoder.encode(person.password()), "ROLE_USER");
        personRepository.save(newPerson);

        return authService.login(person, request);
    }
}
