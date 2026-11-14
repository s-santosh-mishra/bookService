package com.servicehub.serv.service;

import com.servicehub.serv.dto.CustomerCategoryDto;
import com.servicehub.serv.dto.CustomerServiceDto;
import com.servicehub.serv.entity.Category;
import com.servicehub.serv.entity.Service;
import com.servicehub.serv.repository.CategoryRepository;
import com.servicehub.serv.repository.ServiceRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CustomerServiceServiceTest {

    @Test
    void getActiveServicesIncludesBasePricePerHourAndMinimumServiceFee() {
        UUID categoryId = UUID.randomUUID();
        Category category = new Category();
        category.setCategoryId(categoryId);
        category.setCategoryName("Electrical");
        category.setActive(true);

        UUID serviceId = UUID.randomUUID();
        Service service = new Service();
        service.setServiceId(serviceId);
        service.setServiceName("Ceiling Fan Repair");
        service.setCategory(category);
        service.setActive(true);
        service.setBasePricePerHour(new BigDecimal("150.00"));
        service.setMinimumServiceFee(new BigDecimal("100.00"));

        ServiceRepository serviceRepo = createProxy(ServiceRepository.class, (method, args) -> {
            if ("findAll".equals(method.getName())) {
                return List.of(service);
            }
            return null;
        });

        CategoryRepository categoryRepo = createProxy(CategoryRepository.class, (method, args) -> null);

        CustomerServiceService serviceService = new CustomerServiceService(categoryRepo, serviceRepo);

        List<CustomerServiceDto> activeServices = serviceService.getActiveServices();

        assertNotNull(activeServices);
        assertEquals(1, activeServices.size());

        CustomerServiceDto dto = activeServices.get(0);
        assertEquals(serviceId, dto.getServiceId());
        assertEquals("Ceiling Fan Repair", dto.getServiceName());
        assertEquals(categoryId, dto.getCategoryId());
        assertEquals("Electrical", dto.getCategoryName());
        assertEquals(new BigDecimal("150.00"), dto.getBasePricePerHour());
        assertEquals(new BigDecimal("100.00"), dto.getMinimumServiceFee());
    }

    @Test
    void getActiveServicesFiltersOutInactiveServicesAndInactiveCategories() {
        Category activeCategory = new Category();
        activeCategory.setCategoryId(UUID.randomUUID());
        activeCategory.setCategoryName("Plumbing");
        activeCategory.setActive(true);

        Category inactiveCategory = new Category();
        inactiveCategory.setCategoryId(UUID.randomUUID());
        inactiveCategory.setCategoryName("Painting");
        inactiveCategory.setActive(false);

        // 1. Active service in active category -> SHOULD be returned
        Service validService = new Service();
        validService.setServiceId(UUID.randomUUID());
        validService.setServiceName("Tap Repair");
        validService.setCategory(activeCategory);
        validService.setActive(true);
        validService.setBasePricePerHour(new BigDecimal("120.00"));
        validService.setMinimumServiceFee(new BigDecimal("80.00"));

        // 2. Inactive service in active category -> SHOULD be filtered out
        Service inactiveService = new Service();
        inactiveService.setServiceId(UUID.randomUUID());
        inactiveService.setServiceName("Pipe Leakage");
        inactiveService.setCategory(activeCategory);
        inactiveService.setActive(false);
        inactiveService.setBasePricePerHour(new BigDecimal("200.00"));
        inactiveService.setMinimumServiceFee(new BigDecimal("150.00"));

        // 3. Active service in inactive category -> SHOULD be filtered out
        Service inactiveCatService = new Service();
        inactiveCatService.setServiceId(UUID.randomUUID());
        inactiveCatService.setServiceName("Wall Painting");
        inactiveCatService.setCategory(inactiveCategory);
        inactiveCatService.setActive(true);
        inactiveCatService.setBasePricePerHour(new BigDecimal("300.00"));
        inactiveCatService.setMinimumServiceFee(new BigDecimal("250.00"));

        // 4. Service with null category -> SHOULD be filtered out
        Service orphanService = new Service();
        orphanService.setServiceId(UUID.randomUUID());
        orphanService.setServiceName("Orphan Service");
        orphanService.setCategory(null);
        orphanService.setActive(true);

        ServiceRepository serviceRepo = createProxy(ServiceRepository.class, (method, args) -> {
            if ("findAll".equals(method.getName())) {
                return List.of(validService, inactiveService, inactiveCatService, orphanService);
            }
            return null;
        });

        CategoryRepository categoryRepo = createProxy(CategoryRepository.class, (method, args) -> null);

        CustomerServiceService serviceService = new CustomerServiceService(categoryRepo, serviceRepo);

        List<CustomerServiceDto> activeServices = serviceService.getActiveServices();

        assertEquals(1, activeServices.size());
        assertEquals("Tap Repair", activeServices.get(0).getServiceName());
        assertEquals(new BigDecimal("120.00"), activeServices.get(0).getBasePricePerHour());
        assertEquals(new BigDecimal("80.00"), activeServices.get(0).getMinimumServiceFee());
    }

    @Test
    void customerServiceDtoConstructorsAndGettersSettersWorkCorrectly() {
        UUID serviceId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        // 4-arg constructor
        CustomerServiceDto dto4 = new CustomerServiceDto(serviceId, "Service 1", categoryId, "Category 1");
        assertEquals(serviceId, dto4.getServiceId());
        assertEquals("Service 1", dto4.getServiceName());
        assertEquals(categoryId, dto4.getCategoryId());
        assertEquals("Category 1", dto4.getCategoryName());
        assertNull(dto4.getBasePricePerHour());
        assertNull(dto4.getMinimumServiceFee());

        // 6-arg constructor
        CustomerServiceDto dto6 = new CustomerServiceDto(
                serviceId, "Service 2", categoryId, "Category 2",
                new BigDecimal("180.50"), new BigDecimal("120.00")
        );
        assertEquals(new BigDecimal("180.50"), dto6.getBasePricePerHour());
        assertEquals(new BigDecimal("120.00"), dto6.getMinimumServiceFee());

        // No-arg constructor & setters
        CustomerServiceDto dtoNoArg = new CustomerServiceDto();
        dtoNoArg.setServiceId(serviceId);
        dtoNoArg.setServiceName("Service 3");
        dtoNoArg.setCategoryId(categoryId);
        dtoNoArg.setCategoryName("Category 3");
        dtoNoArg.setBasePricePerHour(new BigDecimal("220.00"));
        dtoNoArg.setMinimumServiceFee(new BigDecimal("150.00"));

        assertEquals(serviceId, dtoNoArg.getServiceId());
        assertEquals("Service 3", dtoNoArg.getServiceName());
        assertEquals(categoryId, dtoNoArg.getCategoryId());
        assertEquals("Category 3", dtoNoArg.getCategoryName());
        assertEquals(new BigDecimal("220.00"), dtoNoArg.getBasePricePerHour());
        assertEquals(new BigDecimal("150.00"), dtoNoArg.getMinimumServiceFee());
    }

    @Test
    void getActiveCategoriesReturnsOnlyActiveCategories() {
        Category active = new Category();
        active.setCategoryId(UUID.randomUUID());
        active.setCategoryName("Cleaning");
        active.setActive(true);

        Category inactive = new Category();
        inactive.setCategoryId(UUID.randomUUID());
        inactive.setCategoryName("Gardening");
        inactive.setActive(false);

        CategoryRepository categoryRepo = createProxy(CategoryRepository.class, (method, args) -> {
            if ("findAll".equals(method.getName())) {
                return List.of(active, inactive);
            }
            return null;
        });

        ServiceRepository serviceRepo = createProxy(ServiceRepository.class, (method, args) -> null);

        CustomerServiceService serviceService = new CustomerServiceService(categoryRepo, serviceRepo);

        List<CustomerCategoryDto> categories = serviceService.getActiveCategories();

        assertEquals(1, categories.size());
        assertEquals("Cleaning", categories.get(0).getCategoryName());
    }

    @SuppressWarnings("unchecked")
    private static <T> T createProxy(Class<T> intf, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
                intf.getClassLoader(),
                new Class<?>[]{intf},
                (proxy, method, args) -> {
                    Object res = handler.handle(method, args);
                    if (res != null) return res;
                    Class<?> returnType = method.getReturnType();
                    if (returnType.equals(Optional.class)) return Optional.empty();
                    if (returnType.equals(List.class)) return List.of();
                    if (returnType.equals(boolean.class)) return false;
                    return null;
                }
        );
    }

    @FunctionalInterface
    private interface InvocationHandler {
        Object handle(java.lang.reflect.Method method, Object[] args) throws Throwable;
    }
}
