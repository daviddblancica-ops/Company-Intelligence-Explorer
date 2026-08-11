package cz.companyintel.repository;

import cz.companyintel.domain.ChangeEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChangeEventRepository extends JpaRepository<ChangeEvent, Long> {

    @Query("select distinct event.type from ChangeEvent event order by event.type")
    List<String> findDistinctTypes();

    @Query("select event from ChangeEvent event "
            + "left join event.company company "
            + "left join event.importRun importRun "
            + "where event.archived = :archived "
            + "and (:type is null or event.type = :type) "
            + "and (:severity is null or event.severity = :severity) "
            + "and (:companyId is null or company.id = :companyId) "
            + "and (:importRunId is null or importRun.id = :importRunId) "
            + "and (:query is null "
            + "or lower(company.name) like lower(concat('%', :query, '%')) "
            + "or company.registrationNumber like concat('%', :query, '%') "
            + "or lower(event.description) like lower(concat('%', :query, '%'))) "
            + "and (:from is null or event.createdAt >= :from) "
            + "and (:toExclusive is null or event.createdAt < :toExclusive) "
            + "order by event.createdAt desc")
    List<ChangeEvent> search(
            @Param("type") String type,
            @Param("severity") String severity,
            @Param("archived") boolean archived,
            @Param("companyId") Long companyId,
            @Param("importRunId") Long importRunId,
            @Param("query") String query,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive,
            Pageable pageable);
}
