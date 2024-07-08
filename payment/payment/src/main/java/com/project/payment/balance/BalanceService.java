package com.project.payment.balance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.DTO.balance.BalanceDTO;
import com.example.DTO.order.OrdersDTOSend;
import com.example.enume.OrderStatusEnum;
import com.project.payment.exception.PaymentException;
import com.project.payment.payment.PaymentService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class BalanceService {

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private PaymentService paymentService;

    public Flux<Balance> getBalance() {
        return balanceRepository.findAll()
                .switchIfEmpty(
                        Mono.error(
                                new PaymentException(
                                        String.format("Can't find balance info"))));
    }

    public Mono<Balance> getBalanceById(Long id) {
        return balanceRepository.findById(id)
                .switchIfEmpty(Mono.error(
                        new PaymentException(
                                String.format("Can't find balance info id: " + id))));
    }

    public Mono<Balance> addBalanceInfo(BalanceDTO balanceDTO) {
        Balance balance = Balance.builder()
                .amount(balanceDTO.getAmount())
                .build();

        return balanceRepository.save(balance)
                .flatMap(savedBalance -> {
                    log.info("Balance created with id: {}", savedBalance.getId());
                    savedBalance.setCustomer_id(savedBalance.getId());
                    return getBalanceById(savedBalance.getId())
                            .flatMap(balance2 -> {
                                balance2.setCustomer_id(balance2.getId());
                                return balanceRepository.save(balance2);
                            });
                })
                .doOnError(error -> log.error("Error occurred: " + error.getMessage()));
    }

    public Mono<Balance> updateBalance(Long id, @Valid BalanceDTO balanceDTO) {
        return balanceRepository.findById(id)
                .flatMap(savedBalance -> {
                    // updating the balance amount
                    savedBalance.setAmount(savedBalance.getAmount() + balanceDTO.getAmount());
                    return balanceRepository.save(savedBalance);
                })
                .doOnError(error -> log.error("Error occurred: " + error.getMessage()));
    }

    public Mono<OrdersDTOSend> checkBalanceAndDeduct(@Valid OrdersDTOSend ordersDTO) {
        Long id = ordersDTO.getOrdersDTO().getCustomer_id(); // customer id
        Float price = ordersDTO.getOrdersDTO().getTotal_amount(); // total price of the product
        log.info("Checking the balance for id: " + id);
        log.info("Total price of the product: " + price);

        return balanceRepository.findById(id) // find by id
                .flatMap(savedInfo -> {
                    // check if the account can buy the product
                    if (savedInfo.getAmount() < price) {
                        log.info("Change the order status FAILED(Payment Failed)");
                        ordersDTO.getOrdersDTO().setOrder_status(OrderStatusEnum.FAILED.name());
                        paymentService.addTransactionDetailsFail(ordersDTO).subscribe();
                        return Mono.just(ordersDTO);
                    } else {
                        log.info("Change the order status COMPLETED(Payment Success)");
                        savedInfo.setAmount(savedInfo.getAmount() - price);
                        ordersDTO.getOrdersDTO().setOrder_status(OrderStatusEnum.COMPLETED.name());
                        paymentService.addTransactionDetailsSuccess(ordersDTO).subscribe();
                        return balanceRepository.save(savedInfo)
                                .flatMap(balance -> {
                                    log.info("Balance updated with id: {}", balance.getId());
                                    return Mono.just(ordersDTO);
                                });
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Account not found --> order (FAIL) for item: " + ordersDTO);
                    ordersDTO.getOrdersDTO().setOrder_status(OrderStatusEnum.FAILED.name());
                    Mono.error(new PaymentException("Customer not found. Id: " + id));
                    paymentService.addTransactionDetailsFail(ordersDTO).subscribe();
                    return Mono.just(ordersDTO);
                }));
    }
}
