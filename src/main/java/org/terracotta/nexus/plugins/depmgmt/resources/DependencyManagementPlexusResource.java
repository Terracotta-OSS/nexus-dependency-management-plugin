package org.terracotta.nexus.plugins.depmgmt.resources;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.restlet.data.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.VersionRangeRequest;
import org.sonatype.aether.resolution.VersionRangeResolutionException;
import org.sonatype.aether.resolution.VersionRangeResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.version.Version;
import org.sonatype.nexus.plugins.mavenbridge.NexusAether;
import org.sonatype.nexus.plugins.mavenbridge.NexusMavenBridge;
import org.sonatype.nexus.plugins.mavenbridge.Utils;
import org.sonatype.nexus.plugins.mavenbridge.internal.FileItemModelSource;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.ArtifactStoreRequest;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.AbstractArtifactViewProvider;
import org.sonatype.nexus.rest.ArtifactViewProvider;
import org.terracotta.nexus.plugins.depmgmt.model.ArtifactInformation;
import org.terracotta.nexus.plugins.depmgmt.model.Dependency;
import org.terracotta.nexus.plugins.depmgmt.utils.ExceptionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * @author Ludovic Orban
 */
@Component(role = ArtifactViewProvider.class, hint = "depmgmt")
public class DependencyManagementPlexusResource extends AbstractArtifactViewProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(DependencyManagementPlexusResource.class);

  @Requirement
  private RepositoryRegistry repositoryRegistry;

  @Requirement
  private NexusMavenBridge nexusMavenBridge;

  @Requirement
  private NexusAether nexusAether;

  @Override
  protected Object retrieveView(ResourceStoreRequest request, RepositoryItemUid itemUid, StorageItem item, Request req) throws IOException {
    try {
      MavenRepository itemRepository = itemUid.getRepository().adaptToFacet(MavenRepository.class);
      Gav gav = itemRepository.getGavCalculator().pathToGav(itemUid.getPath());
      StorageFileItem pom = itemRepository.getArtifactStoreHelper().retrieveArtifactPom(new ArtifactStoreRequest(itemRepository, gav, true));

      DependencyNode dependencyNode = nexusMavenBridge.collectDependencies(Utils.createDependencyFromGav(gav, "compile"), repositories());
      Dependency rootDep = buildDependencies(dependencyNode, gav.isSnapshot());

      /*
      WTF? I get:
      java.lang.LinkageError: loader constraint violation: loader (instance of org/codehaus/plexus/classworlds/realm/ClassRealm) previously initiated loading for a different type with name "org/apache/maven/model/Model"
      when I try to make the call without using reflection (ie: when I declare class Model):
      //Properties properties = model.getProperties();
       */
      Object model = nexusMavenBridge.buildModel(new FileItemModelSource(pom), repositories());
      Properties properties = (Properties)model.getClass().getMethod("getProperties").invoke(model);
      Object parent = model.getClass().getMethod("getParent").invoke(model);
      ArtifactInformation artifactInformation = new ArtifactInformation();
      // if we have a parent, we get some info about it and stick into the artifactInformation
      if (parent != null) {
        String id = (String) parent.getClass().getMethod("getId").invoke(parent);
        artifactInformation.setParent(id);
        DefaultArtifact parentArtifact = new DefaultArtifact(id);
        String parentHighestReleaseVersion =  getHighestVersion(parentArtifact,true);
        String parentHighestSnapshotVersion =  getHighestVersion(parentArtifact,false);
        artifactInformation.setParentHighestReleaseVersion(parentHighestReleaseVersion);
        artifactInformation.setParentHighestSnapshotVersion(parentHighestSnapshotVersion);
      }
      artifactInformation.setRepositoryName(itemUid.getRepository().getName());
      artifactInformation.setSvnRevision(properties.getProperty("metadata.svn.revision"));
      artifactInformation.setBuildUrl(properties.getProperty("metadata.build.jenkins.url"));
      artifactInformation.setBuildProfiles(properties.getProperty("metadata.build.maven.active.profiles"));

      artifactInformation.setArtifact(rootDep);

      return artifactInformation;
    } catch (Exception e) {
      Throwable rootCause = ExceptionUtils.getRootCause(e);
      return new ArtifactInformation(rootCause.getMessage() != null ? rootCause.getMessage() : rootCause.getClass().toString());
    }
  }

  private Dependency buildDependencies(DependencyNode parentNode, boolean snapshot) {
    Dependency parent = new Dependency(parentNode.getDependency().getArtifact());
    addLatestVersionInfo(parent, parentNode.getDependency().getArtifact(), false);
    // if the artifact is not a snapshot, don't bother adding version info to its deps
    buildDependencies(parent, parentNode.getChildren(), snapshot);
    return parent;
  }

  private void buildDependencies(Dependency parent, List<DependencyNode> children, boolean addVersionInfo) {
    Collection<Dependency> result = new ArrayList<Dependency>();

    for (DependencyNode child : children) {
      Artifact artifact = child.getDependency().getArtifact();
      Dependency childDep = new Dependency(artifact);
      buildDependencies(childDep, child.getChildren(), addVersionInfo);
      // TODO : remove TC specific conditions
      if (addVersionInfo && parent.isTerracottaMaintained()) {
        addLatestVersionInfo(childDep, artifact, true);
      }
      result.add(childDep);
    }

    parent.setDependencies(result.toArray(new Dependency[] { }));
  }

  private void addLatestVersionInfo(Dependency dependency, Artifact artifact, boolean releaseOnly) {
    LOGGER.debug("Version range request for {}", artifact);
    // TODO : remove TC specific conditions
    if (!dependency.isTerracottaMaintained()) {
      return;
    }

    if (!releaseOnly) {
      String highestVersionString = getHighestVersion(artifact, false);
      dependency.setLatestReleaseVersion(highestVersionString);
    }

    String highestVersionString = getHighestVersion(artifact, true);
    dependency.setLatestReleaseVersion(highestVersionString);
  }

  /**
   *
   * @param artifact
   * @param release : true for getting highest released version, false for highest snapshot version
   * @return
   */
  private String getHighestVersion(Artifact artifact, boolean release) {
    try {
      VersionRangeRequest request = new VersionRangeRequest();
      request.setArtifact(new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getExtension(), "[,]"));
      if (release) {
        request.setRepositories(getRemoteRepositories(RepositoryPolicy.RELEASE));
      } else {
        request.setRepositories(getRemoteRepositories(RepositoryPolicy.SNAPSHOT));
      }

      VersionRangeResult versionRangeResult = nexusAether.getRepositorySystem().resolveVersionRange(nexusAether.getDefaultRepositorySystemSession(), request);
      LOGGER.debug("Version range result: {} with possible exceptions: {} for {}", versionRangeResult.getVersions(), versionRangeResult.getExceptions(), artifact);
      Version highestVersion = versionRangeResult.getHighestVersion();
      if (highestVersion != null && !artifact.getBaseVersion().equals(highestVersion.toString())) {
        LOGGER.debug("Setting latest version to {} for {}", highestVersion, artifact);
        return highestVersion.toString();
      }
    } catch (VersionRangeResolutionException e) {
      LOGGER.error("Unable to resolve version range", e);
    }
    return null;
  }

  private List<RemoteRepository> getRemoteRepositories(RepositoryPolicy policy) {
    List<RemoteRepository> remoteRepositories = new ArrayList<RemoteRepository>();
    for (MavenRepository mavenRepository : repositoryRegistry.getRepositoriesWithFacet(MavenRepository.class)) {
      if (policy.equals(mavenRepository.getRepositoryPolicy()) && mavenRepository.getRepositoryKind().isFacetAvailable(HostedRepository.class)) {
        String url = mavenRepository.getLocalUrl();
        if (!url.startsWith("http")) {
          LOGGER.debug("Adding repository {} ({})", mavenRepository.getId(), url);
          remoteRepositories.add(new RemoteRepository(mavenRepository.getId(), "default", url));
        } else {
          LOGGER.warn("Repository {} with policy {} identifies as \"Hosted\" but local URL is {}", mavenRepository.getId(), policy, url);
        }
      } else {
        LOGGER.debug("Ignoring repository {}", mavenRepository.getId());
      }
    }
    return remoteRepositories;
  }

  private List<MavenRepository> repositories() {
    List<MavenRepository> result = new ArrayList<MavenRepository>();
    List<Repository> repositories = repositoryRegistry.getRepositories();
    for (Repository repository : repositories) {
      MavenRepository mr = repository.adaptToFacet(MavenRepository.class);
      result.add(mr);
    }
    return result;
  }

}
