package org.terracotta.nexus.plugins.depmgmt.model;

import org.sonatype.aether.artifact.Artifact;

import java.io.Serializable;

/**
 * @author Ludovic Orban
 */
public class DependencyInformation implements Serializable{

  private String groupId;
  private String artifactId;
  private String version;
  private boolean snapshot;
  private boolean terracottaMaintained;
  private DependencyInformation[] dependencies;
  private String latestReleaseVersion;
  private String latestSnapshotVersion;
  private String scope;

  public DependencyInformation() {
  }

  public DependencyInformation(Artifact artifact) {
    this.groupId = artifact.getGroupId();
    this.artifactId = artifact.getArtifactId();
    this.version = artifact.getVersion();
    this.snapshot = artifact.isSnapshot();
    // TODO: terracottaMaintained should be true if the artifact's repository is of type "hosted"
    // and contains "terracotta", "ehcache" or "quartz" in its ID.
    this.terracottaMaintained = groupId.contains("terracotta") || groupId.contains("ehcache") || groupId.contains("quartz");
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

  public DependencyInformation[] getDependencies() {
    return dependencies;
  }

  public void setDependencies(DependencyInformation[] dependencies) {
    this.dependencies = dependencies;
  }

  public void setLatestReleaseVersion(String newVersion) {
    this.latestReleaseVersion = newVersion;
  }

  public String getLatestReleaseVersion() {
    return latestReleaseVersion;
  }

  public String getLatestSnapshotVersion() {
    return latestSnapshotVersion;
  }

  public void setLatestSnapshotVersion(String latestSnapshotVersion) {
    this.latestSnapshotVersion = latestSnapshotVersion;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public String getScope() {
    return scope;
  }
}
