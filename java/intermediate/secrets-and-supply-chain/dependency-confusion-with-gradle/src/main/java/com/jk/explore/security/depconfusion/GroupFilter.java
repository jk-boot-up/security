package com.jk.explore.security.depconfusion;

/**
 * A teaching class: which groups a repository content filter covers. {@code includeGroup 'com.shop'} covers com.shop only;
 * {@code includeGroupAndSubgroups 'com.shop'} also covers com.shop.tools. (Gradle's rule, redone by hand so a test can check it.)
 */
final class GroupFilter {
    private GroupFilter() {
    }

    static boolean covers(String rule, String filterGroup, String group) {
        return switch (rule) {
            case "includeGroup", "excludeGroup" -> group.equals(filterGroup);
            case "includeGroupAndSubgroups", "excludeGroupAndSubgroups" ->
                    group.equals(filterGroup) || group.startsWith(filterGroup + ".");
            default -> throw new IllegalArgumentException("unknown rule: " + rule);
        };
    }
}
