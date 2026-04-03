package com.infopharma.ipos_sa.service;

import com.infopharma.ipos_sa.dto.LowStockReportItem;
import com.infopharma.ipos_sa.dto.StockAddRequest;
import com.infopharma.ipos_sa.entity.CatalogueItem;
import com.infopharma.ipos_sa.entity.StockDelivery;
import com.infopharma.ipos_sa.mapper.Mapper;
import com.infopharma.ipos_sa.repository.CatalogueItemRepository;
import com.infopharma.ipos_sa.repository.StockDeliveryRepository;
import com.infopharma.ipos_sa.service.impl.CatalogueServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CatalogueServiceImpl.
 *
 * Covers adding, updating, deleting, and searching catalogue items,
 * recording incoming stock deliveries, and generating the low-stock report.
 *
 * All repository calls are mocked so no database is needed.
 */
@ExtendWith(MockitoExtension.class)
class CatalogueServiceImplTest {

    @Mock CatalogueItemRepository catalogueItemRepository;
    @Mock StockDeliveryRepository stockDeliveryRepository;
    @Mock ModelMapper modelMapper;
    @Mock Mapper<StockDelivery, StockAddRequest> stockDeliveryMapper;

    @InjectMocks CatalogueServiceImpl catalogueService;

    // A standard catalogue item used across multiple tests
    private CatalogueItem item;

    @BeforeEach
    void setUp() {
        item = new CatalogueItem();
        item.setItemId("100 00001");
        item.setDescription("Paracetamol 500mg");
        item.setPackageType("box");
        item.setUnit("Caps");
        item.setUnitsInPack(100);
        item.setPackageCost(new BigDecimal("8.99"));
        item.setAvailability(50);
        item.setMinStockLevel(20);
        item.setReorderBufferPct(new BigDecimal("10.00"));
    }

    // ----------------------------- addItem -----------------------------

    // Adding a new item should save it and return exactly what was saved
    @Test
    void addItem_savesAndReturnsItem() {
        when(catalogueItemRepository.save(item)).thenReturn(item);

        CatalogueItem result = catalogueService.addItem(item);

        assertThat(result).isSameAs(item);
        verify(catalogueItemRepository).save(item);
    }

    // ----------------------------- update -----------------------------

