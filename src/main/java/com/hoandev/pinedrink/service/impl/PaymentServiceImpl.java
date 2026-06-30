package com.hoandev.pinedrink.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoandev.pinedrink.configuration.MomoProperties;
import com.hoandev.pinedrink.entity.Order;
import com.hoandev.pinedrink.entity.PaymentIntent;
import com.hoandev.pinedrink.entity.PaymentTransaction;
import com.hoandev.pinedrink.entity.dto.request.Payment.MomoCreatePaymentRequest;
import com.hoandev.pinedrink.entity.dto.request.Payment.MomoIpnRequest;
import com.hoandev.pinedrink.entity.dto.request.Payment.RecordOfflinePaymentRequest;
import com.hoandev.pinedrink.entity.dto.response.Payment.MomoCreatePaymentResponse;
import com.hoandev.pinedrink.entity.dto.response.Payment.MomoIpnResponse;
import com.hoandev.pinedrink.entity.dto.response.Payment.PaymentTransactionResponse;
import com.hoandev.pinedrink.entity.enums.OrderStatus;
import com.hoandev.pinedrink.entity.enums.PaymentProvider;
import com.hoandev.pinedrink.entity.enums.PaymentStatus;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.PaymentMapper;
import com.hoandev.pinedrink.repository.OrderRepository;
import com.hoandev.pinedrink.repository.PaymentIntentRepository;
import com.hoandev.pinedrink.repository.PaymentTransactionRepository;
import com.hoandev.pinedrink.service.AccessScopeService;
import com.hoandev.pinedrink.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final String METHOD_CASH = "CASH";
    private static final String METHOD_COD = "COD";
    private static final String METHOD_MOMO = PaymentProvider.MOMO.getValue();
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PAID = PaymentStatus.PAID.getValue();
    private static final String STATUS_FAILED = PaymentStatus.FAILED.getValue();
    private static final String STATUS_UNPAID = PaymentStatus.UNPAID.getValue();
    private static final String ORDER_CANCELLED = OrderStatus.CANCELLED.getValue();
    private static final String ORDER_REJECTED = OrderStatus.REJECTED.getValue();

    private final OrderRepository orderRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentMapper paymentMapper;
    private final AccessScopeService accessScopeService;
    private final MomoProperties momoProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    /**
     * Đánh dấu đơn CASH/COD là đã thanh toán theo thao tác của nhân viên.
     * Khóa đơn trước khi cập nhật để tránh 2 request ghi nhận thanh toán cùng lúc.
     */
    public PaymentTransactionResponse recordOfflinePayment(RecordOfflinePaymentRequest request) {
        String paymentMethod = normalizeOfflineMethod(request.getPaymentMethod());
        LocalDateTime now = LocalDateTime.now();
        Order order = orderRepository.findByIdForUpdate(request.getOrderId())
                .orElseThrow(() -> new BaseException(ErrorCode.ORDER_001));

        validateOrderCanRecordOfflinePayment(order, paymentMethod);

        if (STATUS_PAID.equals(order.getPaymentStatus())) {
            return handleAlreadyPaidOrder(order, paymentMethod);
        }

        PaymentIntent intent = getOrCreateIntent(order, paymentMethod);
        PaymentTransaction transaction = paymentTransactionRepository
                .findLatestByOrderAndMethodAndStatus(order.getId(), paymentMethod, STATUS_PENDING)
                .orElseGet(() -> createPendingTransaction(order, intent, paymentMethod));

        transaction = markOfflinePaymentAsPaid(order, intent, transaction, now);

        log.info("Offline payment recorded: orderId={}, transactionId={}, method={}",
                order.getId(), transaction.getId(), paymentMethod);
        return toResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Lấy giao dịch thanh toán mới nhất của một đơn hàng.
     * AccessScopeService đảm bảo người gọi chỉ xem được đơn thuộc phạm vi cho phép.
     */
    public PaymentTransactionResponse getLatestOrderPaymentStatus(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BaseException(ErrorCode.ORDER_001));

        accessScopeService.assertCanViewOrder(order);

        return findLatestTransaction(orderId)
                .map(this::toResponse)
                .orElseThrow(() -> new BaseException(ErrorCode.COM_005));
    }

    @Override
    @Transactional(noRollbackFor = BaseException.class)
    /**
     * Tạo giao dịch thanh toán MoMo cho đơn hàng.
     * Lưu request/response để audit và trả payUrl cho frontend redirect khách sang MoMo.
     */
    public MomoCreatePaymentResponse createMomoPayment(MomoCreatePaymentRequest request) {
        Order order = orderRepository.findByIdForUpdate(request.getOrderId())
                .orElseThrow(() -> new BaseException(ErrorCode.ORDER_001));
        validateOrderCanCreateMomoPayment(order);

        BigDecimal payableAmount = getPayableAmount(order);
        String amount = formatMomoAmount(payableAmount);
        String momoOrderId = order.getId() + "-" + System.currentTimeMillis();
        String orderInfo = blankToDefault(request.getOrderInfo(), "Pay Pine Drink order " + order.getOrderCode());
        String extraData = blankToDefault(request.getExtraData(), "");
        String signature = signMomoCreate(amount, extraData, momoOrderId, orderInfo, momoOrderId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partnerCode", momoProperties.getPartnerCode());
        payload.put("partnerName", momoProperties.getPartnerName());
        payload.put("storeId", momoProperties.getStoreId());
        payload.put("requestId", momoOrderId);
        payload.put("amount", amount);
        payload.put("orderId", momoOrderId);
        payload.put("orderInfo", orderInfo);
        payload.put("redirectUrl", momoProperties.getRedirectUrl());
        payload.put("ipnUrl", momoProperties.getIpnUrl());
        payload.put("lang", momoProperties.getLang());
        payload.put("requestType", momoProperties.getRequestType());
        payload.put("autoCapture", momoProperties.getAutoCapture());
        payload.put("extraData", extraData);
        payload.put("orderGroupId", momoProperties.getOrderGroupId());
        payload.put("signature", signature);

        PaymentIntent intent = getOrCreateIntent(order, METHOD_MOMO);
        intent.setAmount(payableAmount);
        intent.setRequestPayload(writeJson(payload));
        intent.setStatus(STATUS_PENDING);
        paymentIntentRepository.save(intent);

        PaymentTransaction transaction = createPendingTransaction(order, intent, METHOD_MOMO, momoOrderId, payableAmount);
        JsonNode response;
        try {
            response = postMomoCreate(payload);
        } catch (Exception e) {
            transaction.setStatus(STATUS_FAILED);
            transaction.setFailedReason("Cannot connect to MoMo: " + e.getMessage());
            paymentTransactionRepository.save(transaction);

            intent.setStatus(STATUS_FAILED);
            intent.setResponsePayload(e.getMessage());
            paymentIntentRepository.save(intent);

            throw new BaseException(ErrorCode.PAYMENT_001);
        }
        intent.setResponsePayload(response.toString());
        paymentIntentRepository.save(intent);

        Integer resultCode = response.path("resultCode").isInt() ? response.path("resultCode").asInt() : null;
        if (resultCode != null && resultCode != 0) {
            transaction.setStatus(STATUS_FAILED);
            transaction.setFailedReason(response.path("message").asText("MoMo create payment failed"));
            paymentTransactionRepository.save(transaction);
        }

        return MomoCreatePaymentResponse.builder()
                .orderId(momoOrderId)
                .requestId(momoOrderId)
                .payUrl(response.path("payUrl").asText(null))
                .deeplink(response.path("deeplink").asText(null))
                .qrCodeUrl(response.path("qrCodeUrl").asText(null))
                .resultCode(resultCode)
                .message(response.path("message").asText(null))
                .provider(METHOD_MOMO)
                .paymentMethod(METHOD_MOMO)
                .transactionId(transaction.getId())
                .build();
    }

    @Override
    @Transactional
    /**
     * Xử lý IPN callback từ MoMo.
     * Xác thực chữ ký, tìm đúng MoMo orderId, kiểm tra số tiền rồi mới cập nhật trạng thái thanh toán.
     */
    public MomoIpnResponse handleMomoIpn(MomoIpnRequest request) {
        if (!verifyMomoIpnSignature(request)) {
            log.warn("MoMo IPN rejected: invalid signature, orderId={}, requestId={}", request.getOrderId(), request.getRequestId());
            return momoIpnResponse(request, 1, "Invalid signature");
        }

        Optional<PaymentTransaction> latest = paymentTransactionRepository
                .findByTransactionCodeAndPaymentMethod(request.getOrderId(), METHOD_MOMO);
        if (latest.isEmpty()) {
            log.warn("MoMo IPN ignored: transaction not found, momoOrderId={}, requestId={}", request.getOrderId(), request.getRequestId());
            return momoIpnResponse(request, 0, "Transaction already processed or not found");
        }
        if (!Objects.equals(request.getOrderId(), request.getRequestId())) {
            log.warn("MoMo IPN rejected: requestId mismatch, orderId={}, requestId={}", request.getOrderId(), request.getRequestId());
            return momoIpnResponse(request, 1, "Invalid request id");
        }

        PaymentTransaction transaction = latest.get();
        if (!STATUS_PENDING.equals(transaction.getStatus())) {
            log.info("MoMo IPN ignored: transaction already processed, transactionId={}, status={}", transaction.getId(), transaction.getStatus());
            return momoIpnResponse(request, 0, "Transaction already processed");
        }

        Order order = orderRepository.findByIdForUpdate(transaction.getOrder().getId())
                .orElseThrow(() -> new BaseException(ErrorCode.ORDER_001));
        PaymentIntent intent = transaction.getPaymentIntent();

        if (request.getAmount() == null || transaction.getAmount().compareTo(BigDecimal.valueOf(request.getAmount())) != 0) {
            transaction.setStatus(STATUS_FAILED);
            transaction.setFailedReason("Amount mismatch");
            paymentTransactionRepository.save(transaction);
            log.warn("MoMo IPN rejected: amount mismatch, transactionId={}, expected={}, actual={}",
                    transaction.getId(), transaction.getAmount(), request.getAmount());
            return momoIpnResponse(request, 1, "Amount mismatch");
        }

        if (Integer.valueOf(0).equals(request.getResultCode())) {
            transaction.setStatus(STATUS_PAID);
            transaction.setPaidAt(LocalDateTime.now());
            transaction.setFailedReason(null);
            intent.setStatus(STATUS_PAID);
            order.setPaymentStatus(STATUS_PAID);
        } else {
            transaction.setStatus(STATUS_FAILED);
            transaction.setFailedReason(request.getMessage());
            intent.setStatus(STATUS_FAILED);
            order.setPaymentStatus(STATUS_UNPAID);
        }

        intent.setResponsePayload(writeJson(request));
        paymentTransactionRepository.save(transaction);
        paymentIntentRepository.save(intent);
        orderRepository.save(order);
        log.info("MoMo IPN processed: orderId={}, transactionId={}, resultCode={}, paymentStatus={}",
                order.getId(), transaction.getId(), request.getResultCode(), order.getPaymentStatus());
        return momoIpnResponse(request, 0, "Success");
    }

    @Override
    /**
     * Xác thực tham số redirect từ MoMo để frontend hiển thị kết quả.
     * Không cập nhật trạng thái thanh toán ở đây vì redirect qua browser không đủ tin cậy.
     */
    public MomoIpnResponse handleMomoReturn(Map<String, String> params) {
        MomoIpnRequest request = objectMapper.convertValue(params, MomoIpnRequest.class);
        if (!verifyMomoIpnSignature(request)) {
            return momoIpnResponse(request, 1, "Invalid signature");
        }
        return momoIpnResponse(request, 0, "Return verified");
    }

    private PaymentTransactionResponse handleAlreadyPaidOrder(Order order, String paymentMethod) {
        return findLatestTransaction(order.getId())
                .map(this::toResponse)
                .orElseGet(() -> {
                    PaymentIntent intent = getOrCreateIntent(order, paymentMethod);
                    PaymentTransaction transaction = createPaidTransactionForAlreadyPaidOrder(order, intent, paymentMethod, LocalDateTime.now());
                    return toResponse(transaction);
                });
    }

    private PaymentTransaction markOfflinePaymentAsPaid(
            Order order,
            PaymentIntent intent,
            PaymentTransaction transaction,
            LocalDateTime now
    ) {
        transaction.setStatus(STATUS_PAID);
        transaction.setPaidAt(now);
        transaction.setFailedReason(null);
        transaction = paymentTransactionRepository.save(transaction);

        intent.setStatus(STATUS_PAID);
        paymentIntentRepository.save(intent);

        order.setPaymentStatus(STATUS_PAID);
        orderRepository.save(order);

        return transaction;
    }

    private void validateOrderCanRecordOfflinePayment(Order order, String paymentMethod) {
        if (ORDER_CANCELLED.equals(order.getStatus()) || ORDER_REJECTED.equals(order.getStatus())) {
            throw new BaseException(ErrorCode.COM_004);
        }
        if (!paymentMethod.equals(order.getPaymentMethod())) {
            throw new BaseException(ErrorCode.COM_004);
        }
    }

    private PaymentIntent getOrCreateIntent(Order order, String paymentMethod) {
        return paymentIntentRepository.findByOrderAndProvider(order.getId(), paymentMethod)
                .orElseGet(() -> {
                    PaymentIntent intent = new PaymentIntent();
                    intent.setOrder(order);
                    intent.setProvider(paymentMethod);
                    intent.setAmount(order.getTotalAmount());
                    intent.setCurrency("VND");
                    intent.setStatus(STATUS_PENDING);
                    return paymentIntentRepository.save(intent);
                });
    }

    private PaymentTransaction createPendingTransaction(Order order, PaymentIntent intent, String paymentMethod) {
        return createPendingTransaction(order, intent, paymentMethod, generateTransactionCode(paymentMethod), getPayableAmount(order));
    }

    private PaymentTransaction createPendingTransaction(Order order, PaymentIntent intent, String paymentMethod, String transactionCode, BigDecimal amount) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setOrder(order);
        transaction.setPaymentIntent(intent);
        transaction.setTransactionCode(transactionCode);
        transaction.setProvider(paymentMethod);
        transaction.setPaymentMethod(paymentMethod);
        transaction.setAmount(amount);
        transaction.setCurrency("VND");
        transaction.setStatus(STATUS_PENDING);
        return paymentTransactionRepository.save(transaction);
    }

    private PaymentTransaction createPaidTransactionForAlreadyPaidOrder(Order order, PaymentIntent intent, String paymentMethod, LocalDateTime now) {
        PaymentTransaction transaction = createPendingTransaction(order, intent, paymentMethod);
        transaction.setStatus(STATUS_PAID);
        transaction.setPaidAt(now);
        intent.setStatus(STATUS_PAID);
        paymentIntentRepository.save(intent);
        return paymentTransactionRepository.save(transaction);
    }

    private Optional<PaymentTransaction> findLatestTransaction(String orderId) {
        return paymentTransactionRepository.findLatestByOrder(orderId);
    }

    private String normalizeOfflineMethod(String paymentMethod) {
        String normalized = paymentMethod == null ? "" : paymentMethod.trim().toUpperCase();
        if (!METHOD_CASH.equals(normalized) && !METHOD_COD.equals(normalized)) {
            throw new BaseException(ErrorCode.COM_004);
        }
        return normalized;
    }

    private String generateTransactionCode(String paymentMethod) {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return "PAY-" + paymentMethod + "-" + System.currentTimeMillis() + "-" + random;
    }

    private PaymentTransactionResponse toResponse(PaymentTransaction transaction) {
        return paymentMapper.toResponse(transaction, STATUS_UNPAID);
    }

    private BigDecimal getPayableAmount(Order order) {
        return order.getTotalAmount();
    }

    /**
     * Chặn tạo thanh toán online cho đơn đã hủy, đã từ chối hoặc đã thanh toán.
     */
    private void validateOrderCanCreateMomoPayment(Order order) {
        if (ORDER_CANCELLED.equals(order.getStatus()) || ORDER_REJECTED.equals(order.getStatus()) || STATUS_PAID.equals(order.getPaymentStatus())) {
            throw new BaseException(ErrorCode.COM_004);
        }
    }

    /**
     * Chuyển số tiền sang format VNĐ integer theo yêu cầu MoMo.
     * RoundingMode.UNNECESSARY cố tình báo lỗi nếu số tiền có phần thập phân.
     */
    private String formatMomoAmount(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.UNNECESSARY).toPlainString();
    }

    /**
     * Gọi API tạo thanh toán của MoMo.
     * Exception để caller xử lý để còn kịp đánh dấu transaction/intent là FAILED trước khi throw.
     */
    private JsonNode postMomoCreate(Map<String, Object> payload) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = restTemplate.postForObject(momoProperties.getEndpoint(), new HttpEntity<>(payload, headers), String.class);
        return objectMapper.readTree(body);
    }

    /**
     * Tạo raw signature cho request create-payment theo đúng thứ tự field MoMo yêu cầu.
     */
    private String signMomoCreate(String amount, String extraData, String orderId, String orderInfo, String requestId) {
        String rawSignature = "accessKey=" + momoProperties.getAccessKey()
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + momoProperties.getIpnUrl()
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + momoProperties.getPartnerCode()
                + "&redirectUrl=" + momoProperties.getRedirectUrl()
                + "&requestId=" + requestId
                + "&requestType=" + momoProperties.getRequestType();
        return hmacSha256(rawSignature);
    }

    /**
     * Tạo lại chữ ký IPN rồi so sánh với chữ ký MoMo gửi về.
     */
    private boolean verifyMomoIpnSignature(MomoIpnRequest request) {
        String rawSignature = "accessKey=" + momoProperties.getAccessKey()
                + "&amount=" + nullToEmpty(request.getAmount())
                + "&extraData=" + nullToEmpty(request.getExtraData())
                + "&message=" + nullToEmpty(request.getMessage())
                + "&orderId=" + nullToEmpty(request.getOrderId())
                + "&orderInfo=" + nullToEmpty(request.getOrderInfo())
                + "&orderType=" + nullToEmpty(request.getOrderType())
                + "&partnerCode=" + nullToEmpty(request.getPartnerCode())
                + "&payType=" + nullToEmpty(request.getPayType())
                + "&requestId=" + nullToEmpty(request.getRequestId())
                + "&responseTime=" + nullToEmpty(request.getResponseTime())
                + "&resultCode=" + nullToEmpty(request.getResultCode())
                + "&transId=" + nullToEmpty(request.getTransId());
        return hmacSha256(rawSignature).equalsIgnoreCase(nullToEmpty(request.getSignature()));
    }

    private String hmacSha256(String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(momoProperties.getSecretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(hmac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BaseException(ErrorCode.COM_005);
        }
    }

    private MomoIpnResponse momoIpnResponse(MomoIpnRequest request, Integer resultCode, String message) {
        return MomoIpnResponse.builder()
                .partnerCode(momoProperties.getPartnerCode())
                .orderId(request.getOrderId())
                .requestId(request.getRequestId())
                .resultCode(resultCode)
                .message(message)
                .build();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BaseException(ErrorCode.COM_005);
        }
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
