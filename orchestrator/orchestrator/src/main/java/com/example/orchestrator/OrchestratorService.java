package com.example.orchestrator;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.DTO.order.OrdersDTOSend;
import com.example.orchestrator.kafka.OrchestratorProducer;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class OrchestratorService {

    private final WebClient webClient;

    private final OrchestratorProducer orchestratorProducer;

    public OrchestratorService(WebClient webClient, OrchestratorProducer orchestratorProducer) {
        this.webClient = WebClient.builder()
                // .baseUrl("http://localhost:8082")
                .build();
        this.orchestratorProducer = orchestratorProducer;
    }

    // using webclient to check and deduct the stock
    public Mono<OrdersDTOSend> checkAndDeductStock(OrdersDTOSend ordersDTO) {
        log.info("Sending it to product to check stock" + ordersDTO);

        Mono<OrdersDTOSend> ordersDTOSend = webClient
                .post()
                .uri("http://localhost:8082/inventory/check-deduct-stock")
                .bodyValue(ordersDTO)
                .retrieve()
                .bodyToMono(OrdersDTOSend.class)
                .doOnSuccess(response -> {
                    log.info("Received response from product service: {}", response);
                    if ("FAILED".equals(response.getOrdersDTO().getOrder_status())) {
                        orchestratorProducer.sendToOrderUpdateFail(response);
                    } else if ("PROCESSING".equals(response.getOrdersDTO().getOrder_status())) {
                        log.info("Product (OK) --> payment check");
                        checkAccountIdBalance(response).subscribe();
                    }
                })
                .doOnError(error -> log.error("Error occurred: {}", error.getMessage()));

        return ordersDTOSend;
    }

    // using weblient to check and deduct account balance
    public Mono<OrdersDTOSend> checkAccountIdBalance(OrdersDTOSend ordersDTO) {
        log.info("Sending it to payment to check customer's balance and deduct the balance for id: "
                + ordersDTO.getOrdersDTO().getCustomer_id());

        Mono<OrdersDTOSend> ordersDTOSend = webClient
                .post()
                .uri("http://localhost:8081/balance/check-balance")
                .bodyValue(ordersDTO)
                .retrieve()
                .bodyToMono(OrdersDTOSend.class)
                .doOnSuccess(response -> {
                    log.info("Received response from payment service: {}", response);
                    if ("FAILED".equals(response.getOrdersDTO().getOrder_status())) {
                        // send the message to order to change the order status using kafka
                        // send the message to payment to add transaction details
                        // send the message to product to re add the deducted product using webclient
                        log.info("Received response from payment service(FAIL): {}", response);
                        orchestratorProducer.sendToOrderUpdateFail(response); // kafka to order
                        paymentFailReAddStock(response).subscribe(); // webclient to product
                        //paymentFailTransactionDetails(response).subscribe();//webclient to payment 

                    } else {
                        log.info("\nReceived response from payment(SUCCESS)\n");
                        // sending the message to order to change the order status to COMPLETED
                        orchestratorProducer.sendToOrderUpdate(response);
                        // sending the message to payment to add the transaction details with web client
                    }
                })
                .doOnError(error -> log.error("Error occurred: {}", error.getMessage()));

        return ordersDTOSend;
    }

    public Mono<OrdersDTOSend> paymentFailReAddStock(OrdersDTOSend ordersDTO) {
        log.info("Sending it to product to re add stock on product for product id: "
                + ordersDTO.getOrdersDTO().getOrderItems().getProduct_id());

        Mono<OrdersDTOSend> ordersDTOSend = webClient
                .post()
                .uri("http://localhost:8082/inventory/re-add-stock")
                .bodyValue(ordersDTO)
                .retrieve()
                .bodyToMono(OrdersDTOSend.class)
                .doOnSuccess(response -> {
                    log.info("Received response from payment service(FAILED): {}", response);
                })
                .doOnError(error -> log.error("Error occurred: {}", error.getMessage()));

        return ordersDTOSend;
    }

}

// public Mono<OrdersDTOSend> checkAndDeductStock(OrdersDTOSend ordersDTO) {
// log.info("Sending it to product to check stock" + ordersDTO);

// Mono<OrdersDTOSend> ordersDTOSend = webClient
// .post()
// .uri("/inventory/check-deduct-stock")
// .bodyValue(ordersDTO)
// .retrieve()
// .bodyToMono(OrdersDTOSend.class)
// .doOnSuccess(response -> {
// log.info("Received response from product service: {}", response);
// if ("FAILED".equals(response.getOrdersDTO().getOrder_status())) {
// orchestratorProducer.sendToOrderUpdateFail(response);
// } else if ("PROCESSING".equals(response.getOrdersDTO().getOrder_status())) {
// log.info("Product (OK) --> payment check");
// checkAccountIdBalance(response);
// }
// })
// .doOnError(error -> log.error("Error occurred: {}", error.getMessage()));

// return ordersDTOSend;
// }

// public Mono<OrdersDTOSend> postDataUsingWebClient(WebClient webClient, String
// endpoint, Object requestBody) {
// webClient.post()
// .uri(endpoint) // Gunakan parameter URI endpoint
// .body(BodyInserters.fromValue(requestBody))
// .retrieve()
// .bodyToMono(String.class) // Ganti dengan tipe data yang diharapkan
// .subscribe(response -> {
// // Tindakan setelah menerima respons dari server
// System.out.println("Response: " + response);
// });
// }

// public Mono<OrdersDTOSend> paymentFailTransactionDetails(OrdersDTOSend ordersDTO) {
//     log.info("Sending it to payment service to create failed payment: {}", ordersDTO);

//     Mono<OrdersDTOSend> ordersDTOSend = webClient
//             .post()
//             .uri("http://localhost:8081/payment/create/transaction-fail")
//             .bodyValue(ordersDTO)
//             .retrieve()
//             .bodyToMono(OrdersDTOSend.class)
//             .doOnSuccess(response -> {
//                 log.info("Received response from payment service(PAYMENT FAILED): {}");
//             })
//             .doOnError(error -> log.error("Error occurred: {}", error.getMessage()));

//     return ordersDTOSend;
// }
