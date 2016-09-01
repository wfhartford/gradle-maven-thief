package ca.cutterslade.gradle.maven.thief

import groovy.transform.Immutable
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency

@Immutable
class PomDependency {
  enum Scope {
    COMPILE('compile'), RUNTIME('runtime'), TEST('testCompile'), PROVIDED('provided')
    final String gradleConfiguration

    Scope(String gradleConfiguration) {
      this.gradleConfiguration = gradleConfiguration
    }

    static Scope of(String value) {
      value ? valueOf(value.toUpperCase()) : null
    }
  }
  String groupId
  String artifactId
  String version
  String classifier
  String type
  Scope scope

  Set<PomDependency> exclusions

  private PomDependency keyDep

  PomDependency withManagement(Map<PomDependency, PomDependency> dependencyManagement) {
    def depMan = dependencyManagement[asKey()]
    null == depMan || (depMan.version == version && depMan.scope == scope) ? this :
        new PomDependency(groupId: groupId, artifactId: artifactId,
            version: version ?: depMan.version, type: type?: depMan.type, classifier: classifier, scope: scope ?: depMan.scope,
            exclusions: depMan.exclusions ? depMan.exclusions + exclusions : exclusions)
  }

  PomDependency asKey() {
    if (!keyDep) {
      keyDep = null == version && null == scope && null == exclusions ? this :
          new PomDependency(groupId: groupId, artifactId: artifactId, classifier: classifier)
    }
    return keyDep
  }

  Dependency getGradleDependency(Project project) {
    if ("jar".equals(type)) {
      String groovyDep = "$groupId:$artifactId:$version${classifier ? ":$classifier" : ''}"
      project.dependencies.create(groovyDep, {
        exclusions.each { exclude(group: it.groupId, module: it.artifactId) }
      })
    }
    else {
      String groovyDep = "$groupId:$artifactId:$version${classifier ? ":$classifier" : ''}@$type"
      project.dependencies.create(groovyDep, {
        exclusions.each { exclude(group: it.groupId, module: it.artifactId) }
      })
    }
  }

  Configuration getGradleConfiguration(Project project) {
    project.configurations.getByName((scope ?: Scope.COMPILE).gradleConfiguration)
  }
}
