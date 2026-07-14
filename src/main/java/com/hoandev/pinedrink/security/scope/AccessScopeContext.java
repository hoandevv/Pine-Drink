package com.hoandev.pinedrink.security.scope;

import java.util.Set;

/**
 * Mô tả phạm vi truy cập chi nhánh của tài khoản đang được xác thực.
 *
 * Nếu {@code fullAccess = true}, tài khoản có phạm vi SYSTEM
 * và được phép truy cập tất cả chi nhánh.
 *
 * Ngược lại, tài khoản chỉ được phép truy cập các chi nhánh
 * có mã nằm trong danh sách {@code branchIds}.
 *
 * @param fullAccess cho biết tài khoản hiện tại có được truy cập tất cả chi nhánh hay không
 * @param branchIds tập hợp mã chi nhánh được cấp quyền thông qua
 *                  các phân quyền có phạm vi BRANCH đang hoạt động
 */
public record AccessScopeContext(
        boolean fullAccess,
        Set<String> branchIds
) {
}