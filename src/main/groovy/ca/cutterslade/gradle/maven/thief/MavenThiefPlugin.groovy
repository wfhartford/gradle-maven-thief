package ca.cutterslade.gradle.maven.thief

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

class MavenThiefPlugin implements Plugin<Project> {
  @Override
  void apply(final Project project) {
    project.apply plugin: 'java'
    project.apply plugin: 'provided-base'

    def pomHandler = PomHandler.of(project.file('pom.xml'))

    final Set<PomDependency> dependenciesToBeExcluded = []

    pomHandler.dependencies.values().each { PomDependency dep ->
      dep.exclusions.each {
        if (it.scope == null || 'compile' == it.scope.gradleConfiguration) {
          dependenciesToBeExcluded.add(it.asKey())
        }
      }
    }

    pomHandler.dependencies.values().each { PomDependency dep ->
      dep.getGradleConfiguration(project).dependencies.add(dep.getGradleDependency(project))
      if (dep.scope == null || 'compile' == dep.scope.gradleConfiguration) {
        dependenciesToBeExcluded.remove(dep.asKey())
      }
    }

    final Configuration compileConfiguration = project.configurations.getByName('compile')

    dependenciesToBeExcluded.each {
      project.logger.info(
          "MavenThiefPlugin: excluding ${it.groupId}:${it.artifactId}:${it.classifier} from ${project.name}")
      compileConfiguration.exclude([group: it.groupId, module: it.artifactId])
    }

    project.group = pomHandler.groupId
    project.version = pomHandler.version
    boolean dontModifySourceSets =
        project.hasProperty('maven-thief.dontModifySourceSets') &&
            Boolean.parseBoolean(project.property('maven-thief.dontModifySourceSets'))
    if (!dontModifySourceSets) {
      project.sourceSets.main.java.srcDirs = [pomHandler.mainSourceDirectory]
      project.sourceSets.main.resources.srcDirs = [pomHandler.mainResourceDirectory]
      project.sourceSets.test.java.srcDirs = [pomHandler.testSourceDirectory]
      project.sourceSets.test.resources.srcDirs = [pomHandler.testResourceDirectory]
    }

    project.configurations.each {
      it.resolutionStrategy {
        dependencySubstitution {
          project.rootProject.getSubprojects().each { subProject ->
            def pomFile = subProject.file('pom.xml')
            if (pomFile.file) {
              def handler = PomHandler.of(pomFile)
              substitute module("$handler.groupId:$subProject.name") with delegate.project(subProject.path)
            }
          }
        }
      }
    }
  }
}
