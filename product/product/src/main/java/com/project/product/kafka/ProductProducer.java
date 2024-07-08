package com.project.product.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.DTO.order.OrdersDTOSend;

import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;

@Service
@Slf4j
public class ProductProducer {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void updateOrderMessageToOrches(OrdersDTOSend message) {
        kafkaTemplate.send("update-order-orchestrator", message);
    }

    public void updateOrderMessageToOrchesFail(OrdersDTOSend message) {
        kafkaTemplate.send("update-order-orchestrator-fail", message);
        log.info("Product check --> order failed");
    }

}
