package com.cloud.storage.services.security;

import com.cloud.storage.config.security.PersonDetails;
import com.cloud.storage.models.Person;
import com.cloud.storage.repositories.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PersonDetailsService implements UserDetailsService {

    private final PersonRepository personRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Person person = personRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Person not found"));
        return PersonDetails.fromPerson(person);
    }
}
