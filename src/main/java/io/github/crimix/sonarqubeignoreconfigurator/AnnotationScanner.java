package io.github.crimix.sonarqubeignoreconfigurator;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AnnotationScanner {

    public List<String> scan(Project project, File classesDir, List<String> annotations) {
        if (!classesDir.exists()) return Collections.emptyList();

        try (ScanResult scan = new ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .overrideClasspath(classesDir)
                .scan()) {

            Set<String> matched = new HashSet<>();

            for (String annotation : annotations) {
                List<ClassInfo> classInfos = scan.getClassesWithAnnotation(annotation);
                matched.addAll(classInfos.stream()
                        .map(ci -> ci.getResource().getPath())
                        .map(path -> toSourcePath(project, classesDir, path))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()));
            }

            return new ArrayList<>(matched);
        }
    }

    private String toSourcePath(Project project, File classesDir, String classFilePath) {
        String normalizedPath = Paths.get(classesDir.getPath(), classFilePath).toAbsolutePath().toString().replace(File.separatorChar, '/');

        Pattern pattern = Pattern.compile(".*/build/classes/(java|kotlin)/(main|test)/(.+?)\\.class$");
        Matcher matcher = pattern.matcher(normalizedPath);

        if (!matcher.find()) {
            return null; // Skip if it doesn't match expected structure
        }

        String lang = matcher.group(1);     // java or kotlin
        String scope = matcher.group(2);    // main or test
        String relativeClassPath = matcher.group(3); // e.g., com/example/MyClass

        String extension = lang.equals("kotlin") ? ".kt" : ".java";
        String sourcePath = "src/" + scope + "/" + lang + "/" + relativeClassPath + extension;

        if (!project.equals(project.getRootProject())) {
            sourcePath = project.getProjectDir().getName() + "/" + sourcePath;
        }

        return sourcePath;
    }
}
