package com.project.product;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.DTO.order.OrdersDTOSend;
import com.example.DTO.product.ProductDTO;
import com.example.enume.OrderStatusEnum;
import com.project.product.exception.ProductException;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    public Flux<Product> getAllProduct() {
        return productRepository.findAll()
                .switchIfEmpty(Mono.error(
                        new ProductException(
                                String.format("Can't display all product"))));
    }

    // Method to add a product
    public Mono<Product> addProduct(@Valid ProductDTO product) {
        Product products = Product.builder()
                .price(product.getPrice())
                .category(product.getCategory())
                .description(product.getDescription())
                .image_url(product.getImage_url())
                .stock_quantity(product.getStock_quantity())
                // .created_at(LocalDateTime.now())
                // .published_at(LocalDateTime.now())
                .build();

        return productRepository.save(products)
                .flatMap(savedProduct -> productRepository.findById(savedProduct.getId()))
                .switchIfEmpty(Mono.error(
                        new ProductException(
                                String.format("Cannot add product"))));
    }

    public Mono<Product> getById(Long id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(
                        new ProductException(
                                String.format("Product not found. Id: %d", id))));
    }

    public Mono<Product> updateProduct(Long id, Product product) {
        return productRepository.findById(Long.valueOf(id))
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(optionalBook -> {
                    if (optionalBook.isPresent()) {
                        product.setId(id);
                        return productRepository.save(product);
                    }
                    return Mono.empty();
                })
                .switchIfEmpty(Mono.error(
                        new ProductException(
                                String.format("Cannot update product"))));
    }

    public Mono<Product> addStockQuantity(@Valid Long id, @Valid Integer sumItem) {
        return productRepository.findById(id)
                .flatMap(product -> {
                    product.setStock_quantity(product.getStock_quantity() + sumItem);
                    product.setPublished_at(LocalDateTime.now());
                    return productRepository.save(product);
                })
                .switchIfEmpty(Mono.error(new ProductException("Product not found. Id: " + id)));
    }

    public Mono<OrdersDTOSend> checkAndDeductStock(@Valid OrdersDTOSend ordersDTO) {
        Long productId = ordersDTO.getOrdersDTO().getOrderItems().getProduct_id();
        Integer orderQuantity = ordersDTO.getOrdersDTO().getOrderItems().getQuantity();

        log.info("Starting checkAndDeductStock for productId: {}, orderQuantity: {}", productId, orderQuantity);

        return productRepository.findById(productId)
                .doOnNext(product -> log.info("Product found: " + product.toString()))
                .flatMap(product -> {
                    if (product.getStock_quantity() >= orderQuantity) {
                        product.setStock_quantity(product.getStock_quantity() - orderQuantity);
                        product.setPublished_at(LocalDateTime.now());
                        ordersDTO.getOrdersDTO().getOrderItems().setPrice(product.getPrice()); // set prioe
                        ordersDTO.getOrdersDTO().setTotal_amount(product.getPrice() * orderQuantity);
                        return productRepository.save(product)
                                .flatMap(saved -> {
                                    OrdersDTOSend ordersDTOSend = new OrdersDTOSend(ordersDTO.getId(),
                                            ordersDTO.getOrdersDTO(), ordersDTO.getOrder_date());
                                    log.info("Product --> order (SUCCESS) for item: " + ordersDTOSend);
                                    ordersDTOSend.getOrdersDTO().setOrder_status(OrderStatusEnum.PROCESSING.name());
                                    return Mono.just(ordersDTOSend);
                                });
                    } else {
                        OrdersDTOSend orderDTOSend = new OrdersDTOSend(ordersDTO.getId(), ordersDTO.getOrdersDTO(),
                                ordersDTO.getOrder_date());
                        log.info("Product --> order (FAIL) for item: " + orderDTOSend);
                        orderDTOSend.getOrdersDTO().setOrder_status(OrderStatusEnum.FAILED.name());
                        orderDTOSend.getOrdersDTO().getOrderItems().setPrice(product.getPrice());
                        return Mono.just(orderDTOSend);
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Product not found --> order (FAIL) for item: " + ordersDTO);
                    ordersDTO.getOrdersDTO().setOrder_status(OrderStatusEnum.FAILED.name());
                    Mono.error(new ProductException("Product not found. Id: " + productId));
                    return Mono.just(ordersDTO);
                }));

    }

    public Mono<OrdersDTOSend> reAddStock(@Valid OrdersDTOSend ordersDTO) {
        Long product_id = ordersDTO.getOrdersDTO().getOrderItems().getProduct_id();
        Integer orderQuantity = ordersDTO.getOrdersDTO().getOrderItems().getQuantity();
        log.info("Message received, re-add the stock for id: %d by %d", product_id, orderQuantity);

        return getById(product_id)
                .flatMap(savedProduct -> {
                    savedProduct.setStock_quantity(savedProduct.getStock_quantity() + orderQuantity);
                    savedProduct.setPublished_at(LocalDateTime.now());
                    return productRepository.save(savedProduct)
                            .map(updatedProduct -> {
                                log.info("Product --> order (PAYMENT FAIL) for item: " + ordersDTO);
                                return ordersDTO;
                            });
                });
    }

    public Mono<Void> deleteById(Long id) {
        return productRepository.deleteById(id);
    }

    public Mono<Void> deleteAll() {
        return productRepository.deleteAll();
    }

}

