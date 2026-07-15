package com.hoandev.pinedrink.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    COM_001("COM_001", "Dữ liệu gửi lên chưa hợp lệ"),
    COM_002("COM_002", "Hệ thống đang gặp sự cố. Vui lòng thử lại sau"),
    COM_003("COM_003", "Dữ liệu gửi lên không đúng định dạng"),
    COM_004("COM_004", "Tham số yêu cầu chưa hợp lệ"),
    COM_005("COM_005", "Không tìm thấy dữ liệu phù hợp"),

    AUTH_001("AUTH_001", "Tên đăng nhập/email hoặc mật khẩu chưa đúng"),
    AUTH_002("AUTH_002", "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại"),
    AUTH_003("AUTH_003", "Thông tin xác thực không hợp lệ. Vui lòng đăng nhập lại"),
    AUTH_004("AUTH_004", "Phiên đăng nhập không còn hiệu lực. Vui lòng đăng nhập lại"),
    AUTH_005("AUTH_005", "Tài khoản đang bị khóa. Vui lòng liên hệ quản trị viên"),
    AUTH_006("AUTH_006", "Tài khoản chưa được kích hoạt. Vui lòng xác thực OTP trước khi đăng nhập"),
    AUTH_007("AUTH_007", "Bạn chưa có quyền thực hiện thao tác này"),
    AUTH_008("AUTH_008", "Bạn thao tác quá nhiều lần. Vui lòng thử lại sau ít phút"),
    AUTH_009("AUTH_009", "Dịch vụ giới hạn truy cập đang tạm thời gián đoạn"),
    AUTH_010("AUTH_010", "Mật khẩu chưa đủ mạnh"),
    AUTH_011("AUTH_011", "Mã đặt lại mật khẩu đã hết hạn"),
    AUTH_012("AUTH_012", "Không tìm thấy tài khoản tương ứng"),
    AUTH_013("AUTH_013", "Tên đăng nhập này đã được sử dụng"),
    AUTH_014("AUTH_014", "Email này đã được sử dụng"),
    AUTH_015("AUTH_015", "Số điện thoại này đã được sử dụng"),
    AUTH_016("AUTH_016", "Mã OTP chưa đúng. Vui lòng kiểm tra lại"),
    AUTH_017("AUTH_017", "Mã OTP đã hết hạn. Vui lòng gửi lại mã mới"),
    AUTH_018("AUTH_018", "Tài khoản này đã được kích hoạt"),
    AUTH_019("AUTH_019", "Bạn đã nhập sai OTP quá số lần cho phép. Vui lòng gửi lại mã mới"),
    AUTH_020("AUTH_020", "Vui lòng chờ 60 giây trước khi yêu cầu gửi lại OTP"),
    AUTH_021("AUTH_021", "Tài khoản chưa được gán vào trung tâm hoạt động"),
    AUTH_022("AUTH_022", "Kênh đăng ký không hợp lệ"),
    AUTH_023("AUTH_023", "Kênh đăng ký này hiện chưa mở đăng ký công khai"),
    AUTH_024("AUTH_024", "Tên miền trung tâm đã tồn tại"),
    AUTH_025("AUTH_025", "Tài khoản đã có mật khẩu đăng nhập"),
    AUTH_026("AUTH_026", "Tài khoản chưa thiết lập mật khẩu đăng nhập"),
    AUTH_027("AUTH_027", "Mật khẩu xác nhận không khớp"),
    AUTH_028("AUTH_027", "Yêu cầu này cần phải đăng nhập"),
    AUTH_GOOGLE_001("AUTH_GOOGLE_001", "Thiếu mã xác thực Google"),
    AUTH_GOOGLE_002("AUTH_GOOGLE_002", "Mã xác thực Google không hợp lệ"),
    AUTH_GOOGLE_003("AUTH_GOOGLE_003", "Email Google chưa được xác minh"),
    AUTH_GOOGLE_004("AUTH_GOOGLE_004", "Email này đã được dùng bởi phương thức đăng nhập khác"),
    AUTH_GOOGLE_005("AUTH_GOOGLE_005", "Tài khoản Google không khớp với tài khoản hiện có"),

    RATE_LIMIT_EXCEEDED("RATE_001", "Bạn thao tác quá nhiều lần. Vui lòng thử lại sau ít phút"),

    ROLE_NOT_FOUND("ROLE_001", "Không tìm thấy vai trò phù hợp"),
    SCOPE_NOT_FOUND("SCOPE_001", "Không tìm thấy phạm vi quyền phù hợp"),

    CUSTOMER_001("CUSTOMER_001", "Không tìm thấy hồ sơ khách hàng"),
    CUSTOMER_002("CUSTOMER_002", "Không tìm thấy địa chỉ khách hàng"),
    CUSTOMER_003("CUSTOMER_003", "Địa chỉ này không thuộc khách hàng hiện tại"),
    CUSTOMER_004("CUSTOMER_004", "Không thể xóa địa chỉ mặc định"),
    CUSTOMER_005("CUSTOMER_005", "Khách hàng đã có địa chỉ mặc định"),

    BRANCH_001("BRANCH_001", "Không tìm thấy chi nhánh"),
    BRANCH_002("BRANCH_002", "Mã chi nhánh đã tồn tại"),
    BRANCH_003("BRANCH_003", "Không tìm thấy phạm vi chi nhánh"),
    BRANCH_004("BRANCH_004", "Chi nhánh đã ngừng hoạt động"),
    BRANCH_005("BRANCH_005", "Không tìm thấy giờ hoạt động của chi nhánh"),
    BRANCH_006("BRANCH_006", "Giờ hoạt động cho ngày này đã tồn tại"),
    BRANCH_007("BRANCH_007", "Giờ mở cửa phải trước giờ đóng cửa"),
    BRANCH_008("BRANCH_008", "Không tìm thấy cấu hình bán sản phẩm tại chi nhánh"),
    BRANCH_009("BRANCH_009", "Cấu hình bán sản phẩm tại chi nhánh đã tồn tại"),
    BRANCH_010("BRANCH_010", "Không tìm thấy cấu hình bán topping tại chi nhánh"),
    BRANCH_011("BRANCH_011", "Cấu hình bán topping tại chi nhánh đã tồn tại"),
    BRANCH_012("BRANCH_012", "Thời gian bắt đầu phải trước thời gian kết thúc"),

    CATEGORY_001("CATEGORY_001", "Không tìm thấy danh mục"),
    CATEGORY_002("CATEGORY_002", "Mã danh mục đã tồn tại"),
    CATEGORY_003("CATEGORY_003", "Danh mục đã ngừng hoạt động"),

    TOPPING_001("TOPPING_001", "Không tìm thấy topping"),
    TOPPING_002("TOPPING_002", "Mã topping đã tồn tại"),
    TOPPING_003("TOPPING_003", "Topping đã ngừng hoạt động"),
    TOPPING_004("TOPPING_004", "Không tìm thấy topping của sản phẩm"),
    TOPPING_005("TOPPING_005", "Topping của sản phẩm đã tồn tại"),
    TOPPING_006("TOPPING_006", "Topping của sản phẩm đã ngừng hoạt động"),
    TOPPING_007("TOPPING_007", "Topping này không thuộc sản phẩm đã chọn"),

    PRODUCT_001("PRODUCT_001", "Không tìm thấy sản phẩm"),
    PRODUCT_002("PRODUCT_002", "Mã sản phẩm đã tồn tại"),
    PRODUCT_003("PRODUCT_003", "Không tìm thấy phạm vi sản phẩm"),
    PRODUCT_004("PRODUCT_004", "Không tìm thấy danh mục sản phẩm"),
    PRODUCT_005("PRODUCT_005", "Sản phẩm đã ngừng hoạt động"),
    PRODUCT_006("PRODUCT_006", "Danh mục không thuộc phạm vi sản phẩm"),
    PRODUCT_007("PRODUCT_007", "Không tìm thấy biến thể sản phẩm"),
    PRODUCT_008("PRODUCT_008", "Mã biến thể sản phẩm đã tồn tại"),
    PRODUCT_009("PRODUCT_009", "Biến thể sản phẩm đã ngừng hoạt động"),
    PRODUCT_010("PRODUCT_010", "Biến thể này không thuộc sản phẩm đã chọn"),

    DAILY_STOCK_001("DAILY_STOCK_001", "Không tìm thấy tồn kho trong ngày"),
    DAILY_STOCK_002("DAILY_STOCK_002", "Số lượng tồn trong ngày không hợp lệ"),
    DAILY_STOCK_003("DAILY_STOCK_003", "Số lượng tồn trong ngày không đủ"),
    DAILY_STOCK_004("DAILY_STOCK_004", "Thông tin giữ tồn kho không hợp lệ"),

    ORDER_001("ORDER_001", "Không tìm thấy đơn hàng"),
    ORDER_002("ORDER_002", "Đơn hàng đã hết hạn và bị tự động từ chối"),
    ORDER_003("ORDER_003", "Đơn hàng không còn ở trạng thái chờ xác nhận"),

    PAYMENT_001("PAYMENT_001", "Cổng thanh toán đang tạm thời không khả dụng"),

    VOUCHER_001("VOUCHER_001", "Không tìm thấy voucher"),
    VOUCHER_002("VOUCHER_002", "Mã voucher đã tồn tại"),
    VOUCHER_003("VOUCHER_003", "Khoảng thời gian áp dụng voucher không hợp lệ"),
    VOUCHER_004("VOUCHER_004", "Quy tắc giảm giá của voucher không hợp lệ"),
    VOUCHER_005("VOUCHER_005", "Không thể xóa voucher vì đã có lịch sử sử dụng"),
    VOUCHER_006("VOUCHER_006", "Trạng thái voucher không hợp lệ"),
    VOUCHER_007("VOUCHER_007", "Phạm vi chi nhánh của voucher không hợp lệ"),

    REPORT_001("REPORT_001", "Chỉ hỗ trợ xuất báo cáo định dạng PDF"),
    REPORT_002("REPORT_002", "File báo cáo chưa sẵn sàng"),
    REPORT_003("REPORT_003", "Không tìm thấy job báo cáo"),
    REPORT_004("REPORT_004", "fromDate phải nhỏ hơn hoặc bằng toDate"),
    REPORT_005("REPORT_005", "Ngày bắt đầu không được nằm trong tương lai"),
    REPORT_006("REPORT_006", "Ngày kết thúc không được nằm trong tương lai"),
    REPORT_007("REPORT_007", "Khoảng ngày báo cáo không được vượt quá 366 ngày"),

    CHAT_001("CHAT_001", "Không tìm thấy phòng chat"),
    CHAT_002("CHAT_002", "Bạn không có quyền truy cập phòng chat này"),
    CHAT_003("CHAT_003", "Nội dung tin nhắn không được để trống"),
    CHAT_004("CHAT_004", "Người gửi tin nhắn không hợp lệ");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
