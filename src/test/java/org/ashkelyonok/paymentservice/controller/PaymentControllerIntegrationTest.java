package org.ashkelyonok.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.ashkelyonok.paymentservice.AbstractIntegrationTest;
import org.ashkelyonok.paymentservice.model.dto.request.PaymentCreateDto;
import org.ashkelyonok.paymentservice.model.entity.Payment;
import org.ashkelyonok.paymentservice.model.enums.PaymentStatus;
import org.ashkelyonok.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String BASE_URL = "/api/v1/payments";
    private static final String AUTH_HEADER = "Authorization";
    private static final String ROLE_USER = "ROLE_USER";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String TEST_USER_EMAIL = "user@test.com";
    private static final String TEST_ADMIN_EMAIL = "admin@test.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("Create Payment: Returns SUCCESS status")
    void createPayment_ShouldReturnSuccess() throws Exception {
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("2")));

        PaymentCreateDto dto = PaymentCreateDto.builder()
                .orderId(100L)
                .userId(1L)
                .paymentAmount(BigDecimal.valueOf(250.00))
                .build();

        String token = generateTestToken(1L, TEST_USER_EMAIL, ROLE_USER);

        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL)
                        .header(AUTH_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(PaymentStatus.SUCCESS.name())));

        Payment savedPayment = paymentRepository.findByOrderId(100L).orElseThrow();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    @DisplayName("Create Payment: Returns FAILED status")
    void createPayment_ShouldReturnFailed() throws Exception {
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("3")));

        PaymentCreateDto dto = PaymentCreateDto.builder()
                .orderId(200L)
                .userId(1L)
                .paymentAmount(BigDecimal.valueOf(100.00))
                .build();

        String token = generateTestToken(1L, TEST_USER_EMAIL, ROLE_USER);

        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL)
                        .header(AUTH_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is(PaymentStatus.FAILED.name())));
    }

    @Test
    @DisplayName("Create Payment: Validation Error on Invalid Operation")
    void createPayment_ShouldReturnBadRequest() throws Exception {
        Payment payment = new Payment();
        payment.setOrderId(300L);
        payment.setUserId(1L);
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        PaymentCreateDto dto = PaymentCreateDto.builder()
                .orderId(300L)
                .userId(1L)
                .paymentAmount(BigDecimal.valueOf(150.00))
                .build();

        String token = generateTestToken(1L, TEST_USER_EMAIL, ROLE_USER);

        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL)
                        .header(AUTH_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid Operation")));
    }

    @Test
    @DisplayName("Create Payment: Forbidden When User Tries to Pay for Another User")
    void createPayment_ShouldReturnForbidden_WhenUserIsNotOwner() throws Exception {
        PaymentCreateDto dto = PaymentCreateDto.builder()
                .orderId(301L)
                .userId(2L)
                .paymentAmount(BigDecimal.valueOf(150.00))
                .build();

        String token = generateTestToken(1L, TEST_USER_EMAIL, ROLE_USER);

        mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL)
                        .header(AUTH_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Search Payments by Order ID: Success")
    void searchPayments_ByOrderId_ShouldReturnPayment() throws Exception {
        Payment payment = new Payment();
        payment.setOrderId(400L);
        payment.setUserId(2L);
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        String token = generateTestToken(2L, "user2@test.com", ROLE_USER);

        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL)
                        .header(AUTH_HEADER, token)
                        .param("orderId", "400"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].orderId", is(400)))
                .andExpect(jsonPath("$.content[0].status", is(PaymentStatus.SUCCESS.name())));
    }

    @Test
    @DisplayName("Search Payments by Order ID: Forbidden")
    void searchPayments_ByOrderId_ShouldReturnForbidden_WhenUserIsNotOwner() throws Exception {
        Payment payment = new Payment();
        payment.setOrderId(401L);
        payment.setUserId(2L);
        paymentRepository.save(payment);

        String token = generateTestToken(1L, TEST_USER_EMAIL, ROLE_USER);

        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL)
                        .header(AUTH_HEADER, token)
                        .param("orderId", "401"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Search Payments by Order ID: Not Found")
    void searchPayments_ByOrderId_ShouldReturnNotFound() throws Exception {
        String token = generateTestToken(1L, TEST_USER_EMAIL, ROLE_USER);

        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL)
                        .header(AUTH_HEADER, token)
                        .param("orderId", "9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Search Payments by User ID: Success")
    void searchPayments_ByUserId_ShouldReturnPagedList() throws Exception {
        Payment p1 = new Payment();
        p1.setUserId(5L);
        p1.setOrderId(10L);
        paymentRepository.save(p1);

        Payment p2 = new Payment();
        p2.setUserId(5L);
        p2.setOrderId(11L);
        paymentRepository.save(p2);

        String token = generateTestToken(5L, "user5@test.com", ROLE_USER);

        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL)
                        .header(AUTH_HEADER, token)
                        .param("userId", "5")
                        .param("size", "10")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    @DisplayName("Search Payments by User ID: Forbidden")
    void searchPayments_ByUserId_ShouldReturnForbidden_WhenUserIsNotOwner() throws Exception {
        String token = generateTestToken(1L, TEST_USER_EMAIL, ROLE_USER);

        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL)
                        .header(AUTH_HEADER, token)
                        .param("userId", "5"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Search Payments by Status: Success for Admin")
    void searchPayments_ByStatus_ShouldReturnPagedList_WhenUserIsAdmin() throws Exception {
        Payment p1 = new Payment();
        p1.setUserId(1L);
        p1.setOrderId(50L);
        p1.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(p1);

        String token = generateTestToken(99L, TEST_ADMIN_EMAIL, ROLE_ADMIN);

        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL)
                        .header(AUTH_HEADER, token)
                        .param("status", PaymentStatus.SUCCESS.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is(PaymentStatus.SUCCESS.name())));
    }

    @Test
    @DisplayName("Search Payments by Status: Forbidden for User")
    void searchPayments_ByStatus_ShouldReturnForbidden_WhenUserIsNotAdmin() throws Exception {
        String token = generateTestToken(1L, TEST_USER_EMAIL, ROLE_USER);

        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL)
                        .header(AUTH_HEADER, token)
                        .param("status", PaymentStatus.SUCCESS.name()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Get Total Sum For User: Success")
    void getTotalSumForUser_ShouldReturnSum() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        Payment p1 = Payment.builder()
                .orderId(2001L)
                .userId(10L)
                .paymentAmount(BigDecimal.valueOf(100.50))
                .status(PaymentStatus.SUCCESS)
                .timestamp(now)
                .build();
        paymentRepository.save(p1);

        Payment p2 = Payment.builder()
                .orderId(2002L)
                .userId(10L)
                .paymentAmount(BigDecimal.valueOf(50.25))
                .status(PaymentStatus.SUCCESS)
                .timestamp(now.minusHours(1))
                .build();
        paymentRepository.save(p2);

        String token = generateTestToken(10L, "user10@test.com", ROLE_USER);

        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/users/10/sum")
                        .header(AUTH_HEADER, token)
                        .param("startDate", now.minusDays(1).toString())
                        .param("endDate", now.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(150.75)));
    }

    @Test
    @DisplayName("Get Total Sum For User: Forbidden")
    void getTotalSumForUser_ShouldReturnForbidden_WhenUserIsNotOwner() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String token = generateTestToken(1L, TEST_USER_EMAIL, ROLE_USER);

        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/users/10/sum")
                        .header(AUTH_HEADER, token)
                        .param("startDate", now.minusDays(1).toString())
                        .param("endDate", now.plusDays(1).toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Get Total Sum For All Users: Admin Success")
    void getTotalSumForAllUsers_ShouldReturnSum_WhenUserIsAdmin() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        Payment p1 = Payment.builder()
                .orderId(3001L)
                .userId(1L)
                .paymentAmount(BigDecimal.valueOf(200.50))
                .status(PaymentStatus.SUCCESS)
                .timestamp(now)
                .build();
        paymentRepository.save(p1);

        Payment p2 = Payment.builder()
                .orderId(3002L)
                .userId(2L)
                .paymentAmount(BigDecimal.valueOf(300.25))
                .status(PaymentStatus.SUCCESS)
                .timestamp(now)
                .build();
        paymentRepository.save(p2);

        String token = generateTestToken(99L, TEST_ADMIN_EMAIL, ROLE_ADMIN);

        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/sum")
                        .header(AUTH_HEADER, token)
                        .param("startDate", now.minusDays(1).toString())
                        .param("endDate", now.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(500.75)));
    }

    @Test
    @DisplayName("Get Total Sum For All Users: User Forbidden")
    void getTotalSumForAllUsers_ShouldReturnForbidden_WhenUserIsNotAdmin() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String token = generateTestToken(1L, TEST_USER_EMAIL, ROLE_USER);

        mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/sum")
                        .header(AUTH_HEADER, token)
                        .param("startDate", now.minusDays(1).toString())
                        .param("endDate", now.plusDays(1).toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Kafka Consumer: Initializes Payment on OrderCreatedEvent")
    void shouldInitializePayment_WhenOrderCreatedEventReceived() {
        Map<String, Object> orderCreatedPayload = Map.of(
                "id", 888L,
                "orderId", 888L,
                "userId", 42L,
                "amount", 199.99,
                "totalAmount", 199.99,
                "paymentAmount", 199.99
        );

        kafkaTemplate.send("order-created-topic", orderCreatedPayload);

        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    var payment = paymentRepository.findByOrderId(888L);
                    assertThat(payment).isPresent();
                    assertThat(payment.get().getStatus()).isEqualTo(PaymentStatus.PENDING);
                });
    }
}