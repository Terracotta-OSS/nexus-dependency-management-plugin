Nexus Dependency Management Plugin
==================================

The Nexus Dependency Management Plugin allows developers, QAs, devops, etc. to get extended information about the artifacts hosted in their Nexus instance.

## How to build :
Simply clone this repo and run mvn clean install

## How to install :
Unzip the archive in target named nexus-dependency-management-plugin-1.1.0-SNAPSHOT-bundle.zip NEXUSHOME/nexus/WEB-INF/plugin-repository and restart nexus.

## How to use :
Simply search for an artifact and select the "Dependency Management" tab :
![Screenshot](https://raw.github.com/Terracotta-OSS/nexus-dependency-management-plugin/gh-pages/screenshots/dependencies.png "Dependencies")

If you are using the properties-metadata-maven-plugin while releasing your maven artifacts, you can also enjoy some cool metadata :
![Screenshot](https://raw.github.com/Terracotta-OSS/nexus-dependency-management-plugin/gh-pages/screenshots/metadata.png "Metadata")


## Authors :
This plugin was developed during Innovation Days by Terracotta, by

- [Ludovic Orban](https://github.com/lorban/)
- [Louis Jacomet](https://github.com/ljacomet/)
- [Anthony Dahanne](https://github.com/anthonydahanne/)