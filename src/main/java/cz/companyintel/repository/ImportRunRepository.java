package cz.companyintel.repository;

import cz.companyintel.domain.ImportRun;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportRunRepository extends JpaRepository<ImportRun, Long> {

    List<ImportRun> findAllByOrderByStartedAtDesc(Pageable pageable);
}
