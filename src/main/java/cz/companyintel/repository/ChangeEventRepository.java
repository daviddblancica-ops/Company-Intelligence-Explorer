package cz.companyintel.repository;

import cz.companyintel.domain.ChangeEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangeEventRepository extends JpaRepository<ChangeEvent, Long> {

    List<ChangeEvent> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
}
