Sonatype.repoServer.DependencyManagementPanel = function (config) {
    var config = config || {};
    var defaultConfig = {};
    Ext.apply(this, config, defaultConfig);

    Sonatype.repoServer.DependencyManagementPanel.superclass.constructor.call(this, {
        title: 'Dependency Management',
        autoScroll: true,
        border: true,
        frame: true,
        collapsible: false,
        collapsed: false,
        items: [{
            xtype : 'displayfield',
            fieldLabel : 'Build URL',
            name : 'buildUrl',
            anchor : Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
            allowBlank : true,
            readOnly : true
        },{
            xtype : 'displayfield',
            fieldLabel : 'SVN Revision',
            name : 'svnRevision',
            anchor : Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
            allowBlank : true,
            readOnly : true
        },{
            xtype : 'displayfield',
            fieldLabel : 'Build Profiles',
            name : 'buildProfiles',
            anchor : Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
            allowBlank : true,
            readOnly : true
        },{
            xtype : 'label',
            name : 'error',
            anchor : Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
            allowBlank : true,
            readOnly : true
        },{
            xtype : 'treepanel',
            name : 'treePanel',
            collapsible: true,
            title: 'Dependencies',
            loader: new Ext.tree.TreeLoader(),
            root: new Ext.tree.TreeNode({expanded: true}),
            rootVisible: true
        }]
    });

};

Ext.extend(Sonatype.repoServer.DependencyManagementPanel, Ext.form.FormPanel, {

    showArtifact : function(data, artifactContainer) {
        var that =  this;
        this.data = data;
        if (data == null) {
            this.find('name', 'error')[0].setText(null);
            this.find('name', 'buildProfiles')[0].setRawValue(null);
            this.find('name', 'svnRevision')[0].setRawValue(null);
            this.find('name', 'buildUrl')[0].setRawValue(null);
            this.find('name', 'treePanel')[0].getRootNode().removeAll(true);
            this.find('name', 'treePanel')[0].getRootNode().setText(null);
            this.find('name', 'treePanel')[0].getRootNode().setIcon(null);
        } else {
            var resourceURI = this.data.resourceURI;

            Ext.Ajax.request({
                url : this.data.resourceURI + '?describe=depmgmt',
                callback : function(options, isSuccess, response) {
                    if (isSuccess) {
                        var resp = Ext.decode(response.responseText);

                        if (resp.error != null) {
                            that.find('name', 'error')[0].setText('<span style="color: #dd2222;">' + resp.error + '</span>');
                        } else {
                            var buildProfiles = that.find('name', 'buildProfiles')[0];
                            var svnRevision = that.find('name', 'svnRevision')[0];
                            var buildUrl = that.find('name', 'buildUrl')[0];
                            if (resp.artifact.snapshot) {
                                buildProfiles.hide();
                                svnRevision.hide();
                                buildUrl.hide();
                            } else {
                                var defaultText = '<span style="color: #dd2222;font-style: bold;">Missing</span>';

                                buildProfiles.setRawValue(resp.buildProfiles == null ? defaultText : resp.buildProfiles);
                                svnRevision.setRawValue(resp.svnRevision == null ? defaultText : resp.svnRevision);
                                buildUrl.setRawValue(resp.buildUrl == null ? defaultText : '<a href="' + resp.buildUrl + '" target="_blank">' + resp.buildUrl + '</a>');
                                buildProfiles.show();
                                svnRevision.show();
                                buildUrl.show();
                            }

                            that.find('name', 'treePanel')[0].getRootNode().removeAll(true);
                            if (resp.artifact != null) {
                                var rootNode = that.find('name', 'treePanel')[0].getRootNode();
                                fillRootTreeNode(rootNode, resp.artifact);
                                appendChildren(rootNode, resp.artifact.dependencies);
                            }

                            that.find('name', 'error')[0].setText(null);
                        }
                    } else {
                        if (response.status = 404) {
                            artifactContainer.hideTab(this);
                        } else {
                            Sonatype.utils.connectionError(response, 'Unable to retrieve artifact information.');
                        }
                    }
                },
                scope : this,
                method : 'GET',
                suppressStatus : '404'
            });

        }
    }

});

