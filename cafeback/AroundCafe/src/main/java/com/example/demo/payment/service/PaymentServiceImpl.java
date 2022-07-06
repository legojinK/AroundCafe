package com.example.demo.payment.service;

import com.example.demo.common.exception.ResourceNotFoundException;
import com.example.demo.member.entity.Member;
import com.example.demo.member.repository.MemberRepository;
import com.example.demo.payment.dto.*;
import com.example.demo.mypage.cafe.entity.Cafe;
import com.example.demo.mypage.cafe.entity.CafeMenu;
import com.example.demo.mypage.cafe.repository.cafe.CafeRepository;
import com.example.demo.mypage.cafe.repository.menu.MenuRepository;
import com.example.demo.payment.dto.OrderItemRequest;
import com.example.demo.payment.dto.OrderItemResponse;
import com.example.demo.payment.dto.PaymentRequest;
import com.example.demo.payment.dto.PaymentResponse;

import com.example.demo.payment.entity.OrderItem;
import com.example.demo.payment.entity.Payment;
import com.example.demo.payment.entity.PaymentStatus;
import com.example.demo.payment.map.OrderItemResponseMapStruct;
import com.example.demo.payment.map.PaymentResponseMapStruct;
import com.example.demo.payment.repository.OrderItemRepository;
import com.example.demo.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final MemberRepository memberRepository;
    private final PaymentRepository paymentRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuRepository menuRepository;
    private final CafeRepository cafeRepository;
    @Override
    @Transactional
    public Long savePayment(PaymentRequest paymentRequest) {
        log.info(paymentRequest.toString());
        Member member = memberRepository.findByMemNo(paymentRequest.getMemNo()).orElseThrow(() -> new UsernameNotFoundException("no User"));
        List<OrderItemRequest> orderItemRequest = paymentRequest.getOrderItems();

        Payment payment = Payment.builder()
                .itemInitName(paymentRequest.getItemInitName())
                .exPaymentNo(paymentRequest.getExPaymentNo())
                .paymentMethod(paymentRequest.getPaymentMethod())
                .totalQuantity(paymentRequest.getTotalQuantity())
                .totalAmount(paymentRequest.getTotalAmount())
                .totalPointAmount(paymentRequest.getTotalPointAmount())
                .paymentStatus(PaymentStatus.PAYMENT_READY)
                .member(member)
                .cafeNo(paymentRequest.getCafeNo())
                .build();

        paymentRepository.save(payment);

        for(int i = 0; i < paymentRequest.getOrderItems().size(); i++) {
            OrderItem orderItem = OrderItem.builder()
                    .itemName(orderItemRequest.get(i).getItemName())
                    .quantity(orderItemRequest.get(i).getQuantity())
                    .amount(orderItemRequest.get(i).getAmount())
                    .cafeMenuNo(orderItemRequest.get(i).getCafeMenuNo())
                    .payment(payment)
                    .build();
            orderItemRepository.save(orderItem);
        }

        return payment.getPaymentNo();
    }

    @Override
    @Transactional
    public List<PaymentResponse> getAllPaymentResponse(Member member) {
        List<Payment> paymentList =  paymentRepository.findAllByMember(member);
        List<PaymentResponse> paymentDtoList = new ArrayList<>();
        for(Payment payment : paymentList) {
            paymentDtoList.add(payment2PaymentResponse(payment));
        }
        return paymentDtoList;
    }

    @Override
    @Transactional
    public List<PaymentResponse> getAllPaymentResponse(Long cafeNo) {
        List<Payment> paymentList =  paymentRepository.findAllByCafeNo(cafeNo);
        List<PaymentResponse> paymentDtoList = new ArrayList<>();
        for(Payment payment : paymentList) {
            paymentDtoList.add(payment2PaymentResponse(payment));
        }
        return paymentDtoList;
    }

    @Transactional
    @Override
    public void modifyPaymentStatus(Long paymentNo, PaymentStatus paymentStatus) {
        paymentRepository.updatePaymentStatus(paymentNo,paymentStatus);
    }


    @Override
    @Transactional
    public PaymentResponse getPaymentResponse(Long paymentNo) throws ResourceNotFoundException {
        Payment payment = paymentRepository.findByPaymentNo(paymentNo)
                .orElseThrow(() -> new ResourceNotFoundException("payment","paymentNo",paymentNo));
        return payment2PaymentResponse(payment);
    }

    public PaymentResponse payment2PaymentResponse(Payment payment) {
        PaymentResponse paymentResponse = PaymentResponseMapStruct.instance.toDto(payment);
        Cafe cafe = cafeRepository.findByCafeNo(payment.getCafeNo()).orElseThrow(()
                -> new ResourceNotFoundException("cafe", "cafeNo", payment.getCafeNo()));

        paymentResponse.setMemNo(payment.getMember().getMemNo());
        paymentResponse.setCafeName(cafe.getCafe_name());

        List<OrderItemResponse> orderItemList = new ArrayList<>();
        for(OrderItem orderItem : payment.getOrderItem()) {
            OrderItemResponse orderItemResponse = OrderItemResponseMapStruct.instance.toDto(orderItem);
            CafeMenu cafeMenu = menuRepository.findById(orderItem.getCafeMenuNo()).orElseGet(null);
            orderItemResponse.setImageUrl(cafeMenu.getMenu_img());
            orderItemList.add(orderItemResponse);
        }
        paymentResponse.setOrderItems(orderItemList);

        return paymentResponse;
    }

    @Override
    @Transactional
    public void deletePayment(Long paymentNo) {
        paymentRepository.deleteByPaymentNo(paymentNo);
    }

    @Override
    @Transactional
    public void paymentConfirm(Long paymentNo) {
        Payment payment = paymentRepository.findByPaymentNo(paymentNo)
                .orElseThrow(() -> new ResourceNotFoundException("payment", "paymentNo", paymentNo));

        Member member = payment.getMember();
        Long memNo = member.getMemNo();
        Integer point = member.getMemPoint();
        Integer addedPoint = (int)((payment.getTotalAmount() - payment.getTotalPointAmount()) * 0.025);

        memberRepository.updateMemberPoint(memNo, (point + addedPoint));
    }

    @Scheduled(cron = "0 0 2 * 1 ?")
    public void deleteNullDate(){
        List <Payment> paymentList = paymentRepository.findByNullDate();
        if(paymentList.size() > 0) {
            for(int i =0; i < paymentList.size() ; i++)
                orderItemRepository.deleteByPayment(paymentList.get(i));
        }
        paymentRepository.deleteNullDate();
        log.info("test, delete ok");
    }


    @Override
    public List<PaymentSalesResponse> getCafeSalesList(Long cafeNo) {
        List<String> date = paymentRepository.findByDay(cafeNo);

        List<PaymentSalesResponse> paymentList = new ArrayList<>();
        if(date.size() == 0) {
            return null;
        }else {
            for(int i = 0 ; i< date.size(); i++){
                LocalDate date1 = LocalDate.parse(date.get(i));
                log.info("date =" +date1);
                Integer count = paymentRepository.findByCount(cafeNo,date1);
                Integer sum = paymentRepository.findBySum(cafeNo,date1);

                PaymentSalesResponse salesResponse = PaymentSalesResponse.builder()
                        .date(date1)
                        .total_amount(sum)
                        .total_quantity(count)
                        .build();

                paymentList.add(salesResponse);
            }
        }
        return paymentList;
    }

    @Override
    public List<PaymentSalesDetailResponse> getPaymentSalesDetailList(Long cafeNo, String date) {
        Date date1 = java.sql.Date.valueOf(date);
        List<Payment> paymentList = paymentRepository.findByCafeNoAndPaymentDateLike(cafeNo,date1);
        List<PaymentSalesDetailResponse> salesDetail = new ArrayList<>();

        if(paymentList.size() > 0 ) {
            for(int i=0; i < paymentList.size(); i ++ ) {
                Member member = memberRepository.findById(paymentList.get(i).getMember().getMemNo()).orElse(null);
                for(OrderItem orderItem : paymentList.get(i).getOrderItem()){
                    PaymentSalesDetailResponse response = PaymentSalesDetailResponse.builder()
                            .paymentNo(paymentList.get(i).getPaymentNo())
                            .paymentDate(paymentList.get(i).getPaymentDate())
                            .itemName(orderItem.getItemName())
                            .quantity(orderItem.getQuantity())
                            .amount(orderItem.getAmount())
                            .memNick(member.getMemNick())
                            .memImg(member.getMemImg())
                            .build();

                    salesDetail.add(response);
                }
            }
            return salesDetail;
        }else return null;
    }

    @Override
    public List<PaymentSalesMenuResponse1> getPaymentSalesMenuList(Long cafeNo) {
        List<PaymentSalesMenuResponse1> paymentList = paymentRepository.findByMenuList(cafeNo);
        if(paymentList.size() > 0) {
            return paymentList;
        }else return null;
    }
}
}
