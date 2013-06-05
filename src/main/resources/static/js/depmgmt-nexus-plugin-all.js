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
        }]
    });

};

Ext.extend(Sonatype.repoServer.DependencyManagementPanel, Ext.form.FormPanel, {

    showArtifact : function(data, artifactContainer) {
        var that =  this;
        this.data = data;
        if (data == null) {
            this.find('name', 'buildProfiles')[0].setRawValue(null);
            this.find('name', 'svnRevision')[0].setRawValue(null);
            this.find('name', 'buildUrl')[0].setRawValue(null);
            that.find('name', 'error')[0].setText(null);
        } else {
            var resourceURI = this.data.resourceURI;

            Ext.Ajax.request({
                url : this.data.resourceURI + '?describe=depmgmt',
                callback : function(options, isSuccess, response) {
                    if (isSuccess) {
                        var resp = Ext.decode(response.responseText);

                        if (resp.error != null) {
                            that.find('name', 'error')[0].setText(resp.error);
                        } else {
                            if (resp.buildProfiles != null) that.find('name', 'buildProfiles')[0].setRawValue(resp.buildProfiles);
                            if (resp.svnRevision != null) that.find('name', 'svnRevision')[0].setRawValue(resp.svnRevision);
                            if (resp.buildUrl != null) that.find('name', 'buildUrl')[0].setRawValue('<a href="' + resp.buildUrl + '" target="_blank">' + resp.buildUrl + '</a>');
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