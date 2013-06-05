package org.terracotta.nexus.plugins.depmgmt.model;

import org.sonatype.aether.artifact.Artifact;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Ludovic Orban
 */
public class Dependency {

  private String groupId;
  private String artifactId;
  private String version;
  private Dependency[] dependencies;

  public Dependency() {
  }

  public Dependency(Artifact artifact) {
    this.groupId = artifact.getGroupId();
    this.artifactId = artifact.getArtifactId();
    this.version = artifact.getVersion();
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Dependency[] getDependencies() {
    return dependencies;
  }

  public void setDependencies(Dependency[] dependencies) {
    this.dependencies = dependencies;
  }
}
