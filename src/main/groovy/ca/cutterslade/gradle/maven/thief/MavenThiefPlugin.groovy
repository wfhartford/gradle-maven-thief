package ca.cutterslade.gradle.maven.thief

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MavenThiefPlugin implements Plugin<Project> {
  @Override
  void apply(final Project project) {
    final Logger slf4jLogger = LoggerFactory.getLogger('gradle-maven-thief')
    project.apply plugin: 'java'
    project.apply plugin: 'provided-base'

    def pomHandler = PomHandler.of(project.file('pom.xml'))

    final Set<String> inclusions = []

    pomHandler.dependencies.values().each { PomDependency dep ->
      dep.getGradleConfiguration(project).dependencies.add(dep.getGradleDependency(project))
      inclusions.add(groupArtifact(dep))
    }

    pomHandler.dependencies.values().each { PomDependency dep ->
      project.configurations.each { configuration ->
        dep.exclusions.each {
          final String groupArtifact = groupArtifact(it)
          if (!inclusions.contains(groupArtifact)) {
            if (it.scope == null || 'compile' == it.scope.gradleConfiguration) {
              slf4jLogger.info("MavenThiefPlugin: excluding ${it.groupId}:${it.artifactId} from ${project.name}")
              configuration.exclude([group: it.groupId, module: it.artifactId])
            }
          }
        }
      }
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

  private String groupArtifact(PomDependency dep) {
    dep.groupId + ":" + dep.artifactId
  }
}
