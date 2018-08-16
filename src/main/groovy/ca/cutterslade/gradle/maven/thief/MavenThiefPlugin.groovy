package ca.cutterslade.gradle.maven.thief

import ca.cutterslade.gradle.maven.thief.PomDependency.Scope
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

class MavenThiefPlugin implements Plugin<Project> {
  @Override
  void apply(final Project project) {
    project.apply plugin: 'java'
    project.apply plugin: 'provided-base'

    def pomHandler = PomHandler.of(project.file('pom.xml'))

    final Set<Scope> scopes = [Scope.COMPILE, Scope.RUNTIME, Scope.TEST]

    final Map<Scope, Set<PomDependency>> dependenciesToBeExcluded = new HashMap<>()
    for(final scope in scopes) {
      dependenciesToBeExcluded.put(scope, new HashSet<>())
    }

    pomHandler.dependencies.values().each { PomDependency dep ->
      dep.exclusions.each {
        final Scope scope = it.scope ?: Scope.COMPILE

        if (scope != Scope.PROVIDED) {
          dependenciesToBeExcluded.get(scope).add(it.asKey())
        }
      }
    }

    pomHandler.dependencies.values().each { PomDependency dep ->
      dep.getGradleConfiguration(project).dependencies.add(dep.getGradleDependency(project))

      final Scope scope = dep.scope ?: Scope.COMPILE

      if (scope != Scope.PROVIDED && dependenciesToBeExcluded.get(scope).remove(dep.asKey())) {
          project.logger.warn("MavenThiefPlugin: ${scope.gradleConfiguration} dependency " +
              "${dep.groupId}:${dep.artifactId}:${dep.classifier} from ${project.name} is both excluded and included " +
              "in Maven pom")
      }
    }

    for (final scope in scopes) {
      final Configuration configuration = project.configurations.getByName(scope.gradleConfiguration)

      dependenciesToBeExcluded.get(scope).each {
        project.logger.info("MavenThiefPlugin: excluding ${scope.gradleConfiguration} dependency " +
                "${it.groupId}:${it.artifactId}:${it.classifier} from ${project.name}")
        configuration.exclude([group: it.groupId, module: it.artifactId])
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
}
