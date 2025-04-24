package io.github.crimix.sonarqubeignoreconfigurator;

import org.gradle.api.Project;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SonarPropertyUpdater {

    public void updateProperties(Project rootProject, SonarRule sonarRule, List<String> matched) {
        List<String> existing = resolvePropertyList(rootProject, sonarRule);
        List<String> merged = mergeAndDeduplicate(existing, matched);
        String mergedValue = String.join(",", merged);

        System.setProperty(sonarRule.getSonarProperty(), mergedValue);
        rootProject.getLogger().info("[SonarPropertyUpdater] Updated property {} with {}", sonarRule.getSonarProperty(), mergedValue);
    }

    private List<String> resolvePropertyList(Project project, SonarRule rule) {
        String key = rule.getSonarProperty();

        // Prefer system/project properties
        Object sysProp = project.findProperty(key);
        if (sysProp != null) {
            return toList(sysProp.toString());
        }

        // Fallback to SonarQube extension
        return getSonarExtensionProperty(project, rule);
    }

    private List<String> toList(String csv) {
        if (csv == null || csv.isBlank()) return new ArrayList<>();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> mergeAndDeduplicate(List<String> existing, List<String> incoming) {
        Set<String> merged = new LinkedHashSet<>();
        merged.addAll(existing);
        merged.addAll(incoming);
        return new ArrayList<>(merged);
    }

    private List<String> getSonarExtensionProperty(Project project, SonarRule sonarRule) {
        Object sonarExtension = project.getExtensions().findByName("sonar");
        if (sonarExtension == null) return new ArrayList<>();

        try {
            Field actionsField = sonarExtension.getClass().getSuperclass().getDeclaredField("propertiesActions");
            actionsField.setAccessible(true);

            Object action = actionsField.get(sonarExtension);

            Map<String, Object> properties = new HashMap<>();

            ClassLoader loader = sonarExtension.getClass().getClassLoader();
            Class<?> sonarPropsClass = loader.loadClass("org.sonarqube.gradle.SonarProperties");
            Constructor<?> constructor = sonarPropsClass.getConstructor(Map.class);
            Object sonarProps = constructor.newInstance(properties);

            Method executeMethod = action.getClass().getMethod("execute", Object.class);
            executeMethod.invoke(action, sonarProps);

            if (properties.containsKey(sonarRule.getSonarProperty())) {
                return toList(properties.get(sonarRule.getSonarProperty()).toString());
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            project.getLogger().error("[SonarPropertyUpdater] Failed to reflectively access SonarQube extension", e);
        }

        return Collections.emptyList();
    }
}
