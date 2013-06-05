package org.terracotta.nexus.plugins.depmgmt.resources;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.restlet.data.Request;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.nexus.plugins.mavenbridge.NexusMavenBridge;
import org.sonatype.nexus.plugins.mavenbridge.Utils;
import org.sonatype.nexus.plugins.mavenbridge.internal.FileItemModelSource;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.ArtifactStoreRequest;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.AbstractArtifactViewProvider;
import org.sonatype.nexus.rest.ArtifactViewProvider;
import org.terracotta.nexus.plugins.depmgmt.model.ArtifactInformation;
import org.terracotta.nexus.plugins.depmgmt.model.Dependency;

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

  @Requirement
  private RepositoryRegistry repositoryRegistry;

  @Requirement
  private NexusMavenBridge nexusMavenBridge;

  @Override
  protected Object retrieveView(ResourceStoreRequest request, RepositoryItemUid itemUid, StorageItem item, Request req) throws IOException {
    try {
      MavenRepository itemRepository = itemUid.getRepository().adaptToFacet(MavenRepository.class);
      Gav gav = itemRepository.getGavCalculator().pathToGav(itemUid.getPath());
      StorageFileItem pom = itemRepository.getArtifactStoreHelper().retrieveArtifactPom(new ArtifactStoreRequest(itemRepository, gav, true));

      DependencyNode dependencyNode = nexusMavenBridge.collectDependencies(Utils.createDependencyFromGav(gav, "compile"), repositories());
      Dependency rootDep = buildDependencies(dependencyNode);

      /*
      WTF? I get:
      java.lang.LinkageError: loader constraint violation: loader (instance of org/codehaus/plexus/classworlds/realm/ClassRealm) previously initiated loading for a different type with name "org/apache/maven/model/Model"
      when I try to make the call without using reflection (ie: when I declare class Model):
      //Properties properties = model.getProperties();
       */
      Object model = nexusMavenBridge.buildModel(new FileItemModelSource(pom), repositories());
      Properties properties = (Properties)model.getClass().getMethod("getProperties").invoke(model);


      ArtifactInformation artifactInformation = new ArtifactInformation();

      artifactInformation.setRepositoryName(itemUid.getRepository().getName());
      artifactInformation.setSvnRevision(properties.getProperty("metadata.svn.revision"));
      artifactInformation.setBuildUrl(properties.getProperty("metadata.build.jenkins.url"));
      artifactInformation.setBuildProfiles(properties.getProperty("metadata.build.maven.active.profiles"));
      artifactInformation.setArtifact(rootDep);

      return artifactInformation;
    } catch (Exception e) {
      return new ArtifactInformation(e.getMessage());
    }
  }

  private Dependency buildDependencies(DependencyNode dependencyNode) {
    Dependency dependency = new Dependency(dependencyNode.getDependency().getArtifact());
    buildDependencies(dependency, dependencyNode.getChildren());
    return dependency;
  }

  private void buildDependencies(Dependency parent, List<DependencyNode> children) {
    Collection<Dependency> result = new ArrayList<Dependency>();

    for (DependencyNode child : children) {
      Artifact artifact = child.getDependency().getArtifact();
      Dependency childDep = new Dependency(artifact);
      buildDependencies(childDep, child.getChildren());
      result.add(childDep);
    }

    parent.setDependencies(result.toArray(new Dependency[] {}));
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
