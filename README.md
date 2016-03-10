# gradle-maven-thief

[ ![Download](https://api.bintray.com/packages/wesley/maven/gradle-maven-thief/images/download.svg) ](https://bintray.com/wesley/maven/gradle-maven-thief/_latestVersion)
[![Build Status](https://travis-ci.org/wfhartford/gradle-maven-thief.svg?branch=master)](https://travis-ci.org/wfhartford/gradle-maven-thief)

Gradle plugin which steals project information from pom.xml files.

This plugin attempts to parse Maven pom.xml files in the project directory and apply the project settings to the gradle
build. It does not add any tasks to the project, but is executed only in the configuration phase.

## Goal
The goal of this plugin is to allow parallel development using Maven and Gradle. Why on earth would someone want to do
that? I certainly wouldn't recommend it in most cases, though it can be quite useful in a few rare situations such as:
* Demonstrating what a build might look like if it was transitioned from Maven to Gradle
* To facilitate a gradual transition from Maven to Gradle.
* To allow developers to take advantage of the superior performance and features of Gradle while still supporting some
complex build requirements implemented by Maven plugins.

## Non-Goals
This plugin does not attempt to replicate all maven configuration options, nor to configure anything to replace the
plethora of Maven plugins.

## Dependencies
This plugin parses pom.xml files for dependencies, then adds those dependencies to the appropriate configuration of the
gradle project. The parsing of pom.xml files is quite simple, but supports some
commonly used features of maven including
* Dependency Management
* Project Properties
* Parent POM files (only when the relativePath element is used)
* Dependency Exclusions

## Source Directories
This plugin parses pom.xml files for project source directories and sets the gradle source sets to match the maven POM.
If the POM file does not contain any source directory settings, the plugin uses the maven defaults, which match the
gradle defaults.
