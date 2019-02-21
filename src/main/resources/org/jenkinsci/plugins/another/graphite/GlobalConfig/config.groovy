package org.jenkinsci.plugins.another.graphite.GlobalConfig;

import org.jenkinsci.plugins.another.graphite.GlobalConfig;


def f = namespace(lib.FormTagLib);

f.section(title: descriptor.displayName) {
    f.entry(title: _("graphite_server_title"),
            help: descriptor.getHelpFile()) {
        
        f.repeatableHeteroProperty(
                field: "servers",
                hasHeader: "true",
                addCaption: _("graphite_servers_add_new"))
    }

    f.entry(title: _("base_queue_name"), field: "baseQueueName") {
        f.textbox()
    }
}
