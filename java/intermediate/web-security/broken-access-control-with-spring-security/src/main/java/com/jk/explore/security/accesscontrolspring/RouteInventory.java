package com.jk.explore.security.accesscontrolspring;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Lists every route the store's controllers really serve, read from Spring MVC's own handler mapping, and reports each one that
 * has no entry in {@link AccessRules}. A test asserts the list is empty, so a new route without a rule fails the build.
 */
final class RouteInventory {

    /** Every route of this store's controllers, as "METHOD pattern". A mapping with no method is listed as ANY. */
    static List<String> routes(ApplicationContext context) {
        RequestMappingHandlerMapping mapping = context.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        List<String> routes = new ArrayList<>();
        mapping.getHandlerMethods().forEach((info, handler) -> {
            if (!handler.getBeanType().getPackageName().equals(RouteInventory.class.getPackageName())) {
                return;
            }
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            for (String pattern : info.getPatternValues()) {
                if (methods.isEmpty()) {
                    routes.add("ANY " + pattern);
                }
                for (RequestMethod method : methods) {
                    routes.add(method.name() + " " + pattern);
                }
            }
        });
        return routes.stream().sorted().toList();
    }

    static List<String> withoutRule(ApplicationContext context) {
        return routes(context).stream().filter(r -> {
            int space = r.indexOf(' ');
            return !AccessRules.covers(r.substring(0, space), r.substring(space + 1));
        }).toList();
    }

    private RouteInventory() {
    }
}
