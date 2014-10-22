package org.terracotta.nexus.plugins.depmgmt.resources;

import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.terracotta.nexus.plugins.depmgmt.model.ArtifactInformation;
import org.terracotta.nexus.plugins.depmgmt.model.DependencyInformation;

import com.thoughtworks.xstream.XStream;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * DependencyManagementFakePlexusResource
 */
@Named("dep-mgmt-fake")
@Singleton
public class DependencyManagementFakePlexusResource extends AbstractPlexusResource {
    @Override
    public String getResourceUri() {
        return "dep-mgmt/fake";
    }

    @Override
    public PathProtectionDescriptor getResourceProtection() {
        return null;
    }

    @Override
    public Object getPayloadInstance() {
        return null;
    }

    @Override
    public void configureXStream(XStream xstream) {
        xstream.processAnnotations(ArtifactInformation.class);
        xstream.processAnnotations(DependencyInformation.class);
    }
}
