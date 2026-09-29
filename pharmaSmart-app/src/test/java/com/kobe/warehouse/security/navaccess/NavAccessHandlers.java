package com.kobe.warehouse.security.navaccess;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Handlers des {@code @RestController} de l'application, avec leur règle d'accès. */
final class NavAccessHandlers {

    record Handler(Class<?> type, Method method, List<String> paths, NavAccessRules.Rule rule) {
        String name() {
            return type.getSimpleName() + '#' + method.getName();
        }

        /** Hors de {@code /api/**}, ou sous {@code /api/admin/**} : couvert par la chaîne de filtres. */
        boolean coveredByFilterChain() {
            return paths.stream().allMatch(p -> !p.startsWith("/api/") || p.startsWith("/api/admin/"));
        }
    }

    private NavAccessHandlers() {}

    static List<Handler> scan() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<Handler> handlers = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents("com.kobe.warehouse")) {
            Class<?> type = ClassUtils.resolveClassName(definition.getBeanClassName(), NavAccessHandlers.class.getClassLoader());
            List<String> bases = pathsOf(AnnotatedElementUtils.findMergedAnnotation(type, RequestMapping.class));
            for (Method method : type.getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null) {
                    continue;
                }
                List<String> paths = new ArrayList<>();
                for (String base : bases) {
                    for (String path : pathsOf(mapping)) {
                        paths.add(join(base, path));
                    }
                }
                handlers.add(new Handler(type, method, paths, NavAccessRules.resolve(method, type)));
            }
        }
        return handlers;
    }

    private static List<String> pathsOf(RequestMapping mapping) {
        if (mapping == null || mapping.path().length == 0) {
            return List.of("");
        }
        return Arrays.asList(mapping.path());
    }

    private static String join(String base, String path) {
        String joined = (base + "/" + path).replaceAll("/+", "/");
        return joined.length() > 1 && joined.endsWith("/") ? joined.substring(0, joined.length() - 1) : joined;
    }
}