    // Trying to update an item that doesn't exist should throw an error
    @Test
    void update_itemNotFound_throwsEntityNotFoundException() {
        when(catalogueItemRepository.findById("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogueService.update("MISSING", item))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("MISSING");
    }

    // Updating an existing item should merge the new values in and preserve the original item ID
    @Test
    void update_success_mapsAndPreservesId() {
        CatalogueItem updated = new CatalogueItem();
        updated.setDescription("Updated Description");

        when(catalogueItemRepository.findById("100 00001")).thenReturn(Optional.of(item));
        when(catalogueItemRepository.save(item)).thenReturn(item);

        CatalogueItem result = catalogueService.update("100 00001", updated);

        // ModelMapper.map() merges the updated fields into the existing item
        verify(modelMapper).map(updated, item);
        // The item ID from the URL must never be overwritten by whatever is in the request body
        assertThat(item.getItemId()).isEqualTo("100 00001");
        assertThat(result).isSameAs(item);
    }

    // ----------------------------- delete -----------------------------

    // Trying to delete an item that doesn't exist should throw an error
    @Test
    void delete_itemNotFound_throwsEntityNotFoundException() {
        when(catalogueItemRepository.existsById("MISSING")).thenReturn(false);

        assertThatThrownBy(() -> catalogueService.delete("MISSING"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("MISSING");
    }

    // Deleting an item that exists should remove it from the repository
    @Test
    void delete_success_deletesById() {
        when(catalogueItemRepository.existsById("100 00001")).thenReturn(true);

        catalogueService.delete("100 00001");

        verify(catalogueItemRepository).deleteById("100 00001");
    }

    // ----------------------------- findById -----------------------------

    // Looking up an item that exists should return it
    @Test
    void findById_found_returnsItem() {
        when(catalogueItemRepository.findById("100 00001")).thenReturn(Optional.of(item));

        Optional<CatalogueItem> result = catalogueService.findById("100 00001");

        assertThat(result).isPresent();
        assertThat(result.get().getItemId()).isEqualTo("100 00001");
    }

    // Looking up an item ID that doesn't exist should return empty (not an error)
    @Test
    void findById_notFound_returnsEmpty() {
        when(catalogueItemRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThat(catalogueService.findById("NOPE")).isEmpty();
    }

    // ----------------------------- findAll -----------------------------

    // Fetching all items should return every item in the catalogue
    @Test
    void findAll_returnsList() {
        CatalogueItem item2 = new CatalogueItem();
        item2.setItemId("100 00002");
        when(catalogueItemRepository.findAll()).thenReturn(List.of(item, item2));

        List<CatalogueItem> result = catalogueService.findAll();

        assertThat(result).hasSize(2);
    }

    // ----------------------------- search -----------------------------

    // Searching by keyword should delegate to the repository's description search
    @Test
    void search_delegatesToRepository() {
        when(catalogueItemRepository.findByDescriptionContainingIgnoreCase("para"))
                .thenReturn(List.of(item));

        List<CatalogueItem> result = catalogueService.search("para");

        assertThat(result).containsExactly(item);
        verify(catalogueItemRepository).findByDescriptionContainingIgnoreCase("para");
    }

    // ----------------------------- addStock -----------------------------

    // Recording stock for an item that doesn't exist should throw an error
    @Test
    void addStock_itemNotFound_throwsEntityNotFoundException() {
        when(catalogueItemRepository.findById("MISSING")).thenReturn(Optional.empty());

        StockAddRequest request = new StockAddRequest();
        request.setQuantity(10);

        assertThatThrownBy(() -> catalogueService.addStock("MISSING", request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("MISSING");
    }

    // Recording a stock delivery should increase availability and save a delivery record with today's date
    @Test
    void addStock_success_incrementsAvailabilityAndSavesDelivery() {
        StockAddRequest request = new StockAddRequest();
        request.setQuantity(30);
        request.setRecordedBy("Bob");

        StockDelivery delivery = new StockDelivery();

        when(catalogueItemRepository.findById("100 00001")).thenReturn(Optional.of(item));
        when(catalogueItemRepository.save(item)).thenReturn(item);
        when(stockDeliveryMapper.mapFrom(request)).thenReturn(delivery);

        ArgumentCaptor<StockDelivery> deliveryCaptor = ArgumentCaptor.forClass(StockDelivery.class);
        when(stockDeliveryRepository.save(deliveryCaptor.capture())).thenReturn(delivery);

        catalogueService.addStock("100 00001", request);

        // Availability: 50 + 30 = 80
        assertThat(item.getAvailability()).isEqualTo(80);

        StockDelivery saved = deliveryCaptor.getValue();
        assertThat(saved.getItem()).isSameAs(item);
        assertThat(saved.getDeliveryDate()).isEqualTo(LocalDate.now());
    }

    // ----------------------------- getLowStockReport -----------------------------

    // An item below its minimum stock level should appear in the low-stock report
    // with a recommended order quantity that factors in the reorder buffer percentage
    @Test
    void getLowStockReport_itemBelowMinStock_includesInReport() {
        item.setAvailability(5);
        item.setMinStockLevel(20);
        item.setReorderBufferPct(new BigDecimal("10.00"));

        when(catalogueItemRepository.findAll()).thenReturn(List.of(item));

        List<LowStockReportItem> report = catalogueService.getLowStockReport();

        assertThat(report).hasSize(1);
        LowStockReportItem reportItem = report.get(0);
        assertThat(reportItem.getItemId()).isEqualTo("100 00001");
        assertThat(reportItem.getCurrentAvailability()).isEqualTo(5);
        assertThat(reportItem.getMinStockLevel()).isEqualTo(20);
        // Recommended = ceil((20 * 1.10) - 5) = ceil(22 - 5) = 17
        assertThat(reportItem.getRecommendedOrderQty()).isEqualTo(17);
    }

    // An item that still has enough stock should NOT appear in the low-stock report
    @Test
    void getLowStockReport_itemAboveMinStock_excludedFromReport() {
        item.setAvailability(50);
        item.setMinStockLevel(20);

        when(catalogueItemRepository.findAll()).thenReturn(List.of(item));

        List<LowStockReportItem> report = catalogueService.getLowStockReport();

        assertThat(report).isEmpty();
    }

    // When the catalogue has a mix of low-stock and adequate-stock items,
    // only the low-stock ones should appear in the report
    @Test
    void getLowStockReport_mixedItems_returnsOnlyLowStock() {
        CatalogueItem lowItem = new CatalogueItem();
        lowItem.setItemId("LOW001");
        lowItem.setDescription("Low stock item");
        lowItem.setAvailability(3);
        lowItem.setMinStockLevel(10);
        lowItem.setReorderBufferPct(new BigDecimal("20.00"));

        CatalogueItem okItem = new CatalogueItem();
        okItem.setItemId("OK001");
        okItem.setDescription("Adequate stock item");
        okItem.setAvailability(100);
        okItem.setMinStockLevel(10);
        okItem.setReorderBufferPct(new BigDecimal("10.00"));

        when(catalogueItemRepository.findAll()).thenReturn(List.of(lowItem, okItem));

        List<LowStockReportItem> report = catalogueService.getLowStockReport();

        assertThat(report).hasSize(1);
        assertThat(report.get(0).getItemId()).isEqualTo("LOW001");
    }
}
