package cz.companyintel.repository;

import cz.companyintel.domain.CompanyPersonRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyPersonRoleRepository extends JpaRepository<CompanyPersonRole, Long> {

    List<CompanyPersonRole> findByPersonIdOrderByCompanyNameAsc(Long personId);
}
