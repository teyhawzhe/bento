package com.lovius.bento.config;

import com.lovius.bento.dao.EmployeeRepository;
import com.lovius.bento.dao.MenuRepository;
import com.lovius.bento.dao.OrderRepository;
import com.lovius.bento.dao.SupplierRepository;
import com.lovius.bento.model.BentoOrder;
import com.lovius.bento.model.Employee;
import com.lovius.bento.model.Menu;
import com.lovius.bento.model.Supplier;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@ConditionalOnProperty(name = "app.sample-data.enabled", havingValue = "true")
public class SampleDataInitializer {
    private static final Logger logger = LoggerFactory.getLogger(SampleDataInitializer.class);

    private static final List<SupplierSeed> SAMPLE_SUPPLIERS = List.of(
            new SupplierSeed("旬味便當", "orders@seasonal.local", "02-1111-2233", "張小雅", "55667788"),
            new SupplierSeed("元氣食堂", "orders@genki.local", "02-2222-3344", "李志明", "66778899"),
            new SupplierSeed("蔬享廚房", "orders@vege.local", "02-3333-4455", "陳怡君", "77889900"));

    private static final List<MenuTemplate> SAMPLE_MENU_TEMPLATES = List.of(
            new MenuTemplate("照燒雞腿便當", "葷食", "雞腿主餐，附季節配菜", "125.00"),
            new MenuTemplate("滷香排骨便當", "葷食", "招牌排骨，附滷蛋與時蔬", "130.00"),
            new MenuTemplate("椒鹽里肌便當", "葷食", "里肌主餐，附三樣配菜", "128.00"),
            new MenuTemplate("蒲燒鯛魚便當", "葷食", "魚排主餐，附炒青菜", "135.00"),
            new MenuTemplate("清炒時蔬便當", "素食", "當日新鮮蔬食組合", "118.00"),
            new MenuTemplate("咖哩雞丁便當", "葷食", "咖哩雞丁，附滑蛋與青菜", "123.00"));

    @Bean
    @Order(1)
    public ApplicationRunner seedSampleData(
            EmployeeRepository employeeRepository,
            SupplierRepository supplierRepository,
            MenuRepository menuRepository,
            OrderRepository orderRepository) {
        return arguments -> {
            Employee admin = employeeRepository.findByUsername("admin").orElseThrow();
            Employee alice = employeeRepository.findByUsername("alice").orElseThrow();

            logger.info("Starting sample data seeding for alice");
            List<Supplier> suppliers = SAMPLE_SUPPLIERS.stream()
                    .map(seed -> getOrCreateSupplier(supplierRepository, seed))
                    .toList();

            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.of(startDate.getYear(), Month.AUGUST, 31);
            if (startDate.isAfter(endDate)) {
                logger.info("Skipped sample data seeding because {} is after {}", startDate, endDate);
                return;
            }

            int businessDayIndex = 0;
            int menuCreatedCount = 0;
            int orderCreatedCount = 0;
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                if (!isBusinessDay(date)) {
                    continue;
                }

                DailySeedResult dailySeedResult = createDailyMenus(menuRepository, suppliers, admin, date, businessDayIndex);
                menuCreatedCount += dailySeedResult.createdMenuCount();
                List<Menu> dailyMenus = dailySeedResult.menus();
                Menu selectedMenu = dailyMenus.get(businessDayIndex % dailyMenus.size());
                if (createOrderIfMissing(orderRepository, alice, selectedMenu, date)) {
                    orderCreatedCount++;
                }
                businessDayIndex++;
            }
            logger.info(
                    "Sample data seeding finished: suppliers={}, businessDays={}, menusCreated={}, ordersCreated={}, range={}..{}",
                    suppliers.size(),
                    businessDayIndex,
                    menuCreatedCount,
                    orderCreatedCount,
                    startDate,
                    endDate);
        };
    }

    private Supplier getOrCreateSupplier(SupplierRepository supplierRepository, SupplierSeed seed) {
        return supplierRepository.findByBusinessRegistrationNo(seed.businessRegistrationNo())
                .orElseGet(() -> supplierRepository.save(new Supplier(
                        null,
                        seed.name(),
                        seed.email(),
                        seed.phone(),
                        seed.contactName(),
                        seed.businessRegistrationNo(),
                        true,
                        Instant.now())));
    }

    private DailySeedResult createDailyMenus(
            MenuRepository menuRepository,
            List<Supplier> suppliers,
            Employee admin,
            LocalDate date,
            int businessDayIndex) {
        Instant now = Instant.now();
        MenuSeedResult first = createMenuIfMissing(menuRepository, suppliers.get(0), admin, date, businessDayIndex, 0, now);
        MenuSeedResult second = createMenuIfMissing(menuRepository, suppliers.get(1), admin, date, businessDayIndex, 1, now);
        MenuSeedResult third = createMenuIfMissing(menuRepository, suppliers.get(2), admin, date, businessDayIndex, 2, now);
        return new DailySeedResult(
                List.of(first.menu(), second.menu(), third.menu()),
                (first.created() ? 1 : 0) + (second.created() ? 1 : 0) + (third.created() ? 1 : 0));
    }

    private MenuSeedResult createMenuIfMissing(
            MenuRepository menuRepository,
            Supplier supplier,
            Employee admin,
            LocalDate date,
            int businessDayIndex,
            int offset,
            Instant now) {
        MenuTemplate template = SAMPLE_MENU_TEMPLATES.get((businessDayIndex + offset) % SAMPLE_MENU_TEMPLATES.size());
        String menuName = date + " " + template.name();
        return menuRepository.findBySupplierIdAndName(supplier.getId(), menuName)
                .map(menu -> new MenuSeedResult(menu, false))
                .orElseGet(() -> new MenuSeedResult(menuRepository.save(new Menu(
                        null,
                        supplier.getId(),
                        menuName,
                        template.category(),
                        template.description(),
                        new BigDecimal(template.price()),
                        date,
                        date,
                        admin.getId(),
                        now,
                        now)), true));
    }

    private boolean createOrderIfMissing(
            OrderRepository orderRepository,
            Employee employee,
            Menu menu,
            LocalDate orderDate) {
        if (orderRepository.findByEmployeeIdAndOrderDate(employee.getId(), orderDate).isPresent()) {
            return false;
        }
        orderRepository.save(new BentoOrder(
                null,
                employee.getId(),
                menu.getId(),
                orderDate,
                employee.getId(),
                Instant.now()));
        return true;
    }

    private boolean isBusinessDay(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    private record SupplierSeed(
            String name,
            String email,
            String phone,
            String contactName,
            String businessRegistrationNo) {
    }

    private record MenuTemplate(
            String name,
            String category,
            String description,
            String price) {
    }

    private record MenuSeedResult(Menu menu, boolean created) {
    }

    private record DailySeedResult(List<Menu> menus, int createdMenuCount) {
    }
}
