package cz.companyintel.repository;

import cz.companyintel.domain.Company;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByRegistrationNumber(String registrationNumber);

    @Query("select c from Company c where lower(c.normalizedName) like lower(concat('%', :query, '%')) or c.registrationNumber like concat('%', :query, '%')")
    List<Company> search(@Param("query") String query);
}
