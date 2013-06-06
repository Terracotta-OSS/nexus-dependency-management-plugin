package org.terracotta.nexus.plugins.depmgmt.ui;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.plugins.rest.AbstractNexusResourceBundle;
import org.sonatype.nexus.plugins.rest.DefaultStaticResource;
import org.sonatype.nexus.plugins.rest.NexusResourceBundle;
import org.sonatype.nexus.plugins.rest.StaticResource;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ludovic Orban
 */
@Component(role=NexusResourceBundle.class, hint="DependencyManagementNexusPluginResourceBundle")
public class DependencyManagementNexusPluginResourceBundle extends AbstractNexusResourceBundle {

  public List<StaticResource> getContributedResouces() {
    return new ArrayList<StaticResource>() {{
      add(new DefaultStaticResource(getClass().getResource("/static/js/depmgmt-nexus-plugin-all.js"), "/js/depmgmt-nexus-plugin/depmgmt-nexus-plugin-all.js", "text/javascript"));
      add(new DefaultStaticResource(getClass().getResource("/static/icons/jar-jar.png"), "/icons/depmgmt-nexus-plugin/jar-jar.png", "image/png"));
      add(new DefaultStaticResource(getClass().getResource("/static/icons/terracotta-jar.png"), "/icons/depmgmt-nexus-plugin/terracotta-jar.png", "image/png"));
    }};
  }

}
