package com.project.product.kafka;

// @Service
// @Slf4j
public class ProductConsumer {

    // @Autowired
    // private ProductService productService;

    // @KafkaListener(topics = "check-product", groupId = "didiKerenGroup")
    // public void consumeOrderRequest(OrdersDTOSend message) {
    //     try {
    //         log.info("Message is received and will be checked");
    //         // productService.checkAndDeductStock(message).subscribe();
    //     } catch (Exception e) {
    //         // e.getMessage();
    //     }
    // }
}

// try {
// productService.deductStockQuantity(message.getOrderItems().get(1).getProduct_id());
// // Deserialize the message to OrdersDTO
// // OrdersDTO ordersDTO = objectMapper.readValue(message, OrdersDTO.class);

// // // Check product details and stock quantity
// // productService.checkAndDeductStock(ordersDTO)
// // .doOnSuccess(product -> {
// // // Send the response back to order service
// // try {
// // String responseMessage = objectMapper.writeValueAsString(product);
// // productProducer.sendProductResponse(responseMessage);
// // } catch (JsonProcessingException e) {
// // e.printStackTrace();
// // }
// // })
// // .subscribe();
// } catch (Exception e) {
// e.printStackTrace();
// }
