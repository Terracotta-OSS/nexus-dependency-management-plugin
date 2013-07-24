package org.terracotta.nexus.plugins.depmgmt.resources;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.restlet.data.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.VersionRangeRequest;
import org.sonatype.aether.resolution.VersionRangeResolutionException;
import org.sonatype.aether.resolution.VersionRangeResult;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.selector.ScopeDependencySelector;
import org.sonatype.aether.version.Version;
import org.sonatype.nexus.configuration.application.GlobalRestApiSettings;
import org.sonatype.nexus.plugins.mavenbridge.NexusAether;
import org.sonatype.nexus.plugins.mavenbridge.NexusMavenBridge;
import org.sonatype.nexus.plugins.mavenbridge.internal.FileItemModelSource;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.ArtifactStoreRequest;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.rest.AbstractArtifactViewProvider;
import org.sonatype.nexus.rest.ArtifactViewProvider;
import org.terracotta.nexus.plugins.depmgmt.model.ArtifactInformation;
import org.terracotta.nexus.plugins.depmgmt.model.DependencyInformation;
import org.terracotta.nexus.plugins.depmgmt.utils.ExceptionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
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

  @Requirement
  private GlobalRestApiSettings globalRestApiSettings;

  @Override
  protected Object retrieveView(ResourceStoreRequest request, RepositoryItemUid itemUid, StorageItem item, Request req) throws IOException {
    try {
      LOGGER.info("Starting dependency resolution");
      MavenRepository itemRepository = itemUid.getRepository().adaptToFacet(MavenRepository.class);
      Gav gav = itemRepository.getGavCalculator().pathToGav(itemUid.getPath());
      StorageFileItem pom = itemRepository.getArtifactStoreHelper().retrieveArtifactPom(new ArtifactStoreRequest(itemRepository, gav, true));

      Dependency dependency = createDependencyFromGav(gav);

      DependencyNode dependencyNode = resolveDirectDependencies(dependency);
      DependencyInformation rootDep = buildDependencies(dependencyNode, gav.isSnapshot());

      LOGGER.info("Done building dependencies");
      /*
      WTF? I get:
      java.lang.LinkageError: loader constraint violation: loader (instance of org/codehaus/plexus/classworlds/realm/ClassRealm) previously initiated loading for a different type with name "org/apache/maven/model/Model"
      when I try to make the call without using reflection (ie: when I declare class Model):
      //Properties properties = model.getProperties();
       */
      Object model = nexusMavenBridge.buildModel(new FileItemModelSource(pom), getMavenRepositories());
      Properties properties = (Properties)model.getClass().getMethod("getProperties").invoke(model);
      Object parent = model.getClass().getMethod("getParent").invoke(model);
      ArtifactInformation artifactInformation = new ArtifactInformation();
      // if we have a parent, we get some info about it and stick into the artifactInformation
      if (parent != null) {
        String id = (String) parent.getClass().getMethod("getId").invoke(parent);
        artifactInformation.setParent(id);
        DefaultArtifact parentArtifact = new DefaultArtifact(id);
        String parentHighestReleaseVersion =  getHighestVersion(parentArtifact, true);
        String parentHighestSnapshotVersion =  getHighestVersion(parentArtifact, false);
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
      LOGGER.error("Got exception in depmgmt plugin", e);
      Throwable rootCause = ExceptionUtils.getRootCause(e);
      return new ArtifactInformation(rootCause.getMessage() != null ? rootCause.getMessage() : rootCause.getClass().toString());
    } finally {
      LOGGER.info("Done extracting POM information");
    }
  }

  private org.sonatype.aether.graph.Dependency createDependencyFromGav(Gav gav) {
    return new Dependency(
        new DefaultArtifact( gav.getGroupId(), gav.getArtifactId(), gav.getExtension(), gav.getBaseVersion()),
        "compile");

  }

  private RemoteRepository exposePublicAsRemoteRepository() {
    String baseUrl = globalRestApiSettings.getBaseUrl();
    if (baseUrl == null) {
      baseUrl = Request.getCurrent().getRootRef().toString();
    }

    StringBuilder repositoryContentUrlBuilder = new StringBuilder(baseUrl);
    if (!baseUrl.endsWith("/")) {
      repositoryContentUrlBuilder.append("/");
    }
    repositoryContentUrlBuilder.append("content/groups/public");
    LOGGER.debug("Repository URL resolved to {}", repositoryContentUrlBuilder);
    return new RemoteRepository("public", "default", repositoryContentUrlBuilder.toString());
  }

  private DependencyNode resolveDirectDependencies(Dependency dependency) {
    DependencyNode dependencyNode = null;

    CollectRequest collectRequest = new CollectRequest();
    collectRequest.setRoot(dependency);
    collectRequest.addRepository(exposePublicAsRemoteRepository());
    try {
      CollectResult collectResult = nexusAether.getRepositorySystem()
          .collectDependencies(getRepositorySystemSession(), collectRequest);
      dependencyNode = collectResult.getRoot();
    } catch (DependencyCollectionException e) {
      LOGGER.error("Exception collecting deps", e);
    }
    return dependencyNode;
  }

  private DependencyInformation buildDependencies(DependencyNode parentNode, boolean snapshot) {
    DependencyInformation parent = new DependencyInformation(parentNode.getDependency().getArtifact());
    addLatestVersionInfo(parent, parentNode.getDependency().getArtifact(), false);
    // if the artifact is not a snapshot, don't bother adding version info to its deps
    buildDependencies(parent, parentNode.getChildren(), snapshot);
    return parent;
  }

  private void buildDependencies(DependencyInformation parent, List<DependencyNode> children, boolean addVersionInfo) {
    Collection<DependencyInformation> result = new ArrayList<DependencyInformation>();

    for (DependencyNode child : children) {
      Artifact artifact = child.getDependency().getArtifact();
      DependencyInformation childDep = new DependencyInformation(artifact);
      buildDependencies(childDep, child.getChildren(), addVersionInfo);
      // TODO : remove TC specific conditions
      if (addVersionInfo && parent.isTerracottaMaintained()) {
        addLatestVersionInfo(childDep, artifact, true);
      }
      result.add(childDep);
    }

    parent.setDependencies(result.toArray(new DependencyInformation[] { }));
  }

  private void addLatestVersionInfo(DependencyInformation dependencyInformation, Artifact artifact, boolean releaseOnly) {
    LOGGER.debug("Version range request for {}", artifact);
    // TODO : remove TC specific conditions
    if (!dependencyInformation.isTerracottaMaintained()) {
      return;
    }

    if (!releaseOnly) {
      String highestVersionString = getHighestVersion(artifact, false);
      dependencyInformation.setLatestSnapshotVersion(highestVersionString);
    }

    String highestVersionString = getHighestVersion(artifact, true);
    dependencyInformation.setLatestReleaseVersion(highestVersionString);
  }

  private String getHighestVersion(Artifact artifact, boolean release) {
    try {
      VersionRangeRequest request = new VersionRangeRequest();
      request.setArtifact(new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getExtension(), "[,]"));
      request.setRepositories(Collections.singletonList(exposePublicAsRemoteRepository()));

      VersionRangeResult versionRangeResult = nexusAether.getRepositorySystem().resolveVersionRange(nexusAether.getDefaultRepositorySystemSession(), request);
      LOGGER.debug("Version range result: {} with possible exceptions: {} for {}", versionRangeResult.getVersions(), versionRangeResult.getExceptions(), artifact);
      ListIterator<Version> versionListIterator = versionRangeResult.getVersions().listIterator(versionRangeResult.getVersions().size());
      while (versionListIterator.hasPrevious()) {
        Version highestVersion = versionListIterator.previous();
        if ((!release && highestVersion.toString().endsWith("SNAPSHOT")) || (release && !highestVersion.toString().endsWith("SNAPSHOT"))) {
          if (!artifact.getBaseVersion().equals(highestVersion.toString())) {
            LOGGER.debug("Setting latest version to {} for {}", highestVersion, artifact);
            return highestVersion.toString();
          } else {
            return null;
          }
        }
      }
    } catch (VersionRangeResolutionException e) {
      LOGGER.error("Unable to resolve version range", e);
    }
    return null;
  }

  private List<MavenRepository> getMavenRepositories() {
    return repositoryRegistry.getRepositoriesWithFacet(MavenRepository.class);
  }

  private RepositorySystemSession getRepositorySystemSession() {
    DefaultRepositorySystemSession repositorySystemSession = new DefaultRepositorySystemSession(nexusAether.getDefaultRepositorySystemSession());
    repositorySystemSession.setDependencySelector(new ScopeDependencySelector());
    repositorySystemSession.setDependencyTraverser(new DepthLimitedDependencyTraverser(1));
    return repositorySystemSession;
  }

}
