package org.terracotta.nexus.plugins.depmgmt.ui;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.plugins.ui.contribution.UiContributorSupport;
import org.terracotta.nexus.plugins.depmgmt.DependencyManagementPlugin;

/**
 * DependencyManagementUIContributor
 */
@Named
@Singleton
public class DependencyManagementUiContributor extends UiContributorSupport {

    @Inject
    public DependencyManagementUiContributor(DependencyManagementPlugin owner) {
        super(owner);
    }
}
