package org.terracotta.nexus.plugins.depmgmt.model;

/**
 * @author Ludovic Orban
 */
public class ArtifactInformation {

  private String repositoryName;

  private String buildUrl;
  private String svnVersion;
  private String buildProfiles;

  public ArtifactInformation() {
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

  public String getSvnVersion() {
    return svnVersion;
  }

  public void setSvnVersion(String svnVersion) {
    this.svnVersion = svnVersion;
  }

  public String getBuildProfiles() {
    return buildProfiles;
  }

  public void setBuildProfiles(String buildProfiles) {
    this.buildProfiles = buildProfiles;
  }
}
