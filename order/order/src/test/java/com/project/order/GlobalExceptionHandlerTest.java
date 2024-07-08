package com.project.order;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebExchangeBindException;

import com.project.order.GlobalExceptionHandlerTest.TestController;
import com.project.order.exception.ErrorResponse;
import com.project.order.exception.GlobalExceptionHandler;
import com.project.order.exception.OrderException;

import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@WebFluxTest(controllers = { GlobalExceptionHandler.class, TestController.class })
@Import(GlobalExceptionHandler.class)
public class GlobalExceptionHandlerTest {

    @Autowired
    private WebTestClient webTestClient;

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/order")
        public Mono<Void> throwOrderException() throws OrderException {
            throw new OrderException("Order not found");
        }

        @PostMapping("/bind")
        public Mono<Void> throwBindException() throws WebExchangeBindException {
            throw new WebExchangeBindException(null, null);
        }

        @PostMapping("/validation")
        public Mono<Void> throwValidationException() throws MethodArgumentNotValidException {
            throw new MethodArgumentNotValidException(null, null);
        }
    }

    @Test
    public void testOrderException() {
        webTestClient.get().uri("/test/order")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .isEqualTo(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "Order not found"));
    }

    @Test
    public void testWebExchangeBindException() {
        webTestClient.post().uri("/test/bind")
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody(Map.class)
                .isEqualTo(new HashMap<>());
    }

    @Test
    public void testMethodArgumentNotValidException() {
        webTestClient.post().uri("/test/validation")
                .exchange()

                .expectBody(Map.class)
                .isEqualTo(new HashMap<>());
    }
}
