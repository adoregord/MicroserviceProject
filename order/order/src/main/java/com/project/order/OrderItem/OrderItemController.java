package com.project.order.OrderItem;

import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/orderitem")
public class OrderItemController {

    @Autowired
    private OrderItemService orderItemService;

    @GetMapping("/all") // display all order items
    public Flux<OrderItem> getAllOrderItem() {
        return orderItemService.getAllOrderItem();
    }

    @GetMapping("/{id}") // display order items by id
    public Mono<ResponseEntity<OrderItem>> getOrderItemById(@PathVariable Long id) {
        return orderItemService.getOrderItemById(id)
                .map(orderItem -> ResponseEntity.ok(orderItem))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping("/update/{id}") // update order item by id
    public Mono<ResponseEntity<OrderItem>> updateOrderItem(@PathVariable Long id, @RequestBody OrderItem orderItem) {
        return orderItemService.updateOrderItem(id, orderItem)
                .map(orderItemUpdate -> ResponseEntity.status(HttpStatus.CREATED).body(orderItemUpdate));
    }

    @DeleteMapping("/delete/{id}") // delete order item by id
    public Mono<Void> deleteOrderItemById(@PathVariable Long id) {
        return orderItemService.deleteOrderItemById(id);
    }

    @DeleteMapping("/delete") // delete all order items
    public Mono<Void> deleteAllOrderItem() {
        return orderItemService.deleteAllOrderItem();
    }

}
