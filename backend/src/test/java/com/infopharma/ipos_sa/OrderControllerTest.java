package com.infopharma.ipos_sa.controller;

import com.infopharma.ipos_sa.dto.DispatchRequest;
import com.infopharma.ipos_sa.dto.OrderRequest;
import com.infopharma.ipos_sa.entity.Order;
import com.infopharma.ipos_sa.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller (web layer) tests for OrderController.
 *
 * These tests use MockMvc to simulate real HTTP requests without starting
 * a full server. The service layer is mocked, so we're only testing that
 * the controller:
 *   - Accepts the right request format
 *   - Returns the correct HTTP status codes
 *   - Returns the expected fields in the JSON response
 *
 * @WebMvcTest loads only the web layer (controllers, filters, etc.)
 * @WithMockUser provides a fake logged-in user so Spring Security doesn't
 *               block requests with 401/403
 * .with(csrf()) adds the CSRF token required for POST and PUT requests
 */
@WebMvcTest(OrderController.class)
@WithMockUser
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // The service is mocked — we define what it returns in each test
    @MockitoBean OrderService orderService;

    // ----------------------------- POST /api/orders -----------------------------

    // A valid order request should return 201 Created with the new order in the body
    @Test
    void placeOrder_validRequest_returns201WithOrder() throws Exception {
        OrderRequest.OrderItemRequest itemReq = new OrderRequest.OrderItemRequest();
        itemReq.setItemId("ITEM001");
        itemReq.setQuantity(2);

        OrderRequest request = new OrderRequest();
        request.setAccountId(1L);
        request.setItems(List.of(itemReq));

        Order order = buildOrder("IP1234", Order.OrderStatus.ACCEPTED, Order.PaymentStatus.PENDING, "50.00");
        when(orderService.placeOrder(any(OrderRequest.class))).thenReturn(order);

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("IP1234"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));
    }

    // If the account doesn't exist, the service throws EntityNotFoundException
    // which the GlobalExceptionHandler maps to a 404 response
    @Test
    void placeOrder_accountNotFound_returns404() throws Exception {
        OrderRequest request = new OrderRequest();
        request.setAccountId(99L);
        request.setItems(List.of());

        when(orderService.placeOrder(any())).thenThrow(new EntityNotFoundException("Account not found: 99"));

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ----------------------------- GET /api/orders -----------------------------

    // Fetching all orders should return a 200 with a JSON array of all orders
    @Test
    void getAllOrders_returns200WithList() throws Exception {
        Order o1 = buildOrder("IP0001", Order.OrderStatus.ACCEPTED, Order.PaymentStatus.PENDING, "100.00");
        Order o2 = buildOrder("IP0002", Order.OrderStatus.DELIVERED, Order.PaymentStatus.PAID, "200.00");
        when(orderService.findAll()).thenReturn(List.of(o1, o2));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].orderId").value("IP0001"))
                .andExpect(jsonPath("$[1].orderId").value("IP0002"));
    }

    // If there are no orders yet, the response should still be 200 with an empty array
    @Test
    void getAllOrders_emptyList_returns200WithEmptyArray() throws Exception {
        when(orderService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ----------------------------- GET /api/orders/incomplete -----------------------------

    // Fetching incomplete orders should return only orders that haven't been delivered yet
    @Test
    void getIncompleteOrders_returns200WithIncompleteOrders() throws Exception {
        Order o = buildOrder("IP1111", Order.OrderStatus.DISPATCHED, Order.PaymentStatus.PENDING, "75.00");
        when(orderService.findIncomplete()).thenReturn(List.of(o));

        mockMvc.perform(get("/api/orders/incomplete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("DISPATCHED"));
    }

    // ----------------------------- GET /api/orders/my -----------------------------

    // A merchant fetching their own orders should only see orders for their account ID
    @Test
    void getMyOrders_validAccountId_returns200WithOrders() throws Exception {
        Order o = buildOrder("IP2222", Order.OrderStatus.ACCEPTED, Order.PaymentStatus.PENDING, "50.00");
        when(orderService.findByAccountId(1L)).thenReturn(List.of(o));

        mockMvc.perform(get("/api/orders/my").param("accountId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderId").value("IP2222"));
    }

    // ----------------------------- GET /api/orders/{id} -----------------------------

    // Fetching a specific order by its ID should return 200 with the order details
    @Test
    void getOrder_existingId_returns200WithOrder() throws Exception {
        Order order = buildOrder("IP3333", Order.OrderStatus.ACCEPTED, Order.PaymentStatus.PENDING, "120.00");
        when(orderService.findById("IP3333")).thenReturn(Optional.of(order));

        mockMvc.perform(get("/api/orders/IP3333"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("IP3333"));
    }

    // Fetching an order ID that doesn't exist should return 404 Not Found
    @Test
    void getOrder_nonExistingId_returns404() throws Exception {
        when(orderService.findById("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/orders/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    // ----------------------------- PUT /api/orders/{id}/dispatch -----------------------------

    // A valid dispatch request should update the order status to DISPATCHED and return 200
    @Test
    void dispatchOrder_validRequest_returns200() throws Exception {
        DispatchRequest dispatchRequest = new DispatchRequest();
        dispatchRequest.setDispatchedBy("Alice");
        dispatchRequest.setCourier("DHL");
        dispatchRequest.setCourierRef("DHL-999");

        Order dispatched = buildOrder("IP4444", Order.OrderStatus.DISPATCHED, Order.PaymentStatus.PENDING, "90.00");
        dispatched.setDispatchedBy("Alice");
        dispatched.setCourier("DHL");

        when(orderService.dispatch(eq("IP4444"), any(DispatchRequest.class))).thenReturn(dispatched);

        mockMvc.perform(put("/api/orders/IP4444/dispatch")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dispatchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISPATCHED"))
                .andExpect(jsonPath("$.dispatchedBy").value("Alice"));
    }

    // Dispatching an order that doesn't exist should return 404
    @Test
    void dispatchOrder_orderNotFound_returns404() throws Exception {
        when(orderService.dispatch(eq("UNKNOWN"), any())).thenThrow(new EntityNotFoundException("Order not found: UNKNOWN"));

        mockMvc.perform(put("/api/orders/UNKNOWN/dispatch")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DispatchRequest())))
                .andExpect(status().isNotFound());
    }

    // ----------------------------- PUT /api/orders/{id}/delivered -----------------------------

    // Marking a valid order as delivered should return 200 with DELIVERED status
    @Test
    void markDelivered_validId_returns200() throws Exception {
        Order delivered = buildOrder("IP5555", Order.OrderStatus.DELIVERED, Order.PaymentStatus.PENDING, "60.00");
        delivered.setDeliveryDate(LocalDate.now());

        when(orderService.markDelivered("IP5555")).thenReturn(delivered);

        mockMvc.perform(put("/api/orders/IP5555/delivered").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    // Marking a non-existent order as delivered should return 404
    @Test
    void markDelivered_orderNotFound_returns404() throws Exception {
        when(orderService.markDelivered("UNKNOWN")).thenThrow(new EntityNotFoundException("Order not found: UNKNOWN"));

        mockMvc.perform(put("/api/orders/UNKNOWN/delivered").with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ----------------------------- helpers -----------------------------

    // Builds a simple Order object with the given fields for use in test mocks
    private Order buildOrder(String id, Order.OrderStatus status, Order.PaymentStatus paymentStatus, String total) {
        Order order = new Order();
        order.setOrderId(id);
        order.setStatus(status);
        order.setPaymentStatus(paymentStatus);
        order.setTotalValue(new BigDecimal(total));
        order.setOrderDate(LocalDate.now());
        order.setDiscountApplied(BigDecimal.ZERO);
        return order;
    }
}
