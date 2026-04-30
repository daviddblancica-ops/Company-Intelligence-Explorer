package cz.companyintel.repository;

import cz.companyintel.domain.Person;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {

    Optional<Person> findByNormalizedName(String normalizedName);
}
