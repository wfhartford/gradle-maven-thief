package ca.cutterslade.gradle.maven.thief

import groovy.util.slurpersupport.GPathResult

import java.util.regex.Matcher
import java.util.regex.Pattern

class PomHandler {
  private static final Pattern PROPERTY_SEARCH = Pattern.compile('\\$\\{(.+)}')
  private static final Map<File, PomHandler> INSTANCES = [:]
  private final File pomFile
  private GPathResult rootNode
  private PomHandler parentPom
  private Map<String, String> pomProperties
  private Map<PomDependency, PomDependency> pomDependencyManagement
  private Map<PomDependency, PomDependency> pomDependencies

  static PomHandler of(File pomFile) {
    def handler = INSTANCES.get(pomFile)
    if (!handler) {
      handler = new PomHandler(pomFile)
      INSTANCES.put(pomFile, handler)
    }
    return handler
  }

  private PomHandler(File pomFile) {
    this.pomFile = pomFile
    rootNode = new XmlSlurper().parse(pomFile)
    def parentNode = rootNode.parent
    if (parentNode) {
      def relativePathNode = parentNode.relativePath
      if (relativePathNode && relativePathNode.text()) {
        def parentPomFile = new File(pomFile.parentFile, relativePathNode.text())
        parentPom = of(parentPomFile)
      }
    }
  }

  String getGroupId() {
    return rootNode.groupId.text() ?: parentPom.groupId
  }

  String getVersion() {
    return rootNode.version.text() ?: parentPom.version
  }

  String getParentChain() {
    return parentPom ? "$pomFile -> $parentPom.parentChain" : pomFile
  }

  Closure<Map<String, String>> fixProperties = {Map<String, String> input ->
    (Map<String, String>) input.collectEntries {key, value -> [(key): resolveProperties(value, input)]}
  }

  String resolveProperties(String value) {
    resolveProperties(value, getPomProperties())
  }

  static String resolveProperties(String value, Map<String, String> properties) {
    Matcher matcher = PROPERTY_SEARCH.matcher(value)
    StringBuffer buffer = new StringBuffer()
    while (matcher.find()) {
      def replace = properties.get(matcher.group(1))
      if (replace) {
        matcher.appendReplacement(buffer, Matcher.quoteReplacement(replace))
      }
      else {
        matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group()))
      }
    }
    matcher.appendTail(buffer).toString()
  }

  Map<String, String> getPomProperties() {
    if (!pomProperties) {
      Map<String, String> properties = parentPom ? parentPom.pomProperties : [:]
      if (parentPom) {
        properties.putAll(parentPom.getPomProperties())
      }
      properties.put('project.artifactId', rootNode.artifactId.text())
      properties.put('project.version', version)
      rootNode.properties.children().each {properties.put(it.name(), it.text())}
      Map<String, String> fixedProperties = fixProperties(properties)
      while (fixedProperties != properties) {
        properties = fixedProperties
        fixedProperties = fixProperties(properties)
      }
      pomProperties = properties
    }
    pomProperties
  }

  Map<PomDependency, PomDependency> getDependencyManagement() {
    if (!pomDependencyManagement) {
      Map<PomDependency, PomDependency> dependencies = parentPom ? parentPom.dependencyManagement : [:]
      GPathResult dependenciesNode = rootNode.dependencyManagement.dependencies
      pomDependencyManagement = parseDependencies(dependenciesNode, dependencies, dependencies)
    }
    return pomDependencyManagement
  }

  Map<PomDependency, PomDependency> getDependencies() {
    if (!pomDependencies) {
      Map<PomDependency, PomDependency> dependencies = parentPom ? parentPom.dependencies : [:]
      GPathResult dependenciesNode = rootNode.dependencies
      pomDependencies = parseDependencies(dependenciesNode, dependencies, getDependencyManagement())
    }
    return pomDependencies
  }

  Map<PomDependency, PomDependency> parseDependencies(final GPathResult dependenciesNode,
      final Map<PomDependency, PomDependency> dependencies,
      final Map<PomDependency, PomDependency> dependencyManagement = [:]) {
    dependencies.putAll(dependenciesNode.children().collectEntries {GPathResult dependency ->
      String groupId = dependency.groupId.text()
      String artifactId = dependency.artifactId.text()
      String classifier = dependency.classifier ? dependency.classifier.text() : null
      String version = dependency.version ? dependency.version.text() : null
      String scope = dependency.scope ? dependency.scope.text() : null
      def dep = new PomDependency(
          groupId: resolveProperties(groupId),
          artifactId: resolveProperties(artifactId),
          version: resolveProperties(version),
          classifier: resolveProperties(classifier),
          scope: PomDependency.Scope.of(resolveProperties(scope)),
          exclusions: parseExclusions(dependency))
      [(dep.asKey()): dep.withManagement(dependencyManagement)]
    })
    dependencies
  }

  Set<PomDependency> parseExclusions(final GPathResult dependencyNode) {
    dependencyNode.exclusions ? dependencyNode.exclusions.children().collect {GPathResult exclusion ->
      new PomDependency(groupId: resolveProperties(exclusion.groupId.text()),
          artifactId: resolveProperties(exclusion.artifactId.text()),
          classifier: exclusion.classifier ? resolveProperties(exclusion.classifier.text()) : null)
    } : []
  }

  String getMainSourceDirectory() {
    resolveProperties(rootNode.build.sourceDirectory.text()) ?: 'src/main/java'
  }

  String getTestSourceDirectory() {
    resolveProperties(rootNode.build.testSourceDirectory.text()) ?: 'src/test/java'
  }

  String getMainResourceDirectory() {
    resolveProperties(rootNode.build.resources.resource.directory.text()) ?: 'src/main/resources'
  }

  String getTestResourceDirectory() {
    resolveProperties(rootNode.build.testResources.testResource.directory.text()) ?: 'src/test/resources'
  }
}
