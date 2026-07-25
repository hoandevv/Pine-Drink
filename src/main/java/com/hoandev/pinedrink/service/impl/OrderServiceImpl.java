package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.configuration.OrderProperties;
import com.hoandev.pinedrink.configuration.RabbitMqProperties;
import com.hoandev.pinedrink.entity.*;
import com.hoandev.pinedrink.entity.dto.request.Order.CancelOrderRequest;
import com.hoandev.pinedrink.entity.dto.request.Order.CreateOrderRequest;
import com.hoandev.pinedrink.entity.dto.request.Order.UpdateOrderStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.Order.OrderItemResponse;
import com.hoandev.pinedrink.entity.dto.response.Order.OrderItemToppingResponse;
import com.hoandev.pinedrink.entity.dto.response.Order.OrderListItemResponse;
import com.hoandev.pinedrink.entity.dto.response.Order.OrderResponse;
import com.hoandev.pinedrink.entity.enums.DiscountType;
import com.hoandev.pinedrink.entity.enums.OrderStatus;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.OrderMapper;
import com.hoandev.pinedrink.realtime.RealtimeEventFactory;
import com.hoandev.pinedrink.realtime.RealtimeEventType;
import com.hoandev.pinedrink.realtime.RealtimePublishService;
import com.hoandev.pinedrink.realtime.payload.OrderCreatedPayload;
import com.hoandev.pinedrink.realtime.payload.OrderStatusChangedPayload;
import com.hoandev.pinedrink.repository.*;
import com.hoandev.pinedrink.service.BranchVariantDailyStockService;
import com.hoandev.pinedrink.service.DeliveryFeeService;
import com.hoandev.pinedrink.service.OrderService;
import com.hoandev.pinedrink.service.PaymentService;
import com.hoandev.pinedrink.entity.dto.request.Payment.RecordOfflinePaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemToppingRepository orderItemToppingRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartItemToppingRepository cartItemToppingRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final BranchRepository branchRepository;
    private final OrderDeliveryRepository orderDeliveryRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final VoucherBranchRepository voucherBranchRepository;
    private final BranchVariantDailyStockService dailyStockService;
    private final DeliveryFeeService deliveryFeeService;
    private final OrderMapper orderMapper;
    private final OrderProperties orderProperties;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqProperties rabbitMqProperties;
    private final RealtimePublishService realtimePublishService;
    private final RealtimeEventFactory realtimeEventFactory;
    private final PaymentService paymentService;
    private final BranchHoursRepository branchHoursRepository;

    public OrderServiceImpl(OrderRepository orderRepository, OrderItemRepository orderItemRepository, OrderItemToppingRepository orderItemToppingRepository, CartRepository cartRepository, CartItemRepository cartItemRepository, CartItemToppingRepository cartItemToppingRepository, CustomerProfileRepository customerProfileRepository, CustomerAddressRepository customerAddressRepository, BranchRepository branchRepository, OrderDeliveryRepository orderDeliveryRepository, OrderStatusHistoryRepository orderStatusHistoryRepository, VoucherRepository voucherRepository, VoucherUsageRepository voucherUsageRepository, VoucherBranchRepository voucherBranchRepository, BranchVariantDailyStockService dailyStockService, DeliveryFeeService deliveryFeeService, OrderMapper orderMapper, OrderProperties orderProperties, RabbitTemplate rabbitTemplate, RabbitMqProperties rabbitMqProperties, RealtimePublishService realtimePublishService, RealtimeEventFactory realtimeEventFactory, PaymentService paymentService, BranchHoursRepository branchHoursRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderItemToppingRepository = orderItemToppingRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.cartItemToppingRepository = cartItemToppingRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.branchRepository = branchRepository;
        this.orderDeliveryRepository = orderDeliveryRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.voucherRepository = voucherRepository;
        this.voucherUsageRepository = voucherUsageRepository;
        this.voucherBranchRepository = voucherBranchRepository;
        this.dailyStockService = dailyStockService;
        this.deliveryFeeService = deliveryFeeService;
        this.orderMapper = orderMapper;
        this.orderProperties = orderProperties;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitMqProperties = rabbitMqProperties;
        this.realtimePublishService = realtimePublishService;
        this.realtimeEventFactory = realtimeEventFactory;
        this.paymentService = paymentService;
        this.branchHoursRepository = branchHoursRepository;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(String customerId, CreateOrderRequest request) {
        log.info("Creating order: customerId={}, branchId={}, orderType={}",
                customerId, request.getBranchId(), request.getOrderType());

        // Xác thực khách hàng
        CustomerProfile customer = customerProfileRepository.findById(customerId)
                .orElseThrow(() -> new BaseException(ErrorCode.CUSTOMER_001));

        // Xác thực chi nhánh
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new BaseException(ErrorCode.BRANCH_001));
        validateBranchOpen(branch.getId(), request.getPickupTime());

        // Lấy giỏ hàng đang hoạt động với khóa pessimistic để tránh race condition
        Cart cart = cartRepository.findByCustomerIdAndBranchIdAndStatusForUpdate(customerId, request.getBranchId(), "ACTIVE")
                .orElseThrow(() -> new BaseException(ErrorCode.COM_005));

        // Lấy các mục trong giỏ hàng
        List<CartItem> cartItems = cartItemRepository.findByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new BaseException(ErrorCode.COM_005);
        }
        validateOrderableCartItems(cartItems);

        // Tạo đơn hàng
        Order order = new Order();
        order.setOrderCode(generateOrderCode());
        order.setBranch(branch);
        order.setCustomer(customer);
        order.setCustomerName(customer.getFullName());
        order.setCustomerPhone(customer.getPhone());
        order.setCustomerEmail(customer.getEmail());
        order.setOrderType(request.getOrderType());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus("UNPAID");
        order.setPickupTime(request.getPickupTime());
        order.setNote(request.getNote());

        // Tính tổng tiền hàng trước
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            subtotal = subtotal.add(cartItem.getTotalPrice());
        }

        // Khởi tạo phí giao hàng bằng 0 (cho PICKUP/DINE-IN)
        order.setDeliveryFee(BigDecimal.ZERO);

        // Xử lý địa chỉ giao hàng và phí cho đơn DELIVERY
        if ("DELIVERY".equals(request.getOrderType())) {
            CustomerAddress address = hasText(request.getDeliveryAddressId())
                    ? customerAddressRepository.findByIdAndCustomerId(request.getDeliveryAddressId(), customerId)
                    .orElseThrow(() -> new BaseException(ErrorCode.CUSTOMER_002))
                    : customerAddressRepository.findByCustomerIdAndIsDefaultTrue(customerId)
                    .orElseThrow(() -> new BaseException(ErrorCode.CUSTOMER_002));

            if (hasText(address.getReceiverName())) {
                order.setCustomerName(address.getReceiverName());
            }
            if (hasText(address.getReceiverPhone())) {
                order.setCustomerPhone(address.getReceiverPhone());
            }
            order.setDeliveryAddress(formatAddress(address));
            order.setDeliveryFee(deliveryFeeService.calculate(branch, address, subtotal));
        }

        if (!hasText(order.getCustomerPhone())) {
            String accountPhone = customer.getAccount() != null ? customer.getAccount().getPhone() : null;
            if (hasText(accountPhone)) {
                order.setCustomerPhone(accountPhone);
            }
        }

        if (!hasText(order.getCustomerPhone())) {
            throw new BaseException(ErrorCode.CUSTOMER_002);
        }

        order.setSubtotalAmount(subtotal);
        BigDecimal discountAmount = calculateDiscount(request.getVoucherCode(), subtotal, branch.getId(), customerId);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(subtotal.add(order.getDeliveryFee()).subtract(discountAmount));

        // Lưu đơn hàng
        order = orderRepository.save(order);
        saveStatusHistory(order, null, OrderStatus.PENDING.getValue(), "Order created");
        saveVoucherUsage(request.getVoucherCode(), order, customer, discountAmount);
        saveDelivery(request, order);

        // Tải tất cả topping của mục giỏ hàng trong một truy vấn (khắc phục N+1)
        List<String> cartItemIds = cartItems.stream().map(CartItem::getId).collect(Collectors.toList());
        List<CartItemTopping> allCartToppings = cartItemToppingRepository.findByCartItemIdIn(cartItemIds);

        // Gom nhóm topping theo id mục giỏ hàng để tra cứu nhanh
        var toppingsByCartItemId = allCartToppings.stream()
                .collect(Collectors.groupingBy(t -> t.getCartItem().getId()));

        // Tạo các mục đơn hàng từ mục giỏ hàng
        List<OrderItem> orderItems = new ArrayList<>();
        List<OrderItemTopping> orderItemToppings = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            ProductVariant variant = cartItem.getVariant();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setVariant(variant);
            orderItem.setProductCode(product.getCode());
            orderItem.setProductName(product.getName());
            orderItem.setVariantName(variant != null ? variant.getVariantName() : null);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSugarLevel(cartItem.getSugarLevel());
            orderItem.setIceLevel(cartItem.getIceLevel());
            orderItem.setNote(cartItem.getNote());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setTotalPrice(cartItem.getTotalPrice());

            orderItem = orderItemRepository.save(orderItem);
            orderItems.add(orderItem);

            // Tạo topping đơn hàng từ topping giỏ hàng đã tải trước
            List<CartItemTopping> cartToppings = toppingsByCartItemId.getOrDefault(cartItem.getId(), new ArrayList<>());
            for (CartItemTopping cartTopping : cartToppings) {
                Topping topping = cartTopping.getTopping();

                OrderItemTopping orderTopping = new OrderItemTopping();
                orderTopping.setOrderItem(orderItem);
                orderTopping.setTopping(topping);
                orderTopping.setToppingCode(topping.getCode());
                orderTopping.setToppingName(topping.getName());
                orderTopping.setQuantity(cartTopping.getQuantity());
                orderTopping.setUnitPrice(cartTopping.getUnitPrice());
                orderTopping.setTotalPrice(cartTopping.getTotalPrice());
                orderItemToppings.add(orderTopping);
            }

            // Đặt giữ tồn kho
            if (variant != null) {
                dailyStockService.reserve(
                        branch.getId(),
                        variant.getId(),
                        LocalDate.now(),
                        cartItem.getQuantity(),
                        order.getId()
                );
            }
        }

        // Lưu hàng loạt topping của mục đơn hàng
        if (!orderItemToppings.isEmpty()) {
            orderItemToppingRepository.saveAll(orderItemToppings);
        }

        cartItemToppingRepository.deleteAll(allCartToppings);
        cartItemRepository.deleteAll(cartItems);
        cart.setStatus("INACTIVE");
        cartRepository.save(cart);

        log.info("Order created successfully: orderId={}, orderCode={}", order.getId(), order.getOrderCode());

        // Register callback to send delayed message AFTER transaction commit
        String orderId = order.getId();
        String branchId = branch.getId();
        String orderCode = order.getOrderCode();
        BigDecimal totalAmount = order.getTotalAmount();
        String customerAccountId = customerId;
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        //delay
                        sendOrderExpiryMessage(orderId);
                        // realtime
                        publishOrderCreatedEvent(orderId, orderCode, branchId, customerAccountId, totalAmount);
                    }
                }
        );

        return toOrderResponse(order);
    }

    private void validateOrderableCartItems(List<CartItem> cartItems) {
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (product == null || !"ACTIVE".equals(product.getStatus())) {
                throw new BaseException(ErrorCode.PRODUCT_002);
            }
            if (product.getCategory() == null || !"ACTIVE".equals(product.getCategory().getStatus())) {
                throw new BaseException(ErrorCode.PRODUCT_002);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String orderId, String customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BaseException(ErrorCode.ORDER_001));

        // Kiểm tra quyền sở hữu nếu customerId được cung cấp (cho vai trò khách hàng)
        if (customerId != null && !isCustomerOrder(order, customerId)) {
            throw new BaseException(ErrorCode.AUTH_007); // Insufficient permissions
        }

        return toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByCode(String orderCode, String customerId) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new BaseException(ErrorCode.ORDER_001));

        // Kiểm tra quyền sở hữu nếu customerId được cung cấp (cho vai trò khách hàng)
        if (customerId != null && !isCustomerOrder(order, customerId)) {
            throw new BaseException(ErrorCode.AUTH_007); // Insufficient permissions
        }

        return toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getCustomerOrders(String customerId, Pageable pageable) {
        Page<Order> ordersPage = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
        List<OrderResponse> responses = toOrderResponseList(ordersPage.getContent());
        return new PageImpl<>(responses, pageable, ordersPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderListItemResponse> getCustomerOrderSummaries(String customerId, Pageable pageable) {
        Page<Order> ordersPage = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
        List<OrderListItemResponse> responses = toOrderListItemResponses(ordersPage.getContent());
        return new PageImpl<>(responses, pageable, ordersPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getBranchOrders(String branchId, String status, Pageable pageable) {
        Page<Order> ordersPage;
        if (status != null && !status.isEmpty()) {
            ordersPage = orderRepository.findByBranchIdAndStatusOrderByCreatedAtDesc(branchId, status, pageable);
        } else {
            ordersPage = orderRepository.findByBranchIdOrderByCreatedAtDesc(branchId, pageable);
        }

        List<OrderResponse> responses = toOrderResponseList(ordersPage.getContent());
        return new PageImpl<>(responses, pageable, ordersPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderListItemResponse> getBranchOrderSummaries(String branchId, String status, Pageable pageable) {
        Page<Order> ordersPage;
        if (status != null && !status.isEmpty()) {
            ordersPage = orderRepository.findByBranchIdAndStatusOrderByCreatedAtDesc(branchId, status, pageable);
        } else {
            ordersPage = orderRepository.findByBranchIdOrderByCreatedAtDesc(branchId, pageable);
        }

        List<OrderListItemResponse> responses = toOrderListItemResponses(ordersPage.getContent());
        return new PageImpl<>(responses, pageable, ordersPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(String status, Pageable pageable) {
        Page<Order> ordersPage;
        if (status != null && !status.isEmpty()) {
            ordersPage = orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            ordersPage = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<OrderResponse> responses = toOrderResponseList(ordersPage.getContent());
        return new PageImpl<>(responses, pageable, ordersPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderListItemResponse> getAllOrderSummaries(String status, Pageable pageable) {
        Page<Order> ordersPage;
        if (status != null && !status.isEmpty()) {
            ordersPage = orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            ordersPage = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<OrderListItemResponse> responses = toOrderListItemResponses(ordersPage.getContent());
        return new PageImpl<>(responses, pageable, ordersPage.getTotalElements());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(String orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BaseException(ErrorCode.ORDER_001));

        String currentStatus = order.getStatus();
        String newStatus = request.getStatus();

        // Xác thực chuyển trạng thái
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            throw new BaseException(ErrorCode.COM_004); // Invalid status transition
        }

        LocalDateTime now = LocalDateTime.now();

        // Cập nhật timestamps theo trạng thái
        switch (newStatus) {
            case "CONFIRMED":
                order.setConfirmedAt(now);
                break;
            case "PREPARING":
                order.setPreparedAt(now);
                break;
            case "READY":
                order.setReadyAt(now);
                break;
            case "DELIVERING":
                order.setDeliveringAt(now);
                break;
            case "DELIVERED":
                order.setDeliveredAt(now);
                break;
            case "COMPLETED":
                recordPaymentIfRequired(order, request);
                order.setCompletedAt(now);
                confirmSoldStock(order);
                break;
            case "CANCELLED":
                order.setCancelledAt(now);
                order.setCancelReason(request.getReason());
                releaseStock(order);
                paymentService.refundPaidOrderIfNeeded(order, request.getReason());
                break;
            case "REJECTED":
                order.setRejectedAt(now);
                order.setCancelReason(request.getReason());
                releaseStock(order);
                paymentService.refundPaidOrderIfNeeded(order, request.getReason());
                break;
        }

        order.setStatus(newStatus);
        order = orderRepository.save(order);
        saveStatusHistory(order, currentStatus, newStatus, request.getReason());

        log.info("Order status updated: orderId={}, oldStatus={}, newStatus={}", orderId, currentStatus, newStatus);

        // Publish realtime event after transaction commit
        String orderCode = order.getOrderCode();
        String branchId = getBranchId(order);
        String customerAccountId = getCustomerId(order);
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        publishOrderStatusChangedEvent(orderId, orderCode, currentStatus, newStatus,
                                request.getReason(), branchId, customerAccountId);
                    }
                }
        );

        return toOrderResponse(order);
    }

    private boolean isValidStatusTransition(String currentStatus, String newStatus) {
        // Định nghĩa các chuyển trạng thái hợp lệ
        switch (currentStatus) {
            case "PENDING":
                return newStatus.equals("CONFIRMED") || newStatus.equals("REJECTED");
            case "CONFIRMED":
                return newStatus.equals("PREPARING") || newStatus.equals("REJECTED");
            case "PREPARING":
                return newStatus.equals("READY") || newStatus.equals("REJECTED");
            case "READY":
                return newStatus.equals("DELIVERING") || newStatus.equals("COMPLETED") || newStatus.equals("REJECTED");
            // mở rộng sau này ch có thể shipper ms có tể confirm
            case "DELIVERING":
                return newStatus.equals("DELIVERED")
                        || newStatus.equals("COMPLETED")
                        || newStatus.equals("REJECTED");
            case "DELIVERED":
                return newStatus.equals("COMPLETED");
            case "COMPLETED":
            case "CANCELLED":
            case "REJECTED":
                return false; // Trạng thái cuối cùng - không cho chuyển tiếp
            default:
                return false;
        }
    }

    private void recordPaymentIfRequired(Order order, UpdateOrderStatusRequest request) {
        if ("PAID".equals(order.getPaymentStatus())) {
            return;
        }

        String paymentMethod = request.getPaymentMethod();
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new BaseException(ErrorCode.COM_004);
        }

        paymentService.recordOfflinePayment(RecordOfflinePaymentRequest.builder()
                .orderId(order.getId())
                .paymentMethod(paymentMethod)
                .build());
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(String orderId, String customerId, CancelOrderRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BaseException(ErrorCode.ORDER_001));

        // Xác minh quyền sở hữu
        if (!isCustomerOrder(order, customerId)) {
            throw new BaseException(ErrorCode.AUTH_007);
        }

        String currentStatus = order.getStatus();
        OrderProperties.Cancel cancelProperties = orderProperties.getCancel();
        if (!cancelProperties.getAllowedStatuses().contains(currentStatus)) {
            throw new BaseException(ErrorCode.COM_004);
        }

        Integer timeoutMinutes = cancelProperties.getTimeoutMinutes();
        if (timeoutMinutes != null && timeoutMinutes > 0
                && order.getCreatedAt().plusMinutes(timeoutMinutes).isBefore(LocalDateTime.now())) {
            throw new BaseException(ErrorCode.COM_004);
        }

        order.setStatus("CANCELLED");
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason(request.getReason());
        paymentService.refundPaidOrderIfNeeded(order, request.getReason());
        order = orderRepository.save(order);
        saveStatusHistory(order, currentStatus, "CANCELLED", request.getReason());

        // Giải phóng tồn kho
        releaseStock(order);

        log.info("Order cancelled: orderId={}, reason={}", orderId, request.getReason());

        // Publish realtime event after transaction commit
        String orderCode = order.getOrderCode();
        String branchId = getBranchId(order);
        String customerAccountId = getCustomerId(order);
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        publishOrderStatusChangedEvent(orderId, orderCode, currentStatus, "CANCELLED",
                                request.getReason(), branchId, customerAccountId);
                    }
                }
        );

        return toOrderResponse(order);
    }

    private String generateOrderCode() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ORD-" + timestamp.substring(timestamp.length() - 8) + "-" + random;
    }

    private void sendOrderExpiryMessage(String orderId) {
        try {
            int delayMs = orderProperties.getExpire().getTimeoutMinutes() * 60 * 1000;

            rabbitTemplate.convertAndSend(
                    rabbitMqProperties.orderExpiry().exchange(),
                    rabbitMqProperties.orderExpiry().routingKey(),
                    orderId,
                    message -> {
                        message.getMessageProperties().setHeader("x-delay", delayMs);
                        return message;
                    }
            );

            log.info("Order expiry message sent: orderId={}, delay={}ms", orderId, delayMs);
        } catch (Exception e) {
            log.error("Failed to send order expiry message: orderId={}", orderId, e);
        }
    }

    private String formatAddress(CustomerAddress address) {
        return String.format("%s, %s, %s, %s - %s (%s)",
                address.getAddressLine(),
                address.getWard(),
                address.getDistrict(),
                address.getCity(),
                address.getReceiverName(),
                address.getReceiverPhone());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void releaseStock(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderIdWithVariant(order.getId());
        String branchId = getBranchId(order);
        for (OrderItem item : items) {
            ProductVariant variant = item.getVariant();
            if (variant != null) {
                dailyStockService.release(
                        branchId,
                        variant.getId(),
                        LocalDate.now(),
                        item.getQuantity(),
                        order.getId()
                );
            }
        }
    }

    private void confirmSoldStock(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderIdWithVariant(order.getId());
        String branchId = getBranchId(order);
        for (OrderItem item : items) {
            ProductVariant variant = item.getVariant();
            if (variant != null) {
                dailyStockService.confirmSold(
                        branchId,
                        variant.getId(),
                        LocalDate.now(),
                        item.getQuantity(),
                        order.getId()
                );
            }
        }
    }

    private void saveStatusHistory(Order order, String oldStatus, String newStatus, String reason) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setReason(reason);
        orderStatusHistoryRepository.save(history);
    }

    private BigDecimal calculateDiscount(String voucherCode, BigDecimal subtotal, String branchId, String customerId) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return BigDecimal.ZERO;
        }

        Voucher voucher = voucherRepository.findByCodeForUpdate(voucherCode.trim().toUpperCase())
                .orElseThrow(() -> new BaseException(ErrorCode.VOUCHER_001));
        LocalDateTime now = LocalDateTime.now();
        if (!"ACTIVE".equals(voucher.getStatus()) || voucher.getStartAt().isAfter(now) || voucher.getEndAt().isBefore(now)) {
            throw new BaseException(ErrorCode.VOUCHER_006);
        }
        if (voucher.getMinOrderAmount() != null && subtotal.compareTo(voucher.getMinOrderAmount()) < 0) {
            throw new BaseException(ErrorCode.VOUCHER_004);
        }
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new BaseException(ErrorCode.VOUCHER_006);
        }
        if (voucher.getUsageLimitPerCustomer() != null
                && voucherUsageRepository.countByVoucherIdAndCustomerId(voucher.getId(), customerId) >= voucher.getUsageLimitPerCustomer()) {
            throw new BaseException(ErrorCode.VOUCHER_006);
        }
        if (voucherBranchRepository.existsByVoucherId(voucher.getId())
                && !voucherBranchRepository.existsByVoucherIdAndBranchId(voucher.getId(), branchId)) {
            throw new BaseException(ErrorCode.VOUCHER_007);
        }

        BigDecimal discount = DiscountType.PERCENTAGE.getValue().equals(voucher.getDiscountType())
                ? subtotal.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : voucher.getDiscountValue();
        if (voucher.getMaxDiscountAmount() != null && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
            discount = voucher.getMaxDiscountAmount();
        }
        return discount.min(subtotal);
    }

    private void saveVoucherUsage(String voucherCode, Order order, CustomerProfile customer, BigDecimal discountAmount) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return;
        }

        Voucher voucher = voucherRepository.findByCodeForUpdate(voucherCode.trim().toUpperCase())
                .orElseThrow(() -> new BaseException(ErrorCode.VOUCHER_001));
        voucher.setUsedCount(voucher.getUsedCount() + 1);
        voucherRepository.save(voucher);

        VoucherUsage usage = new VoucherUsage();
        usage.setVoucher(voucher);
        usage.setOrder(order);
        usage.setCustomer(customer);
        usage.setDiscountAmount(discountAmount);
        voucherUsageRepository.save(usage);
    }

    private void saveDelivery(CreateOrderRequest request, Order order) {
        if (!"DELIVERY".equals(order.getOrderType())) {
            return;
        }

        OrderDelivery delivery = new OrderDelivery();
        delivery.setOrder(order);
        delivery.setReceiverName(order.getCustomerName());
        delivery.setReceiverPhone(order.getCustomerPhone());
        delivery.setDeliveryAddress(order.getDeliveryAddress());
        delivery.setDeliveryNote(request.getNote());
        delivery.setDeliveryFee(order.getDeliveryFee());
        orderDeliveryRepository.save(delivery);
    }

    private void publishOrderCreatedEvent(String orderId, String orderCode, String branchId,
                                          String customerAccountId, BigDecimal totalAmount) {
        try {
            OrderCreatedPayload payload = new OrderCreatedPayload(
                    orderId, orderCode, branchId, customerAccountId, totalAmount);

            var event = realtimeEventFactory.create(
                    RealtimeEventType.ORDER_CREATED,
                    customerAccountId,
                    "ORDER",
                    orderId,
                    payload);

            realtimePublishService.publishBranchOrderEvent(branchId, event);
            log.debug("Published ORDER_CREATED event: orderId={}", orderId);
        } catch (Exception e) {
            log.error("Failed to publish ORDER_CREATED event: orderId={}", orderId, e);
        }
    }

    private void publishOrderStatusChangedEvent(String orderId, String orderCode, String oldStatus,
                                                String newStatus, String reason, String branchId,
                                                String customerAccountId) {
        try {
            OrderStatusChangedPayload payload = new OrderStatusChangedPayload(
                    orderId, orderCode, oldStatus, newStatus, reason, branchId, customerAccountId);

            var event = realtimeEventFactory.create(
                    RealtimeEventType.ORDER_STATUS_CHANGED,
                    customerAccountId,
                    "ORDER",
                    orderId,
                    payload);

            realtimePublishService.publishOrderEvent(orderId, event);
            realtimePublishService.publishBranchOrderEvent(branchId, event);
            log.debug("Published ORDER_STATUS_CHANGED event: orderId={}, {} -> {}", orderId, oldStatus, newStatus);
        } catch (Exception e) {
            log.error("Failed to publish ORDER_STATUS_CHANGED event: orderId={}", orderId, e);
        }
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

        // Tải tất cả topping trong một truy vấn để tránh N+1
        List<String> orderItemIds = orderItems.stream().map(OrderItem::getId).collect(Collectors.toList());
        List<OrderItemTopping> allToppings = orderItemIds.isEmpty()
                ? new ArrayList<>()
                : orderItemToppingRepository.findByOrderItemIdIn(orderItemIds);

        // Gom nhóm topping theo id mục đơn hàng
        var toppingsByItemId = allToppings.stream()
                .collect(Collectors.groupingBy(t -> t.getOrderItem().getId()));

        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(item -> {
                    List<OrderItemTopping> toppings = toppingsByItemId.getOrDefault(item.getId(), new ArrayList<>());
                    List<OrderItemToppingResponse> toppingResponses = toppings.stream()
                            .map(orderMapper::toToppingResponse)
                            .collect(Collectors.toList());
                    return orderMapper.toItemResponse(item, toppingResponses);
                })
                .collect(Collectors.toList());

        return orderMapper.toResponse(order, itemResponses);
    }

    private List<OrderListItemResponse> toOrderListItemResponses(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> orderIds = orders.stream()
                .map(Order::getId)
                .toList();
        Map<String, List<OrderItem>> itemsByOrderId = orderItemRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        return orders.stream()
                .map(order -> toOrderListItemResponse(order, itemsByOrderId.getOrDefault(order.getId(), new ArrayList<>())))
                .toList();
    }

    private OrderListItemResponse toOrderListItemResponse(Order order, List<OrderItem> items) {
        return OrderListItemResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .customerName(order.getCustomerName())
                .orderType(order.getOrderType())
                .paymentStatus(order.getPaymentStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .totalItems(items.size())
                .itemsPreview(buildItemsPreview(items))
                .build();
    }

    private boolean isCustomerOrder(Order order, String customerId) {
        return customerId != null && customerId.equals(getCustomerId(order));
    }

    private String getCustomerId(Order order) {
        CustomerProfile customer = order.getCustomer();
        return customer != null ? customer.getId() : null;
    }

    private String getBranchId(Order order) {
        return order.getBranch().getId();
    }

    private String buildItemsPreview(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return "No items";
        }

        OrderItem firstItem = items.get(0);
        String itemName = firstItem.getProductName();
        if (itemName == null || itemName.isBlank()) {
            itemName = "Item";
        }

        String preview = itemName + " x" + firstItem.getQuantity();
        int restCount = items.size() - 1;
        if (restCount > 0) {
            preview += " and " + restCount + " other item";
            if (restCount > 1) {
                preview += "s";
            }
        }
        return preview;
    }

    private List<OrderResponse> toOrderResponseList(List<Order> orders) {
        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        // Tải tất cả mục đơn hàng trong một truy vấn
        List<String> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        List<OrderItem> allItems = orderItemRepository.findByOrderIdIn(orderIds);

        // Gom nhóm mục theo id đơn hàng
        var itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        // Tải tất cả topping trong một truy vấn
        List<String> orderItemIds = allItems.stream().map(OrderItem::getId).collect(Collectors.toList());
        List<OrderItemTopping> allToppings = orderItemIds.isEmpty()
                ? new ArrayList<>()
                : orderItemToppingRepository.findByOrderItemIdIn(orderItemIds);

        // Gom nhóm topping theo id mục đơn hàng
        var toppingsByItemId = allToppings.stream()
                .collect(Collectors.groupingBy(t -> t.getOrderItem().getId()));

        // Xây dựng danh sách phản hồi
        return orders.stream().map(order -> {
            List<OrderItem> orderItems = itemsByOrderId.getOrDefault(order.getId(), new ArrayList<>());

            List<OrderItemResponse> itemResponses = orderItems.stream()
                    .map(item -> {
                        List<OrderItemTopping> toppings = toppingsByItemId.getOrDefault(item.getId(), new ArrayList<>());
                        List<OrderItemToppingResponse> toppingResponses = toppings.stream()
                                .map(orderMapper::toToppingResponse)
                                .collect(Collectors.toList());
                        return orderMapper.toItemResponse(item, toppingResponses);
                    })
                    .collect(Collectors.toList());

            return orderMapper.toResponse(order, itemResponses);
        }).collect(Collectors.toList());
    }
    private void validateBranchOpen(String branchId, LocalDateTime requestedTime) {
        LocalDateTime time = requestedTime == null ? LocalDateTime.now() : requestedTime;
        int dayOfWeek = time.getDayOfWeek().getValue();
        LocalTime currentTime = time.toLocalTime();

        BranchHours branchHours = branchHoursRepository.findByBranchIdAndDayOfWeek(branchId, dayOfWeek)
                .orElseThrow(() -> new BaseException(ErrorCode.BRANCH_005));

        if (branchHours.isClosed()
                || currentTime.isBefore(branchHours.getOpenTime())
                || !currentTime.isBefore(branchHours.getCloseTime())) {
            throw new BaseException(ErrorCode.BRANCH_013);
        }
    }
}
