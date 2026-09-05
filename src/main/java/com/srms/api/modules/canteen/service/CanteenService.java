package com.srms.api.modules.canteen.service;

import com.srms.api.modules.canteen.entity.CanteenOrder;
import com.srms.api.modules.canteen.entity.DailyMenu;
import com.srms.api.modules.canteen.entity.MenuItem;
import com.srms.api.modules.canteen.repository.CanteenOrderRepository;
import com.srms.api.modules.canteen.repository.DailyMenuRepository;
import com.srms.api.modules.canteen.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class CanteenService {
    private final MenuItemRepository menuRepo;
    private final CanteenOrderRepository orderRepo;
    private final DailyMenuRepository dailyMenuRepo;

    public List<MenuItem> getMenu(String schoolId) { return menuRepo.findBySchoolId(schoolId); }
    public MenuItem createMenuItem(String schoolId, MenuItem item) { item.setSchoolId(schoolId); return menuRepo.save(item); }
    public MenuItem updateMenuItem(String schoolId, String id, MenuItem updated) {
        MenuItem item = menuRepo.findById(id).filter(m -> m.getSchoolId().equals(schoolId)).orElseThrow();
        item.setName(updated.getName());
        item.setCategory(updated.getCategory());
        item.setPrice(updated.getPrice());
        item.setCostPrice(updated.getCostPrice());
        item.setAvailable(updated.isAvailable());
        item.setOpeningStock(updated.getOpeningStock());
        item.setReorderLevel(updated.getReorderLevel());
        item.setServingSize(updated.getServingSize());
        item.setUnitOfMeasure(updated.getUnitOfMeasure());
        item.setIssuePoint(updated.getIssuePoint());
        item.setAllergens(updated.getAllergens());
        item.setVegetarian(updated.getVegetarian());
        item.setHalal(updated.getHalal());
        item.setSupplier(updated.getSupplier());
        item.setPreparationTime(updated.getPreparationTime());
        item.setDescription(updated.getDescription());
        return menuRepo.save(item);
    }
    public void deleteMenuItem(String schoolId, String id) { menuRepo.findById(id).filter(m -> m.getSchoolId().equals(schoolId)).ifPresent(menuRepo::delete); }

    public List<CanteenOrder> getOrders(String schoolId) { return orderRepo.findBySchoolIdOrderByOrderDateDesc(schoolId); }
    public CanteenOrder createOrder(String schoolId, CanteenOrder order) {
        order.setSchoolId(schoolId);
        if (order.getOrderDate() == null) order.setOrderDate(LocalDate.now());
        if (order.getStatus() == null) order.setStatus("PENDING");
        if (order.getQuantity() == null || order.getQuantity() < 1) order.setQuantity(1);
        return orderRepo.save(order);
    }

    public List<DailyMenu> getDailyMenu(String schoolId, LocalDate date) {
        return date != null
            ? dailyMenuRepo.findBySchoolIdAndMenuDateOrderByMealPeriodAsc(schoolId, date)
            : dailyMenuRepo.findBySchoolIdOrderByMenuDateDescMealPeriodAsc(schoolId);
    }

    public DailyMenu saveDailyMenu(String schoolId, DailyMenu incoming) {
        incoming.setSchoolId(schoolId);
        if (incoming.getMenuDate() == null) incoming.setMenuDate(LocalDate.now());
        return dailyMenuRepo.findBySchoolIdAndMenuDateAndMealPeriod(schoolId, incoming.getMenuDate(), incoming.getMealPeriod())
            .map(existing -> {
                existing.setItems(incoming.getItems());
                existing.setNotes(incoming.getNotes());
                existing.setEnteredBy(incoming.getEnteredBy());
                return dailyMenuRepo.save(existing);
            })
            .orElseGet(() -> dailyMenuRepo.save(incoming));
    }

    public void deleteDailyMenu(String schoolId, String id) {
        dailyMenuRepo.findById(id).filter(m -> m.getSchoolId().equals(schoolId)).ifPresent(dailyMenuRepo::delete);
    }
}
