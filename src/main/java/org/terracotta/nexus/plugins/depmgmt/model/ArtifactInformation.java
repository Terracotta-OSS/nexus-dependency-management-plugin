package org.terracotta.nexus.plugins.depmgmt.model;

/**
 * @author Ludovic Orban
 */
public class ArtifactInformation {

  private String error;

  private String repositoryName;
  private String buildUrl;
  private String svnRevision;
  private String buildProfiles;

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
}
