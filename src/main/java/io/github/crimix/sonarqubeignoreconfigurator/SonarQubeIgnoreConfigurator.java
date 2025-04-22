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
        rootProject.getAllprojects().forEach(project -> {
            // Register preparation task
            Task prepTask = project.getTasks().create("prepareSonarExclusions", task -> {
                task.setGroup("verification");
                task.setDescription("Scans annotations and sets Sonar exclusions");
                task.doLast(t -> processProject(rootProject, project));
            });

            // Hook into sonarqube task if available
            project.getTasks().matching(t -> t.getName().equals("sonarqube")).configureEach(task -> task.dependsOn(prepTask));
            project.getTasks().matching(t -> t.getName().equals("sonar")).configureEach(task -> task.dependsOn(prepTask));
        });
    }

    private void processProject(Project rootProject, Project project) {
        List<File> dirs = new ArrayList<>();
        File classesDir = new File(project.getBuildDir(), "classes/java/main");
        if (classesDir.exists()) {
            dirs.add(classesDir);
        }

        File testsDir = new File(project.getBuildDir(), "classes/java/test");
        if (testsDir.exists()) {
            dirs.add(testsDir);
        }

        if (dirs.isEmpty()) return;

        for (SonarRule rule : SonarRule.values()) {
            List<String> annotations = getAnnotationsFor(project, rule);
            if (annotations.isEmpty()) continue;


            List<String> matched = new ArrayList<>();
            for (File dir : dirs) {
                matched.addAll(scanner.scan(project, dir, annotations));
            }

            if (!matched.isEmpty()) {
                updater.updateProperties(rootProject, rule, matched);
                project.getLogger().lifecycle("[SonarConfigurator] [{}:{}] Excluded {} files", project.getName(), rule, matched.size());
            }
        }
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