function fillRootTreeNode(treeNode, artifact) {
    var text = artifact.groupId + ':' + artifact.artifactId + ':' + artifact.version;

    text = text + '&nbsp;&nbsp;<span style="background-color: #5555aa;color: #ffffff;font-style: italic;">Latest Snapshot';
    if (artifact.latestSnapshotVersion != null) {
        text = text + ': ' + artifact.latestSnapshotVersion + '</span>';
    } else {
        text = text + '</span>';
    }

    text = text + '&nbsp;&nbsp;<span style="background-color: #5555aa;color: #ffffff;font-style: italic;">Latest Release';
    if (artifact.latestReleaseVersion != null) {
        text = text + ': ' + artifact.latestReleaseVersion + '</span>';
    } else {
        text = text + '</span>';
    }

    treeNode.setText(text);

    if (artifact.terracottaMaintained) {
        treeNode.setIcon("icons/depmgmt-nexus-plugin/terracotta-jar.png");
    } else {
        treeNode.setIcon("icons/depmgmt-nexus-plugin/jar-jar.png");
    }
}

function fillTreeNode(treeNode, artifact) {
    var expand = false;
    var text = artifact.groupId + ':' + artifact.artifactId + ':' + artifact.version;

    if (artifact.snapshot) {
        text = '<span style="background-color: #aa5555;color: #ffffff;">' + text + '</span>';
        expand = true;
    }

    if (artifact.latestReleaseVersion != null) {
        text = text + '&nbsp;&nbsp;<span style="background-color: #55aa55;color: #ffffff;font-style: italic;">New Release available: ' + artifact.latestReleaseVersion + '</span>';
        expand = true;
    }

    treeNode.setText(text);

    if (artifact.terracottaMaintained) {
        treeNode.setIcon("icons/depmgmt-nexus-plugin/terracotta-jar.png");
        expand = true;
    } else {
        treeNode.setIcon("icons/depmgmt-nexus-plugin/jar-jar.png");
    }
    return expand;
}

function appendChildren(treeNode, dependencies) {
    var expand = false;
    for (var i = 0; i<dependencies.length ; i++) {
        var dependency = dependencies[i];

        var subNode = new Ext.tree.TreeNode();
        if (fillTreeNode(subNode, dependency)) {
            expand = true;
        }

        appendChildren(subNode, dependency.dependencies);
        treeNode.appendChild(subNode);
    }
    treeNode.expanded = expand;
}

Sonatype.Events.addListener("fileContainerInit", function (items) {
    items.push(new Sonatype.repoServer.DependencyManagementPanel({name: "DependencyManagementPanel", tabTitle: "Dependency Management", preferredIndex: 50}))
});

Sonatype.Events.addListener("fileContainerUpdate", function (artifactContainer, data) {
    var panel = artifactContainer.find("name", "DependencyManagementPanel")[0];
    if (data == null || !data.leaf) {
        panel.showArtifact(null, artifactContainer)
    } else {
        panel.showArtifact(data, artifactContainer)
    }
});

Sonatype.Events.addListener("artifactContainerInit", function (items) {
    items.push(new Sonatype.repoServer.DependencyManagementPanel({name: "DependencyManagementPanel", tabTitle: "Dependency Management", preferredIndex: 50}))
});

Sonatype.Events.addListener("artifactContainerUpdate", function (artifactContainer, payload) {
    var panel = artifactContainer.find("name", "DependencyManagementPanel")[0];
    if (payload == null || !payload.leaf) {
        panel.showArtifact(null, artifactContainer)
    } else {
        panel.showArtifact(payload, artifactContainer)
    }
});