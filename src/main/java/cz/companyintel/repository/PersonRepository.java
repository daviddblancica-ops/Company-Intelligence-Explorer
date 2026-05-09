package cz.companyintel.repository;

import cz.companyintel.domain.Person;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

public interface PersonRepository extends JpaRepository<Person, Long> {

    Optional<Person> findByNormalizedName(String normalizedName);

    List<Person> findByNormalizedNameContaining(String normalizedName, Sort sort);
}
