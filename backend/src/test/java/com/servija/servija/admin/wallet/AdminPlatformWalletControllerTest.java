package com.servija.servija.admin.wallet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.servija.servija.controller.AdminPlatformWalletController;
import com.servija.servija.dto.BankAccountDTO;
import com.servija.servija.dto.BankAccountResponse;
import com.servija.servija.dto.PlatformWithdrawalRequestDTO;
import com.servija.servija.exception.GlobalExceptionHandler;
import com.servija.servija.model.PlatformWallet;
import com.servija.servija.model.PlatformWithdrawalRequest;
import com.servija.servija.service.PlatformBankAccountService;
import com.servija.servija.service.PlatformWalletReportService;
import com.servija.servija.service.PlatformWalletService;
import com.servija.servija.service.PlatformWalletTransactionService;
import com.servija.servija.service.PlatformWithdrawalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminPlatformWalletControllerTest {

    @Mock
    private PlatformWalletService platformWalletService;

    @Mock
    private PlatformWalletTransactionService platformWalletTransactionService;

    @Mock
    private PlatformBankAccountService platformBankAccountService;

    @Mock
    private PlatformWithdrawalService platformWithdrawalService;

    @Mock
    private PlatformWalletReportService platformWalletReportService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AdminPlatformWalletController(
                        platformWalletService,
                        platformWalletTransactionService,
                        platformBankAccountService,
                        platformWithdrawalService,
                        platformWalletReportService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper)
                )
                .build();
    }

    @Test
    @DisplayName("deve retornar saldo da carteira da plataforma")
    void getWallet_returnsOk() throws Exception {
        PlatformWallet wallet = PlatformWallet.builder()
                .id(1L)
                .availableBalance(new BigDecimal("150.00"))
                .reservedBalance(new BigDecimal("20.00"))
                .totalEarned(new BigDecimal("500.00"))
                .totalWithdrawn(new BigDecimal("330.00"))
                .createdAt(LocalDateTime.of(2026, 5, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 5, 2, 10, 0))
                .build();

        when(platformWalletService.getOrCreateWallet()).thenReturn(wallet);

        mockMvc.perform(get("/api/admin/platform-wallet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.availableBalance").value(150.00))
                .andExpect(jsonPath("$.reservedBalance").value(20.00));
    }

    @Test
    @DisplayName("deve converter datas ao listar transações")
    void getTransactions_convertsDates() throws Exception {
        when(platformWalletTransactionService.getTransactions(
                "WITHDRAWAL",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/admin/platform-wallet/transactions")
                        .param("type", "WITHDRAWAL")
                        .param("dateFrom", "2026-05-01")
                        .param("dateTo", "2026-05-31"))
                .andExpect(status().isOk());

        verify(platformWalletTransactionService).getTransactions(
                "WITHDRAWAL",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );
    }

    @Test
    @DisplayName("deve cadastrar conta bancária da plataforma")
    void addBankAccount_returnsCreated() throws Exception {
        BankAccountDTO request = BankAccountDTO.builder()
                .accountType("PIX_KEY")
                .pixKey("12345678900")
                .pixKeyType("CPF")
                .holderName("Servi Ja")
                .holderCpfCnpj("12345678900")
                .build();

        BankAccountResponse response = BankAccountResponse.builder()
                .id(1L)
                .accountType("PIX_KEY")
                .pixKeyValue("12345678900")
                .pixKey("***.456.***-**")
                .pixKeyType("CPF")
                .holderName("Servi Ja")
                .holderCpfCnpj("***.456.***-**")
                .isDefault(true)
                .build();

        when(platformBankAccountService.createAccount(any())).thenReturn(response);

        mockMvc.perform(post("/api/admin/platform-wallet/bank-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountType").value("PIX_KEY"));
    }

    @Test
    @DisplayName("deve solicitar saque da carteira da plataforma")
    void requestWithdrawal_returnsCreated() throws Exception {
        PlatformWithdrawalRequestDTO request = PlatformWithdrawalRequestDTO.builder()
                .amount(new BigDecimal("200.00"))
                .bankAccountId(1L)
                .build();

        PlatformWithdrawalRequest response = PlatformWithdrawalRequest.builder()
                .id(1L)
                .amount(new BigDecimal("200.00"))
                .status(PlatformWithdrawalRequest.WithdrawalStatus.COMPLETED)
                .withdrawalMethod(PlatformWithdrawalRequest.WithdrawalMethod.PIX)
                .bankAccountId(1L)
                .destinationLabel("PIX - CPF - **900")
                .transactionId("tx-123")
                .processedAt(LocalDateTime.of(2026, 5, 10, 10, 0))
                .createdAt(LocalDateTime.of(2026, 5, 10, 9, 0))
                .build();

        when(platformWithdrawalService.requestWithdrawal(new BigDecimal("200.00"), 1L)).thenReturn(response);

        mockMvc.perform(post("/api/admin/platform-wallet/withdrawals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.withdrawalMethod").value("PIX"));
    }

    @Test
    @DisplayName("deve retornar relatório com headers corretos")
    void downloadReport_returnsHeaders() throws Exception {
        byte[] content = "test".getBytes(StandardCharsets.US_ASCII);
        LocalDate dateFrom = LocalDate.of(2026, 5, 1);
        LocalDate dateTo = LocalDate.of(2026, 5, 31);

        when(platformWalletReportService.generateReport(
                PlatformWalletReportService.ReportFormat.PDF,
                "WITHDRAWAL",
                dateFrom,
                dateTo))
                .thenReturn(new PlatformWalletReportService.ReportFile(
                        content,
                        "application/pdf",
                        "relatorio.pdf"
                ));

        mockMvc.perform(get("/api/admin/platform-wallet/report")
                        .param("format", "PDF")
                        .param("type", "WITHDRAWAL")
                        .param("dateFrom", "2026-05-01")
                        .param("dateTo", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("filename=\"relatorio.pdf\"")));
    }

    @Test
    @DisplayName("deve retornar 400 para payload inválido")
    void requestWithdrawal_returnsBadRequestForInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/admin/platform-wallet/withdrawals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.bankAccountId").exists());
    }

    @Test
    @DisplayName("deve proteger o controller com role admin")
    void controllerShouldRequireAdminRole() {
        PreAuthorize annotation = AdminPlatformWalletController.class.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('ADMIN')");
    }
}
