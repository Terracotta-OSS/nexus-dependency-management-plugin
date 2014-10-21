package org.terracotta.nexus.plugins.depmgmt.model;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Ludovic Orban
 */
@XStreamAlias(value = "artifactInformation")
@XmlRootElement(name = "artifactInformation")
@XmlAccessorType(XmlAccessType.FIELD)
public class ArtifactInformation implements Serializable {

  private String error;

  private String repositoryName;
  private String buildUrl;
  private String svnRevision;
  private String buildProfiles;
  private DependencyInformation artifact;
  private String parent;
  private String parentHighestReleaseVersion;
  private String parentHighestSnapshotVersion;

  public ArtifactInformation() {
  }

  public ArtifactInformation(String error) {
    this.error = error;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public String getBuildUrl() {
    return buildUrl;
  }

  public void setBuildUrl(String buildUrl) {
    this.buildUrl = buildUrl;
  }

  public String getSvnRevision() {
    return svnRevision;
  }

  public void setSvnRevision(String svnRevision) {
    this.svnRevision = svnRevision;
  }

  public String getBuildProfiles() {
    return buildProfiles;
  }

  public void setBuildProfiles(String buildProfiles) {
    this.buildProfiles = buildProfiles;
  }

  public DependencyInformation getArtifact() {
    return artifact;
  }

  public void setArtifact(DependencyInformation artifact) {
    this.artifact = artifact;
  }

  public String getParent() {
    return parent;
  }

  public void setParent(String parent) {
    this.parent = parent;
  }

  public void setParentHighestReleaseVersion(String parentHighestReleaseVersion) {
    this.parentHighestReleaseVersion = parentHighestReleaseVersion;
  }

  public String getParentHighestReleaseVersion() {
    return parentHighestReleaseVersion;
  }

  public void setParentHighestSnapshotVersion(String parentHighestSnapshotVersion) {
    this.parentHighestSnapshotVersion = parentHighestSnapshotVersion;
  }

  public String getParentHighestSnapshotVersion() {
    return parentHighestSnapshotVersion;
  }
}
