# SonarQube Configurator Plugin
A Gradle plugin to configure SonarQube code coverage and duplicated code exclusions from annotations

## Installation
Recommended way is to apply the plugin to the root `build.gradle` in the `plugins` block
```groovy
plugins {
    id 'io.github.crimix.sonarqube-ignore-configurator' version 'VERSION'
}
```
Then it can be configured using the following three options in `gradle.properties`

| **Property**                         | **Description**                                       |
|--------------------------------------|-------------------------------------------------------|
| sonar.ignore.coverage.annotations    | Autoconfigures the sonar.coverage.exclusions property |
| sonar.ignore.duplication.annotations | Autoconfigures the sonar.cpd.exclusions property      |
| sonar.ignore.issue.annotations       | Autoconfigures the sonar.issue.exclusions property    |

Each property takes a fully qualified annotation class (com.mypackage.MyAnnotation)
Then it takes the default configured Sonar property and adds the dynamic parts to it from the classes found with the annotations

## Local Development
To develop and test locally with your own projects there are a few changes needed to be made.
 1. First add the following to `settings.gradle` file in the root project
  ```groovy
  pluginManagement {
    repositories {
      mavenLocal()
      gradlePluginPortal()
    }
  }
  buildscript {
    configurations.all {
      resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
    }
  }
  ```
 2. Change the version inside of `build.gradle` in SonarQubeConfigurator to be `x.y-SNAPSHOT`
 3. Change the plugin version used in your project to be `x.y-SNAPSHOT`
 4. Do a refresh / sync of Gradle

## Local debugging
To debug the plugin on your project, first checkout the repo and set breakpoints.
Then you start the project task as the following in your project
`./gradlew prepareSonarExclusions -Dorg.gradle.debug=true --no-daemon`

Then the Gradle task will wait for you to connect a debugger, this can be done using the `GradleRemoteDebug` run configuration from this repo.
