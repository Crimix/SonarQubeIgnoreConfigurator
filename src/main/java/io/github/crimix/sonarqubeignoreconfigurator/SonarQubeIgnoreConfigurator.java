package io.github.crimix.sonarqubeignoreconfigurator;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SonarQubeIgnoreConfigurator implements Plugin<Project> {

    private final AnnotationScanner scanner = new AnnotationScanner();
    private final SonarPropertyUpdater updater = new SonarPropertyUpdater();

    @Override
    public void apply(Project rootProject) {
        rootProject.afterEvaluate(this::configureProjectTask);
    }

    private void configureProjectTask(Project rootProject) {
        // Register preparation task
        Task prepTask = rootProject.getTasks().create("prepareSonarExclusions", task -> {
            task.setGroup("verification");
            task.setDescription("Scans annotations and sets Sonar exclusions");
            task.doLast(t -> processProjects(rootProject));
        });

        // Make sure that the project has been built first
        rootProject.getTasks().matching(t -> t.getName().equals("assemble")).configureEach(prepTask::dependsOn);

        // Hook into sonarqube task if available
        rootProject.getTasks().matching(t -> t.getName().equals("sonarqube")).configureEach(task -> task.dependsOn(prepTask));
        rootProject.getTasks().matching(t -> t.getName().equals("sonar")).configureEach(task -> task.dependsOn(prepTask));
    }

    private void processProjects(Project rootProject) {
        for (SonarRule rule : SonarRule.values()) {
            List<String> matched = new ArrayList<>();
            for (Project project : rootProject.getAllprojects()) {
                matched.addAll(findExcludedFiles(project, rule));
            }

            if (!matched.isEmpty()) {
                updater.updateProperties(rootProject, rule, matched);
                rootProject.getLogger().lifecycle("[SonarConfigurator] [{}] Excluded {} files", rule, matched.size());
            }
        }
    }

    private List<String> findExcludedFiles(Project project, SonarRule rule) {
        project.getLogger().info("[SonarConfigurator] [{}:{}] Starting to find exclusions", project.getName(), rule);
        List<File> dirs = new ArrayList<>();
        File classesDir = new File(project.getBuildDir(), "classes/java/main");
        if (classesDir.exists()) {
            dirs.add(classesDir);
        }

        File testsDir = new File(project.getBuildDir(), "classes/java/test");
        if (testsDir.exists()) {
            dirs.add(testsDir);
        }

        if (dirs.isEmpty()) return new ArrayList<>();

        project.getLogger().info("[SonarConfigurator] [{}:{}] Founds dirs to check {}", project.getName(), rule, dirs.stream().map(File::getName).collect(Collectors.joining(", ")));

        List<String> annotations = getAnnotationsFor(project, rule);
        if (annotations.isEmpty()) return new ArrayList<>();

        List<String> matched = new ArrayList<>();
        for (File dir : dirs) {
            matched.addAll(scanner.scan(project, dir, annotations));
        }

        if (!matched.isEmpty()) {
            project.getLogger().lifecycle("[SonarConfigurator] [{}:{}] Found {} files to exclude", project.getName(), rule, matched.size());
        }

        return matched;
    }

    private List<String> getAnnotationsFor(Project project, SonarRule sonarRule) {
        Object prop = project.findProperty(sonarRule.getAnnotationProperty());
        if (prop == null) return Collections.emptyList();

        return Arrays.stream(prop.toString().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}