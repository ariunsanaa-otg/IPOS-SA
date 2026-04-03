package com.infopharma.ipos_sa.service;

import com.infopharma.ipos_sa.dto.PaymentRequest;
import com.infopharma.ipos_sa.entity.*;
import com.infopharma.ipos_sa.mapper.Mapper;
import com.infopharma.ipos_sa.repository.*;
import com.infopharma.ipos_sa.service.impl.PaymentServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * Unit tests for PaymentServiceImpl.
 *
 * Covers recording payments (including the rules around auto-restoring
 * a suspended account when its balance is cleared), and looking up invoices.
 *
 * Key business rules tested:
 *   - A SUSPENDED account is automatically restored to NORMAL when its balance reaches zero
 *   - An IN_DEFAULT account is never auto-restored — a manager must do it manually
 *   - A partial payment leaves a SUSPENDED account still suspended
 *
 * All repository calls are mocked so no database is needed.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock PaymentRepository paymentRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock UserAccountRepository userAccountRepository;
    @Mock Mapper<Payment, PaymentRequest> paymentMapper;

    @InjectMocks PaymentServiceImpl paymentService;

    // Reusable test data
    private UserAccount account;
    private Invoice invoice;
    private Order order;

    @BeforeEach
    void setUp() {
        account = new UserAccount();
        account.setAccountId(1L);
        account.setAccountStatus(UserAccount.AccountStatus.NORMAL);
        account.setBalance(new BigDecimal("200.00"));

        order = new Order();
        order.setOrderId("IP1001");
        order.setPaymentStatus(Order.PaymentStatus.PENDING);

        invoice = new Invoice();
        invoice.setInvoiceId("INV001");
        invoice.setOrder(order);
        invoice.setAccount(account);
        invoice.setAmountDue(new BigDecimal("200.00"));
    }

    // ----------------------------- recordPayment -----------------------------

    // Recording a payment for an account ID that doesn't exist should throw an error
    @Test
    void recordPayment_accountNotFound_throwsEntityNotFoundException() {
        PaymentRequest request = buildPaymentRequest(99L, "INV001", "100.00");
        when(userAccountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.recordPayment(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    // Recording a payment against an invoice that doesn't exist should throw an error
    @Test
    void recordPayment_invoiceNotFound_throwsEntityNotFoundException() {
        PaymentRequest request = buildPaymentRequest(1L, "MISSING_INV", "100.00");
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(invoiceRepository.findById("MISSING_INV")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.recordPayment(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("MISSING_INV");
    }

    // A successful full payment should: save the payment record, reduce the account balance,
    // and mark the invoice's order as PAID
    @Test
    void recordPayment_success_savesPaymentAndDeductsBalance() {
        PaymentRequest request = buildPaymentRequest(1L, "INV001", "200.00");
        Payment payment = new Payment();

        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(invoiceRepository.findById("INV001")).thenReturn(Optional.of(invoice));
        when(paymentMapper.mapFrom(request)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(userAccountRepository.save(account)).thenReturn(account);

        Payment result = paymentService.recordPayment(request);

        assertThat(payment.getAccount()).isSameAs(account);
        assertThat(payment.getInvoice()).isSameAs(invoice);
        assertThat(payment.getPaymentDate()).isEqualTo(LocalDate.now());

        // Balance reduced: 200.00 - 200.00 = 0.00
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        // The order linked to the invoice is now marked as paid
        assertThat(order.getPaymentStatus()).isEqualTo(Order.PaymentStatus.PAID);

        assertThat(result).isSameAs(payment);
    }

    // If a SUSPENDED account pays off its entire balance, it should be automatically
    // restored to NORMAL status — no manager action needed
    @Test
    void recordPayment_suspendedAccountBalanceCleared_autoRestoresToNormal() {
        account.setAccountStatus(UserAccount.AccountStatus.SUSPENDED);
        account.setBalance(new BigDecimal("100.00"));

        PaymentRequest request = buildPaymentRequest(1L, "INV001", "100.00");
        Payment payment = new Payment();

        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(invoiceRepository.findById("INV001")).thenReturn(Optional.of(invoice));
        when(paymentMapper.mapFrom(request)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(userAccountRepository.save(account)).thenReturn(account);

        paymentService.recordPayment(request);

        // Balance cleared (100 - 100 = 0), so the suspension is lifted
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.getAccountStatus()).isEqualTo(UserAccount.AccountStatus.NORMAL);
    }

    // A partial payment on a SUSPENDED account does not restore it —
    // the account stays SUSPENDED until the full balance is cleared
    @Test
    void recordPayment_suspendedAccountPartialPayment_remainsSuspended() {
        account.setAccountStatus(UserAccount.AccountStatus.SUSPENDED);
        account.setBalance(new BigDecimal("200.00"));

        PaymentRequest request = buildPaymentRequest(1L, "INV001", "50.00");
        Payment payment = new Payment();

        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(invoiceRepository.findById("INV001")).thenReturn(Optional.of(invoice));
        when(paymentMapper.mapFrom(request)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(userAccountRepository.save(account)).thenReturn(account);

        paymentService.recordPayment(request);

        // Balance is still positive after the partial payment
        assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(account.getAccountStatus()).isEqualTo(UserAccount.AccountStatus.SUSPENDED);
    }

    // An IN_DEFAULT account should remain IN_DEFAULT even after paying off its balance —
    // only a manager can manually restore an account from this state
    @Test
    void recordPayment_inDefaultAccount_notAutoRestored() {
        account.setAccountStatus(UserAccount.AccountStatus.IN_DEFAULT);
        account.setBalance(new BigDecimal("100.00"));

        PaymentRequest request = buildPaymentRequest(1L, "INV001", "100.00");
        Payment payment = new Payment();

        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(invoiceRepository.findById("INV001")).thenReturn(Optional.of(invoice));
        when(paymentMapper.mapFrom(request)).thenReturn(payment);
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(userAccountRepository.save(account)).thenReturn(account);

        paymentService.recordPayment(request);

        assertThat(account.getAccountStatus()).isEqualTo(UserAccount.AccountStatus.IN_DEFAULT);
    }

    // ----------------------------- findInvoiceById -----------------------------

    // Looking up an invoice that exists should return it
    @Test
    void findInvoiceById_found_returnsInvoice() {
        when(invoiceRepository.findById("INV001")).thenReturn(Optional.of(invoice));

        Optional<Invoice> result = paymentService.findInvoiceById("INV001");

        assertThat(result).isPresent();
        assertThat(result.get().getInvoiceId()).isEqualTo("INV001");
    }

    // Looking up an invoice ID that doesn't exist should return empty (not an error)
    @Test
    void findInvoiceById_notFound_returnsEmpty() {
        when(invoiceRepository.findById("NOPE")).thenReturn(Optional.empty());

        assertThat(paymentService.findInvoiceById("NOPE")).isEmpty();
    }

    // ----------------------------- findAllInvoices -----------------------------

    // Fetching all invoices should return every invoice in the system
    @Test
    void findAllInvoices_returnsList() {
        Invoice inv2 = new Invoice(); inv2.setInvoiceId("INV002");
        when(invoiceRepository.findAll()).thenReturn(List.of(invoice, inv2));

        List<Invoice> result = paymentService.findAllInvoices();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Invoice::getInvoiceId).containsExactly("INV001", "INV002");
    }

    // ----------------------------- findInvoicesByAccountId -----------------------------

    // Fetching invoices for an account ID that doesn't exist should throw an error
    @Test
    void findInvoicesByAccountId_accountNotFound_throwsEntityNotFoundException() {
        when(userAccountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.findInvoicesByAccountId(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    // Fetching invoices for a valid account should return only that account's invoices
    @Test
    void findInvoicesByAccountId_success_returnsInvoicesForAccount() {
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(invoiceRepository.findByAccount(account)).thenReturn(List.of(invoice));

        List<Invoice> result = paymentService.findInvoicesByAccountId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInvoiceId()).isEqualTo("INV001");
    }

    // ----------------------------- helpers -----------------------------

    private PaymentRequest buildPaymentRequest(Long accountId, String invoiceId, String amount) {
        PaymentRequest request = new PaymentRequest();
        request.setAccountId(accountId);
        request.setInvoiceId(invoiceId);
        request.setAmountPaid(new BigDecimal(amount));
        request.setPaymentMethod(Payment.PaymentMethod.BANK_TRANSFER);
        request.setRecordedBy("Staff1");
        return request;
    }
}
