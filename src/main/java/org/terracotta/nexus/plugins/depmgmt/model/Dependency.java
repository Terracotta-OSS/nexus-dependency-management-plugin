package org.terracotta.nexus.plugins.depmgmt.model;

import org.sonatype.aether.artifact.Artifact;

/**
 * @author Ludovic Orban
 */
public class Dependency {

  private String groupId;
  private String artifactId;
  private String version;
  private boolean snapshot;
  private boolean terracottaMaintained;
  private Dependency[] dependencies;
  private String latestVersion;

  public Dependency() {
  }

  public Dependency(Artifact artifact) {
    this.groupId = artifact.getGroupId();
    this.artifactId = artifact.getArtifactId();
    this.version = artifact.getVersion();
    this.snapshot = artifact.isSnapshot();
    // TODO: terracottaMaintained should be true if the artifact's repository is of type "hosted" and contains "terracotta" in its ID.
    this.terracottaMaintained = groupId.contains("terracotta");
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

  public boolean isSnapshot() {
    return snapshot;
  }

  public void setSnapshot(boolean snapshot) {
    this.snapshot = snapshot;
  }

  public boolean isTerracottaMaintained() {
    return terracottaMaintained;
  }

  public void setTerracottaMaintained(boolean terracottaMaintained) {
    this.terracottaMaintained = terracottaMaintained;
  }

  public Dependency[] getDependencies() {
    return dependencies;
  }

  public void setDependencies(Dependency[] dependencies) {
    this.dependencies = dependencies;
  }

  public void setLatestVersion(String newVersion) {
    this.latestVersion = newVersion;
  }

  public String getLatestVersion() {
    return latestVersion;
  }
}
