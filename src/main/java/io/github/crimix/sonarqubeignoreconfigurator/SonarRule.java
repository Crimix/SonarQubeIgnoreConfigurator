package io.github.crimix.sonarqubeignoreconfigurator;

import java.util.StringJoiner;

public enum SonarRule {
    COVERAGE("coverage", "sonar.coverage.exclusions", "sonar.ignore.coverage.annotations"),
    ISSUE("issue", "sonar.issue.exclusions", "sonar.ignore.issue.annotations"),
    DUPLICATION("duplication", "sonar.cpd.exclusions", "sonar.ignore.duplication.annotations");

    private final String ruleName;
    private final String sonarProperty;
    private final String annotationProperty;

    SonarRule(String ruleName, String sonarProperty, String annotationProperty) {
        this.ruleName = ruleName;
        this.sonarProperty = sonarProperty;
        this.annotationProperty = annotationProperty;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getSonarProperty() {
        return sonarProperty;
    }

    public String getAnnotationProperty() {
        return annotationProperty;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SonarRule.class.getSimpleName() + "[", "]")
                .add("ruleName='" + ruleName + "'")
                .toString();
    }
}