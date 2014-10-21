package org.terracotta.nexus.plugins.depmgmt;

import org.eclipse.sisu.EagerSingleton;
import org.jetbrains.annotations.NonNls;
import org.sonatype.nexus.plugin.PluginIdentity;

import javax.inject.Named;

/**
 * DependencyManagement Plugin
 */
@Named
@EagerSingleton
public class DependencyManagementPlugin extends PluginIdentity {

    @NonNls
    public static final String ID_PREFIX = "dependency-management";

    @NonNls
    public static final String GROUP_ID = "org.terracotta.nexus.plugins";

    @NonNls
    public static final String ARTIFACT_ID = "nexus-" + ID_PREFIX + "-plugin";

    public DependencyManagementPlugin() throws Exception {
        super(GROUP_ID, ARTIFACT_ID);
    }
}
