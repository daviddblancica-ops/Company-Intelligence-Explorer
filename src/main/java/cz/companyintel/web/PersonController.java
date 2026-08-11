package cz.companyintel.web;

import cz.companyintel.domain.CompanyPersonRole;
import cz.companyintel.domain.Person;
import cz.companyintel.service.PersonService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/people")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @GetMapping
    public List<PersonResponse> search(
            @RequestParam(defaultValue = "") String q,
            Authentication authentication) {
        boolean includeSensitiveDetails = canReadSensitiveDetails(authentication);
        return personService.searchPeople(q).stream()
                .map(person -> PersonResponse.from(
                        person,
                        personService.findRelationships(person.getId()),
                        includeSensitiveDetails))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public PersonResponse get(@PathVariable Long id, Authentication authentication) {
        Person person = personService.getPerson(id);
        List<CompanyPersonRole> relationships = personService.findRelationships(person.getId());
        return PersonResponse.from(person, relationships, canReadSensitiveDetails(authentication));
    }

    @PutMapping("/{id}")
    public PersonResponse update(
            @PathVariable Long id,
            @RequestBody PersonUpdateRequest request,
            Authentication authentication) {
        Person person = personService.updatePerson(id, request);
        return PersonResponse.from(
                person,
                personService.findRelationships(id),
                canReadSensitiveDetails(authentication));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        personService.deletePerson(id);
    }

    private boolean canReadSensitiveDetails(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())
                    || "ROLE_EDITOR".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
