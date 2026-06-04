package com.hoandev.pinedrink.security.scope;

import java.util.Set;

/**
 * Describes branch access resolved for the current authenticated account.
 * Full access represents SYSTEM scope; otherwise access is limited to the listed branch IDs.
 *
 * @param fullAccess whether current account can access all branches
 * @param branchIds branch IDs granted by active BRANCH scope assignments
 */
public record AccessScopeContext(boolean fullAccess, Set<String> branchIds) {
}
