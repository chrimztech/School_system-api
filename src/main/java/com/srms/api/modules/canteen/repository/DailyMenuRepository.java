package com.srms.api.modules.canteen.repository;

import com.srms.api.modules.canteen.entity.DailyMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyMenuRepository extends JpaRepository<DailyMenu, String> {
    List<DailyMenu> findBySchoolIdAndMenuDateOrderByMealPeriodAsc(String schoolId, LocalDate menuDate);
    List<DailyMenu> findBySchoolIdOrderByMenuDateDescMealPeriodAsc(String schoolId);
    Optional<DailyMenu> findBySchoolIdAndMenuDateAndMealPeriod(String schoolId, LocalDate menuDate, String mealPeriod);
}
