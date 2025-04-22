# SonarQube Configurator Plugin
A Gradle plugin to configure SonarQube code coverage and duplicated code exclusions from annotations

## Installation
Recommended way is to apply the plugin to the root `build.gradle` in the `plugins` block
```groovy
plugins {
    id 'io.github.crimix.sonarqube-ignore-configurator' version 'VERSION'
}
```

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
