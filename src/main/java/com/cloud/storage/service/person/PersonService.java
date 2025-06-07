package com.cloud.storage.service.person;

import com.cloud.storage.dto.person.PersonResponse;
import com.cloud.storage.model.Person;
import com.cloud.storage.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PersonService {
    private final PersonRepository personRepository;

    public PersonResponse getPersonBySession() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationException("User is not logged in") {
            };
        }

        return new PersonResponse(auth.getName());
    }

    public Integer getPersonById() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationException("User not authenticated") {
            };
        }

        Person person = personRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Person not found"));
        return person.getId();
    }
}
