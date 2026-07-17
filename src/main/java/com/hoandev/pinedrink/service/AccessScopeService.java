package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.Order;
import com.hoandev.pinedrink.entity.Scope;
import com.hoandev.pinedrink.security.scope.AccessScopeContext;

/**
 * Giải quyết và thực thi quyền truy cập phạm vi tài khoản trên các dịch vụ nhận biết chi nhánh.
 */
public interface AccessScopeService {

    /**
     * Giải quyết quyền cấp phạm vi cho tài khoản hiện tại đã xác thực.
     *
     * @return phạm vi cho tài khoản hiện tại
     */
    AccessScopeContext resolveCurrentScope();
    /**
     * Giải quyết quyền cấp phạm vi cho một tài khoản cụ thể.
     *
     * @param accountId ID tài khoản cần giải quyết
     * @return phạm vi cho tài khoản mục tiêu
     */
    AccessScopeContext resolveScopeByAccountId(String accountId);
    /**
     * Đảm bảo tài khoản hiện tại có quyền truy cập cấp SYSTEM.
     */
    void assertSystemAccess();
    /**
     * Đảm bảo tài khoản hiện tại có thể xem hoặc sử dụng dữ liệu từ chi nhánh được chỉ định.
     */
    void assertCanAccessBranch(String branchId);
    /**
     * Đảm bảo tài khoản hiện tại có thể xem đơn hàng được chỉ định.
     */
    void assertCanViewOrder(Order order);
    /**
     * Đảm bảo tài khoản hiện tại có thể quản lý (tạo/cập nhật/xóa) dữ liệu trong chi nhánh được chỉ định.
     */
    void assertCanManageBranch(String branchId);
    /**
     * Đảm bảo tài khoản hiện tại có thể quản lý dữ liệu trong chi nhánh được chỉ định.
     */
    void assertCanDeleteBranch(String branchId);
    /**
     * Đảm bảo tài khoản hiện tại có thể xóa hoặc vô hiệu hóa chi nhánh được chỉ định.
     */
    void assertCanAccessAccount(String targetAccountId);
    /**
     * Đảm bảo tài khoản hiện tại có thể truy cập tài khoản mục tiêu.
     */
    void assertCanAccessScope(Scope scope);
    /**
     * Đảm bảo tài khoản hiện tại có thể truy cập phạm vi mục tiêu.
     */
    void assertCanManageTargetScope(String scopeType, String branchId);
}
