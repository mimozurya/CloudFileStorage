package com.cloud.storage.services.person;

import com.cloud.storage.dto.person.PersonResponse;
import com.cloud.storage.models.Person;
import com.cloud.storage.repositories.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PersonService {
    private final PersonRepository personRepository;

    public ResponseEntity<PersonResponse> getPersonBySession() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new PersonResponse("User not logged in"));
        }

        return ResponseEntity.ok(new PersonResponse(auth.getName()));
    }

    public Integer getPersonById() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            throw new RuntimeException();
        }

        Person person = personRepository.findByUsername(auth.getName()).orElseThrow(() -> new UsernameNotFoundException("Person not found"));
        return person.getId();
    }
}
