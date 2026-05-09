package cz.companyintel.web;

import cz.companyintel.domain.CompanyPersonRole;
import cz.companyintel.domain.Person;
import cz.companyintel.service.PersonService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/people")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @GetMapping
    public List<PersonResponse> search(@RequestParam(defaultValue = "") String q) {
        return personService.searchPeople(q).stream()
                .map(person -> PersonResponse.from(person, personService.findRelationships(person.getId())))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public PersonResponse get(@PathVariable Long id) {
        Person person = personService.getPerson(id);
        List<CompanyPersonRole> relationships = personService.findRelationships(person.getId());
        return PersonResponse.from(person, relationships);
    }
}