// ini method yang menggunakan kafka
// public Mono<Product> checkAndDeductStock(OrdersDTOSend ordersDTO) {
// Long productId = ordersDTO.getOrdersDTO().getOrderItems().getProduct_id();
// Integer orderQuantity =
// ordersDTO.getOrdersDTO().getOrderItems().getQuantity();

// log.info("Starting checkAndDeductStock for productId: {}, orderQuantity: {}",
// productId, orderQuantity);

// return productRepository.findById(productId)
// .doOnNext(product -> log.info("Product found: " + product.toString()))
// .flatMap(product -> {
// if (product.getStock_quantity() >= orderQuantity) {
// product.setStock_quantity(product.getStock_quantity() - 0);// orderQuantity);
// ordersDTO.getOrdersDTO().getOrderItems().setPrice(product.getPrice());
// return productRepository.save(product)
// .doOnSuccess(saved -> {
// OrdersDTOSend orderDTOSend = new OrdersDTOSend(ordersDTO.getId(),
// ordersDTO.getOrdersDTO());
// log.info("Product --> order (SUCCESS) for item: " + orderDTOSend);
// productProducer.updateOrderMessageToOrches(orderDTOSend);
// });
// } else {
// return productRepository.findById(productId)
// .doOnNext(saved -> {
// OrdersDTOSend orderDTOSend = new OrdersDTOSend(ordersDTO.getId(),
// ordersDTO.getOrdersDTO());
// log.info("Product --> order (FAIL) for item: " + orderDTOSend);
// ;
// productProducer.updateOrderMessageToOrchesFail(orderDTOSend);
// });
// }
// // return Mono.error(new ProductException("Insufficient stock for product ID:
// "
// // + productId))
// // .flatMap(null);

// })
// // .doOnError(error -> log.error("Error occurred: " + error.getMessage()))
// // .doOnSuccess(send ->
// productProducer.updateOrderMessageToOrches(ordersDTO))
// .switchIfEmpty(Mono.defer(() -> {
// OrdersDTOSend orderDTOSend = new OrdersDTOSend(ordersDTO.getId(),
// ordersDTO.getOrdersDTO());
// log.info("Product not found --> order (FAIL) for item: " + orderDTOSend);
// productProducer.updateOrderMessageToOrchesFail(orderDTOSend);
// return Mono.error(new ProductException("Product not found for ID: " +
// productId));
// }));
// }
