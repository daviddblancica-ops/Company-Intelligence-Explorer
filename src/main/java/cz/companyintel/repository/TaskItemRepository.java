package cz.companyintel.repository;

import cz.companyintel.domain.TaskItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskItemRepository extends JpaRepository<TaskItem, Long> {

    List<TaskItem> findByArchivedOrderByDoneAscPriorityAscUpdatedAtDesc(boolean archived);
}
