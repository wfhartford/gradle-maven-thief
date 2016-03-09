package ca.cutterslade.gradle.maven.thief

import org.gradle.api.Plugin
import org.gradle.api.Project

class MavenThiefPlugin implements Plugin<Project> {
  @Override
  void apply(final Project project) {
    project.apply plugin: 'java'
    project.apply plugin: 'provided-base'

    def pomHandler = new PomHandler(project, project.file('pom.xml'))
    pomHandler.dependencies.values().each {PomDependency dep ->
      dep.getGradleConfiguration(project).dependencies.add(dep.getGradleDependency(project))
    }
    project.group = pomHandler.groupId
    project.version = pomHandler.version
    project.sourceSets.main.java.srcDirs = [pomHandler.mainSourceDirectory]
    project.sourceSets.main.resources.srcDirs = [pomHandler.mainResourceDirectory]
    project.sourceSets.test.java.srcDirs = [pomHandler.testSourceDirectory]
    project.sourceSets.test.resources.srcDirs = [pomHandler.testResourceDirectory]
  }
}
