package org.terracotta.nexus.plugins.depmgmt.resources;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.restlet.data.Request;
import org.sonatype.nexus.plugins.mavenbridge.NexusMavenBridge;
import org.sonatype.nexus.plugins.mavenbridge.internal.FileItemModelSource;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.ArtifactStoreRequest;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.rest.AbstractArtifactViewProvider;
import org.sonatype.nexus.rest.ArtifactViewProvider;
import org.terracotta.nexus.plugins.depmgmt.model.ArtifactInformation;

import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

/**
 * @author Ludovic Orban
 */
@Component(role = ArtifactViewProvider.class, hint = "depmgmt")
public class DependencyManagementPlexusResource extends AbstractArtifactViewProvider {

  @Requirement
  private NexusMavenBridge nexusMavenBridge;

  @Override
  protected Object retrieveView(ResourceStoreRequest request, RepositoryItemUid itemUid, StorageItem item, Request req) throws IOException {
    try {
      MavenRepository repository = itemUid.getRepository().adaptToFacet(MavenRepository.class);
      Gav gav = repository.getGavCalculator().pathToGav(itemUid.getPath());
      StorageFileItem pom = repository.getArtifactStoreHelper().retrieveArtifactPom(new ArtifactStoreRequest(repository, gav, true));
      Object model = nexusMavenBridge.buildModel(new FileItemModelSource(pom), Collections.singletonList(repository));

      /*
      WTF? I get:
      java.lang.LinkageError: loader constraint violation: loader (instance of org/codehaus/plexus/classworlds/realm/ClassRealm) previously initiated loading for a different type with name "org/apache/maven/model/Model"
      when I try to make the call without using reflection:
      //Properties properties = model.getProperties();
       */
      Properties properties = (Properties)model.getClass().getMethod("getProperties").invoke(model);


      ArtifactInformation artifactInformation = new ArtifactInformation();

      artifactInformation.setRepositoryName(itemUid.getRepository().getName());
      artifactInformation.setSvnVersion(properties.getProperty("metadata.svn.revision"));
      artifactInformation.setBuildUrl(properties.getProperty("metadata.build.jenkins.url"));
      artifactInformation.setBuildProfiles(properties.getProperty("metadata.build.maven.active.profiles"));

      return artifactInformation;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
