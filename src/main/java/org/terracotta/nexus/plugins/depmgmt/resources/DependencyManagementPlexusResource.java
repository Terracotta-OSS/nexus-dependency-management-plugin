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
import org.sonatype.plexus.rest.ReferenceFactory;
import org.terracotta.nexus.plugins.depmgmt.model.ArtifactInformation;
import org.terracotta.nexus.plugins.depmgmt.model.DependencyInformation;
import org.terracotta.nexus.plugins.depmgmt.utils.ExceptionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

  @Requirement
  private ReferenceFactory referenceFactory;

  @Override
  protected Object retrieveView(ResourceStoreRequest request, RepositoryItemUid itemUid, StorageItem item, Request req) throws IOException {
    try {
      LOGGER.info("Starting dependency resolution");
      MavenRepository itemRepository = itemUid.getRepository().adaptToFacet(MavenRepository.class);
      Gav gav = itemRepository.getGavCalculator().pathToGav(itemUid.getPath());
      StorageFileItem pom = itemRepository.getArtifactStoreHelper().retrieveArtifactPom(new ArtifactStoreRequest(itemRepository, gav, true));

      Dependency dependency = createDependencyFromGav(gav);

      RemoteRepository remoteRepository = exposePublicAsRemoteRepository(req);

      DependencyNode dependencyNode = resolveDirectDependencies(dependency, remoteRepository);
      DependencyInformation rootDep = buildDependencies(dependencyNode, gav.isSnapshot(), remoteRepository);

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
        String parentHighestReleaseVersion =  getHighestVersion(parentArtifact,true, remoteRepository);
        String parentHighestSnapshotVersion =  getHighestVersion(parentArtifact,false, remoteRepository);
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
    } finally {
      LOGGER.info("Done extracting POM information");
    }
  }

  private org.sonatype.aether.graph.Dependency createDependencyFromGav(Gav gav) {
    return new Dependency(
        new DefaultArtifact( gav.getGroupId(), gav.getArtifactId(), gav.getExtension(), gav.getBaseVersion()),
        "compile");

  }

  private RemoteRepository exposePublicAsRemoteRepository(Request req) {
    String publicURL = referenceFactory.createReference(referenceFactory.getContextRoot(req), "content/groups/public").toString();
    return new RemoteRepository("public", "default", publicURL);
  }

  private DependencyNode resolveDirectDependencies(org.sonatype.aether.graph.Dependency dependency, RemoteRepository remoteRepository) {
    DependencyNode dependencyNode = null;

    CollectRequest collectRequest = new CollectRequest();
    collectRequest.setRoot(dependency);
    collectRequest.addRepository(remoteRepository);
    try {
      CollectResult collectResult = nexusAether.getRepositorySystem()
          .collectDependencies(getRepositorySystemSession(), collectRequest);
      dependencyNode = collectResult.getRoot();
    } catch (DependencyCollectionException e) {
      LOGGER.error("Exception collecting deps", e);
    }
    return dependencyNode;
  }

  private DependencyInformation buildDependencies(DependencyNode parentNode, boolean snapshot, RemoteRepository remoteRepository) {
    DependencyInformation parent = new DependencyInformation(parentNode.getDependency().getArtifact());
    addLatestVersionInfo(parent, parentNode.getDependency().getArtifact(), false, remoteRepository);
    // if the artifact is not a snapshot, don't bother adding version info to its deps
    buildDependencies(parent, parentNode.getChildren(), snapshot, remoteRepository);
    return parent;
  }

  private void buildDependencies(DependencyInformation parent, List<DependencyNode> children, boolean addVersionInfo, RemoteRepository remoteRepository) {
    Collection<DependencyInformation> result = new ArrayList<DependencyInformation>();

    for (DependencyNode child : children) {
      Artifact artifact = child.getDependency().getArtifact();
      DependencyInformation childDep = new DependencyInformation(artifact);
      buildDependencies(childDep, child.getChildren(), addVersionInfo, remoteRepository);
      // TODO : remove TC specific conditions
      if (addVersionInfo && parent.isTerracottaMaintained()) {
        addLatestVersionInfo(childDep, artifact, true, remoteRepository);
      }
      result.add(childDep);
    }

    parent.setDependencies(result.toArray(new DependencyInformation[] { }));
  }

  private void addLatestVersionInfo(DependencyInformation dependencyInformation, Artifact artifact, boolean releaseOnly, RemoteRepository remoteRepository) {
    LOGGER.debug("Version range request for {}", artifact);
    // TODO : remove TC specific conditions
    if (!dependencyInformation.isTerracottaMaintained()) {
      return;
    }

    if (!releaseOnly) {
      String highestVersionString = getHighestVersion(artifact, false, remoteRepository);
      dependencyInformation.setLatestReleaseVersion(highestVersionString);
    }

    String highestVersionString = getHighestVersion(artifact, true, remoteRepository);
    dependencyInformation.setLatestReleaseVersion(highestVersionString);
  }

  /**
   *
   *
   * @param artifact
   * @param release : true for getting highest released version, false for highest snapshot version
   * @param remoteRepository
   * @return
   */
  private String getHighestVersion(Artifact artifact, boolean release, RemoteRepository remoteRepository) {
    try {
      VersionRangeRequest request = new VersionRangeRequest();
      request.setArtifact(new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getExtension(), "[,]"));
      request.setRepositories(Collections.singletonList(remoteRepository));

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
