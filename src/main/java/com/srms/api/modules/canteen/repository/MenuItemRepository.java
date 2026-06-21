package com.srms.api.modules.canteen.repository;

import com.srms.api.modules.canteen.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, String> {
    List<MenuItem> findBySchoolId(String schoolId);
    List<MenuItem> findBySchoolIdAndAvailableTrue(String schoolId);
}
