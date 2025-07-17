package com.orderflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.inventory.ProductRepository;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "orderflow.simulation.delay-ms=0",
        "orderflow.payment.max-amount=1000"
})
class OrderFlowIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    ProductRepository products;

    @Test
    void happyPathEndsShippedAndConsumesStock() throws Exception {
        int before = stock("BOOK-001");

        UUID id = placeOrder("BOOK-001", 2, "10.00");

        JsonNode order = awaitStatus(id, "SHIPPED");
        assertThat(order.get("trackingNumber").asText()).startsWith("TRK-");
        assertThat(stock("BOOK-001")).isEqualTo(before - 2);
        assertThat(timelineTypes(id)).containsExactly(
                "OrderCreated", "InventoryReserved", "PaymentRequested",
                "PaymentCompleted", "OrderConfirmed", "OrderShipped");
    }

    @Test
    void outOfStockRejectsOrderAndLeavesStockUntouched() throws Exception {
        int before = stock("LAPTOP-001");

        UUID id = placeOrder("LAPTOP-001", before + 1, "1.00");

        JsonNode order = awaitStatus(id, "REJECTED");
        assertThat(order.get("statusReason").asText()).contains("Insufficient stock");
        assertThat(stock("LAPTOP-001")).isEqualTo(before);
        assertThat(timelineTypes(id)).containsExactly("OrderCreated", "InventoryRejected");
    }

    @Test
    void unknownSkuIsRejected() throws Exception {
        UUID id = placeOrder("NOPE-999", 1, "1.00");

        JsonNode order = awaitStatus(id, "REJECTED");
        assertThat(order.get("statusReason").asText()).contains("Unknown SKU");
    }

    @Test
    void declinedPaymentCancelsOrderAndReleasesStock() throws Exception {
        int before = stock("PEN-001");

        UUID id = placeOrder("PEN-001", 10, "500.00"); // 5000 > limit of 1000

        JsonNode order = awaitStatus(id, "CANCELLED");
        assertThat(order.get("statusReason").asText()).contains("declined");
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(stock("PEN-001")).isEqualTo(before));
        assertThat(timelineTypes(id)).contains("PaymentFailed", "OrderCancelled", "CompensationRequested", "InventoryReleased");
    }

    private UUID placeOrder(String sku, int quantity, String unitPrice) throws Exception {
        String body = """
                {"customerId":"cust-1","items":[{"sku":"%s","quantity":%d,"unitPrice":%s}]}
                """.formatted(sku, quantity, unitPrice);
        String response = mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        JsonNode created = mapper.readTree(response);
        assertThat(created.get("status").asText()).isEqualTo("PENDING");
        return UUID.fromString(created.get("id").asText());
    }

    private JsonNode awaitStatus(UUID id, String expected) {
        JsonNode[] last = new JsonNode[1];
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            last[0] = fetch("/orders/" + id);
            assertThat(last[0].get("status").asText()).isEqualTo(expected);
        });
        return last[0];
    }

    private List<String> timelineTypes(UUID id) throws Exception {
        return fetch("/orders/" + id + "/events").findValuesAsText("type");
    }

    private JsonNode fetch(String url) throws Exception {
        return mapper.readTree(mvc.perform(get(url)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private int stock(String sku) {
        return products.findById(sku).orElseThrow().getAvailable();
    }
}
