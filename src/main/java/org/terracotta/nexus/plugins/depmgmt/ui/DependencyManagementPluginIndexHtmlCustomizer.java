package org.terracotta.nexus.plugins.depmgmt.ui;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.plugins.rest.AbstractNexusIndexHtmlCustomizer;
import org.sonatype.nexus.plugins.rest.NexusIndexHtmlCustomizer;

import java.util.Map;

/**
 * @author Ludovic Orban
 */
@Component(role=NexusIndexHtmlCustomizer.class, hint="DependencyManagementPluginIndexHtmlCustomizer")
public class DependencyManagementPluginIndexHtmlCustomizer extends AbstractNexusIndexHtmlCustomizer {

  public String getPostHeadContribution(Map<String, Object> ctx) {
    return "<script src=\"js/depmgmt-nexus-plugin/depmgmt-nexus-plugin-all.js\" " +
        "type=\"text/javascript\" charset=\"utf-8\"></script>" +
    "<link rel=\"stylesheet\" href=\"css/depmgmt-nexus-plugin/depmgmt-nexus-plugin-all.css\" " +
            "type=\"text/css\" media=\"screen\" title=\"no title\" charset=\"utf-8\">";
  }

}
