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
          fieldLabel : 'Parent Pom',
          name : 'parent',
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
            xtype : 'displayfield',
            name : 'error',
            fieldLabel : 'Error',
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
            rootVisible: true,
            listeners: {
              dblclick: {
                fn: this.search,
                scope: this
              }
            }
        }]
    });

};

Ext.extend(Sonatype.repoServer.DependencyManagementPanel, Ext.form.FormPanel, {

    showArtifact : function(data, artifactContainer) {
        var that =  this;
        this.data = data;
        if (data == null) {
            this.find('name', 'error')[0].setRawValue(null);
            this.find('name', 'buildProfiles')[0].setRawValue(null);
            this.find('name', 'svnRevision')[0].setRawValue(null);
            this.find('name', 'parent')[0].setRawValue(null);
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
                      var error = that.find('name', 'error')[0];
                      if (resp.error != null) {
                          error.show();
                          error.setRawValue('<span class="error">' + resp.error + '</span>');
                        } else if (response.responseText == "{}") {
                          error.show();
                          error.setRawValue('<span class="error">The request returned an empty response</span>');
                        }
                        else {
                            error.hide();
                            var buildProfiles = that.find('name', 'buildProfiles')[0];
                            var svnRevision = that.find('name', 'svnRevision')[0];
                            var buildUrl = that.find('name', 'buildUrl')[0];
                            var parent = that.find('name', 'parent')[0];

                            var defaultText = '<span class="no-parent-pom">No parent pom</span>';
                            parent.setRawValue(resp.parent == null ? defaultText : getParentPomInfo(resp.parent,resp.parentHighestReleaseVersion, resp.parentHighestSnapshotVersion));
                            parent.show();
                            if (resp.artifact.snapshot) {
                                buildProfiles.hide();
                                svnRevision.hide();
                                buildUrl.hide();
                            } else {
                                defaultText = '<span class="missing">Missing</span>';
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
    },
    search: function (node, event) {
      var groupId = node.attributes.groupId;
      var artifactId = node.attributes.artifactId;
      var version = node.attributes.version;
      window.location = "index.html#nexus-search;gav~" + groupId + "~" + artifactId + "~" + version + "~~~";
    }

});

function getParentPomInfo(parentId, parentHighestReleaseVersion, parentHighestSnapshotVersion) {
  var parentGav =  stringToGAV(parentId);
  var text = '&nbsp;&nbsp;<span class="latest-snapshot">Latest Snapshot';
  if (parentHighestSnapshotVersion != null) {
    text = text + ': ' + parentHighestSnapshotVersion + '</span>';
  } else {
    text = text + '</span>';
  }

  text = text + '&nbsp;&nbsp;<span class="latest-release">Latest Release';
  if (parentHighestReleaseVersion != null) {
    text = text + ': ' + parentHighestReleaseVersion + '</span>';
  } else {
    text = text + '</span>';
  }

  var parentPomInfo = '<a href="index.html#nexus-search;gav~' + parentGav.groupId + '~' + parentGav.artifactId + '~' + parentGav.version + '~' + parentGav.packaging + '~~">'  + parentId + '</a>' + text;
  return parentPomInfo;
}


function fillRootTreeNode(treeNode, artifact) {
    var text = artifact.groupId + ':' + artifact.artifactId + ':' + artifact.version;

    text = text + '&nbsp;&nbsp;<span class="latest-snapshot">Latest Snapshot';
    if (artifact.latestSnapshotVersion != null) {
        text = text + ': ' + artifact.latestSnapshotVersion + '</span>';
    } else {
        text = text + '</span>';
    }

    text = text + '&nbsp;&nbsp;<span class="latest-release">Latest Release';
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
        text = '<span class="snapshot">' + text + '</span>';
        expand = true;
    }

    if (artifact.latestReleaseVersion != null) {
        text = text + '&nbsp;&nbsp;<span class="new-release">New Release available: ' + artifact.latestReleaseVersion + '</span>';
        expand = true;
    }

    treeNode.setText(text);
    treeNode.attributes.groupId = artifact.groupId;
    treeNode.attributes.artifactId = artifact.artifactId;
    treeNode.attributes.version = artifact.version;


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
// groupId:artifactId:packaging:version
function stringToGAV(stringContainingGav) {
  var gav = new Object();
  var groupId = "";
  var artifactId = "";
  var packaging = "";
  var version = "";
  if(stringContainingGav != null &&  stringContainingGav.indexOf(":") != -1) {
    var trailing = "";
    groupId = stringContainingGav.substring(0, stringContainingGav.indexOf(":"));
    trailing = stringContainingGav.substring(stringContainingGav.indexOf(":") + 1);
    artifactId = trailing.substring(0, trailing.indexOf(":"));
    trailing = trailing.substring(trailing.indexOf(":") + 1);
    packaging = trailing.substring(0, trailing.indexOf(":"));
    version = trailing.substring(trailing.indexOf(":") + 1);
  }
  gav.groupId = groupId;
  gav.artifactId =  artifactId;
  gav.packaging = packaging;
  gav.version = version;
  return gav;
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