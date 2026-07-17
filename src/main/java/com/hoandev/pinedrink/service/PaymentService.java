package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Payment.RecordOfflinePaymentRequest;
import com.hoandev.pinedrink.entity.dto.request.Payment.MomoCreatePaymentRequest;
import com.hoandev.pinedrink.entity.dto.request.Payment.MomoIpnRequest;
import com.hoandev.pinedrink.entity.dto.response.Payment.MomoCreatePaymentResponse;
import com.hoandev.pinedrink.entity.dto.response.Payment.MomoIpnResponse;
import com.hoandev.pinedrink.entity.dto.response.Payment.PaymentTransactionResponse;

import java.util.Map;

public interface PaymentService {
    /**
     * Ghi nhận thanh toán thủ công cho CASH/COD.
     * Nhân viên gọi hàm này sau khi đã nhận tiền ngoài cổng thanh toán online.
     */
    PaymentTransactionResponse recordOfflinePayment(RecordOfflinePaymentRequest request);

    /**
     * Lấy giao dịch thanh toán mới nhất của đơn hàng.
     * Có kiểm tra quyền xem đơn theo phạm vi truy cập.
     */
    PaymentTransactionResponse getLatestOrderPaymentStatus(String orderId);

    /**
     * Tạo phiên thanh toán MoMo cho đơn chưa thanh toán.
     * Kết quả trả về chứa payUrl/deeplink/QR để frontend redirect khách sang MoMo.
     */
    MomoCreatePaymentResponse createMomoPayment(MomoCreatePaymentRequest request);

    /**
     * Xử lý IPN server-to-server từ MoMo.
     * Đây là nguồn tin cậy để cập nhật trạng thái giao dịch và trạng thái thanh toán đơn hàng.
     */
    MomoIpnResponse handleMomoIpn(MomoIpnRequest request);

    /**
     * Xác thực tham số redirect từ trình duyệt sau khi khách rời MoMo.
     * Chỉ dùng để hiển thị UI, không dùng để đánh dấu đơn đã thanh toán.
     */
    MomoIpnResponse handleMomoReturn(Map<String, String> params);
}
