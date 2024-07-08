package com.project.order;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import com.example.DTO.order.OrdersDTOSend;
import com.project.order.kafka.OrderConsumer;
import com.project.order.orders.OrdersService;

import reactor.core.publisher.Mono;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = { "order-update", "order-update-fail" })
public class OrderConsumerTest {

    @Mock
    private OrdersService ordersService;

    @InjectMocks
    private OrderConsumer orderConsumer;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private OrdersDTOSend message;

    private CountDownLatch latch;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        message = new OrdersDTOSend();
        latch = new CountDownLatch(1);
    }

    @Test
    void consumeOrderResponse() throws Exception {
        when(ordersService.updateOrders2(message)).thenReturn(Mono.empty());

        ContainerProperties containerProps = new ContainerProperties("order-update");
        containerProps.setMessageListener((MessageListener<String, OrdersDTOSend>) record -> {
            orderConsumer.consumeOrderResponse(record.value());
            latch.countDown();
        });

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("didiKerenGroup", "false", embeddedKafaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        DefaultKafkaConsumerFactory<String, OrdersDTOSend> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        ConcurrentMessageListenerContainer<String, OrdersDTOSend> container =
                new ConcurrentMessageListenerContainer<>(cf, containerProps);
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

        kafkaTemplate.send("order-update", message);

        latch.await(10, TimeUnit.SECONDS);

        verify(ordersService).updateOrders2(message);

        container.stop();
    }

    @Test
    void consumeOrderResponseFail() throws Exception {
        when(ordersService.updateOrdersFail(message)).thenReturn(Mono.empty());

        ContainerProperties containerProps = new ContainerProperties("order-update-fail");
        containerProps.setMessageListener((MessageListener<String, OrdersDTOSend>) record -> {
            orderConsumer.consumeOrderResponseFail(record.value());
            latch.countDown();
        });

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("didiKerenGroup", "false", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        DefaultKafkaConsumerFactory<String, OrdersDTOSend> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        ConcurrentMessageListenerContainer<String, OrdersDTOSend> container =
                new ConcurrentMessageListenerContainer<>(cf, containerProps);
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

        kafkaTemplate.send("order-update-fail", message);

        latch.await(10, TimeUnit.SECONDS);

        verify(ordersService).updateOrdersFail(message);

        container.stop();
    }
}