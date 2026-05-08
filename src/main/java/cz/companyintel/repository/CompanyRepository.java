package cz.companyintel.repository;

import cz.companyintel.domain.Company;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByRegistrationNumber(String registrationNumber);

    @Query("select distinct c from Company c "
            + "left join c.people relationship "
            + "left join relationship.person person "
            + "where lower(c.normalizedName) like lower(concat(:query, '%')) "
            + "or c.registrationNumber like concat(:query, '%') "
            + "or lower(person.normalizedName) like lower(concat('%', :query, '%')) "
            + "or lower(relationship.role) like lower(concat('%', :query, '%')) "
            + "order by c.updatedAt desc")
    List<Company> search(@Param("query") String query, Pageable pageable);
}
