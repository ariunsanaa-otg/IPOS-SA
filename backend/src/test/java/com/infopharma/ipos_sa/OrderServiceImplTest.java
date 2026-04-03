package com.infopharma.ipos_sa.service;

import com.infopharma.ipos_sa.dto.DispatchRequest;
import com.infopharma.ipos_sa.dto.OrderRequest;
import com.infopharma.ipos_sa.entity.*;
import com.infopharma.ipos_sa.mapper.impl.DispatchMapper;
import com.infopharma.ipos_sa.repository.*;
import com.infopharma.ipos_sa.service.impl.OrderServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderServiceImpl.
 *
 * All database calls are replaced with mocks (fake objects that return
 * pre-defined values), so these tests run without a real database.
 *
 * Each test follows the same pattern:
 *   1. Set up the data / mock responses needed for that scenario
 *   2. Call the method being tested
 *   3. Assert that the result or side-effects are what we expect
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    // --- Mocked dependencies (no real database needed) ---
    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock UserAccountRepository userAccountRepository;
    @Mock CatalogueItemRepository catalogueItemRepository;
    @Mock DispatchMapper dispatchMapper;

    // The class we are actually testing, with the mocks injected automatically
    @InjectMocks OrderServiceImpl orderService;

    // Reusable test data created fresh before every test
    private UserAccount normalAccount;
    private CatalogueItem catalogueItem;

    @BeforeEach
    void setUp() {
        normalAccount = new UserAccount();
        normalAccount.setAccountId(1L);
        normalAccount.setUsername("merchant1");
        normalAccount.setAccountStatus(UserAccount.AccountStatus.NORMAL);
        normalAccount.setBalance(BigDecimal.ZERO);

        catalogueItem = new CatalogueItem();
        catalogueItem.setItemId("ITEM001");
        catalogueItem.setDescription("Paracetamol 500mg");
        catalogueItem.setPackageCost(new BigDecimal("12.50"));
        catalogueItem.setAvailability(100);
    }

    // ----------------------------- placeOrder -----------------------------

    // Placing an order for an account ID that doesn't exist should throw an error
    @Test
    void placeOrder_accountNotFound_throwsEntityNotFoundException() {
        OrderRequest request = buildOrderRequest(99L, "ITEM001", 2);
        when(userAccountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    // A suspended merchant should not be able to place new orders
    @Test
    void placeOrder_suspendedAccount_throwsIllegalStateException() {
        normalAccount.setAccountStatus(UserAccount.AccountStatus.SUSPENDED);
        OrderRequest request = buildOrderRequest(1L, "ITEM001", 2);
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(normalAccount));

        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SUSPENDED");
    }

    // An account in default (seriously overdue) should also be blocked from ordering
    @Test
    void placeOrder_inDefaultAccount_throwsIllegalStateException() {
        normalAccount.setAccountStatus(UserAccount.AccountStatus.IN_DEFAULT);
        OrderRequest request = buildOrderRequest(1L, "ITEM001", 2);
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(normalAccount));

        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IN_DEFAULT");
    }

    // Ordering a catalogue item that doesn't exist should throw an error
    @Test
    void placeOrder_itemNotFound_throwsEntityNotFoundException() {
        OrderRequest request = buildOrderRequest(1L, "MISSING_ITEM", 2);
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(normalAccount));
        when(orderRepository.existsById(anyString())).thenReturn(false);
        when(catalogueItemRepository.findById("MISSING_ITEM")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("MISSING_ITEM");
    }

    // A valid order should: set status to ACCEPTED, calculate the correct total,
    // generate an order ID starting with "IP", decrement stock, and update the merchant balance
    @Test
    void placeOrder_success_createsOrderAndInvoiceAndUpdatesBalance() {
        OrderRequest request = buildOrderRequest(1L, "ITEM001", 2);

        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(normalAccount));
        when(orderRepository.existsById(anyString())).thenReturn(false);
        when(invoiceRepository.existsById(anyString())).thenReturn(false);
        when(catalogueItemRepository.findById("ITEM001")).thenReturn(Optional.of(catalogueItem));
        when(catalogueItemRepository.save(any(CatalogueItem.class))).thenReturn(catalogueItem);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.placeOrder(request);

        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.ACCEPTED);
        assertThat(result.getPaymentStatus()).isEqualTo(Order.PaymentStatus.PENDING);

        // Total = 12.50 * 2 = 25.00
        assertThat(result.getTotalValue()).isEqualByComparingTo(new BigDecimal("25.00"));

        // Order ID generated (starts with "IP")
        assertThat(result.getOrderId()).startsWith("IP");

        // Stock decremented: 100 - 2 = 98
        assertThat(catalogueItem.getAvailability()).isEqualTo(98);

        // Merchant balance increases by the order total: 0 + 25.00 = 25.00
        assertThat(normalAccount.getBalance()).isEqualByComparingTo(new BigDecimal("25.00"));

        verify(invoiceRepository, times(1)).save(any(Invoice.class));
        verify(orderItemRepository, times(1)).saveAll(anyList());
    }

    // The auto-generated invoice should have the correct total and today's date
    @Test
    void placeOrder_success_invoiceHasCorrectAmountDue() {
        OrderRequest request = buildOrderRequest(1L, "ITEM001", 3);

        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(normalAccount));
        when(orderRepository.existsById(anyString())).thenReturn(false);
        when(invoiceRepository.existsById(anyString())).thenReturn(false);
        when(catalogueItemRepository.findById("ITEM001")).thenReturn(Optional.of(catalogueItem));
        when(catalogueItemRepository.save(any())).thenReturn(catalogueItem);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        when(invoiceRepository.save(invoiceCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        orderService.placeOrder(request);

        Invoice savedInvoice = invoiceCaptor.getValue();
        // Total = 12.50 * 3 = 37.50
        assertThat(savedInvoice.getAmountDue()).isEqualByComparingTo(new BigDecimal("37.50"));
        assertThat(savedInvoice.getInvoiceDate()).isEqualTo(LocalDate.now());
    }

    // ----------------------------- findById -----------------------------

    // Looking up an order that exists should return it
    @Test
    void findById_existingOrder_returnsOrder() {
        Order order = new Order();
        order.setOrderId("IP1234");
        when(orderRepository.findById("IP1234")).thenReturn(Optional.of(order));

        Optional<Order> result = orderService.findById("IP1234");

        assertThat(result).isPresent();
        assertThat(result.get().getOrderId()).isEqualTo("IP1234");
    }

    // Looking up an order ID that doesn't exist should return an empty result (not an error)
    @Test
    void findById_missingOrder_returnsEmpty() {
        when(orderRepository.findById("MISSING")).thenReturn(Optional.empty());

        assertThat(orderService.findById("MISSING")).isEmpty();
    }

    // ----------------------------- findAll -----------------------------

    // Fetching all orders should return every order in the system
    @Test
    void findAll_returnsList() {
        Order o1 = new Order(); o1.setOrderId("IP0001");
        Order o2 = new Order(); o2.setOrderId("IP0002");
        when(orderRepository.findAll()).thenReturn(List.of(o1, o2));

        List<Order> result = orderService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Order::getOrderId).containsExactly("IP0001", "IP0002");
    }

    // ----------------------------- findByAccountId -----------------------------

    // Fetching orders for an account ID that doesn't exist should throw an error
    @Test
    void findByAccountId_accountNotFound_throwsEntityNotFoundException() {
        when(userAccountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findByAccountId(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    // Fetching orders for a valid account should return only that account's orders
    @Test
    void findByAccountId_success_returnsOrdersForAccount() {
        Order o = new Order(); o.setOrderId("IP1111");
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(normalAccount));
        when(orderRepository.findByAccount(normalAccount)).thenReturn(List.of(o));

        List<Order> result = orderService.findByAccountId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderId()).isEqualTo("IP1111");
    }

    // ----------------------------- findIncomplete -----------------------------

    // Fetching incomplete orders should exclude any orders that have been delivered
    @Test
    void findIncomplete_delegatesToRepository() {
        Order o = new Order(); o.setStatus(Order.OrderStatus.ACCEPTED);
        when(orderRepository.findByStatusNot(Order.OrderStatus.DELIVERED)).thenReturn(List.of(o));

        List<Order> result = orderService.findIncomplete();

        assertThat(result).hasSize(1);
        verify(orderRepository).findByStatusNot(Order.OrderStatus.DELIVERED);
    }

    // ----------------------------- dispatch -----------------------------

    // Trying to dispatch an order that doesn't exist should throw an error
    @Test
    void dispatch_orderNotFound_throwsEntityNotFoundException() {
        when(orderRepository.findById("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.dispatch("UNKNOWN", new DispatchRequest()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    // Dispatching a valid order should set its status to DISPATCHED and record today's date
    @Test
    void dispatch_success_setsStatusAndDispatchDate() {
        Order order = new Order();
        order.setOrderId("IP2000");
        order.setStatus(Order.OrderStatus.ACCEPTED);

        DispatchRequest dispatchRequest = new DispatchRequest();
        dispatchRequest.setDispatchedBy("Alice");
        dispatchRequest.setCourier("DHL");
        dispatchRequest.setCourierRef("DHL-999");
        dispatchRequest.setExpectedDelivery(LocalDate.now().plusDays(3));

        when(orderRepository.findById("IP2000")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.dispatch("IP2000", dispatchRequest);

        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.DISPATCHED);
        assertThat(result.getDispatchDate()).isEqualTo(LocalDate.now());
        verify(dispatchMapper).applyTo(dispatchRequest, order);
    }

    // ----------------------------- markDelivered -----------------------------

    // Trying to mark a non-existent order as delivered should throw an error
    @Test
    void markDelivered_orderNotFound_throwsEntityNotFoundException() {
        when(orderRepository.findById("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.markDelivered("UNKNOWN"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    // Marking a valid order as delivered should set its status to DELIVERED and record today's date
    @Test
    void markDelivered_success_setsStatusAndDeliveryDate() {
        Order order = new Order();
        order.setOrderId("IP3000");
        order.setStatus(Order.OrderStatus.DISPATCHED);

        when(orderRepository.findById("IP3000")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.markDelivered("IP3000");

        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.DELIVERED);
        assertThat(result.getDeliveryDate()).isEqualTo(LocalDate.now());
    }

    // ----------------------------- helpers -----------------------------

    private OrderRequest buildOrderRequest(Long accountId, String itemId, int qty) {
        OrderRequest.OrderItemRequest itemReq = new OrderRequest.OrderItemRequest();
        itemReq.setItemId(itemId);
        itemReq.setQuantity(qty);

        OrderRequest request = new OrderRequest();
        request.setAccountId(accountId);
        request.setItems(List.of(itemReq));
        return request;
    }
}
