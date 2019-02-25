package org.jenkinsci.plugins.another.graphite.servers.Server


import org.jenkinsci.plugins.another.graphite.servers.Server

def f = namespace(lib.FormTagLib);

f.entry(title: _("servers_id"), field: "id") {
    f.textbox()
}

f.entry(title: _("servers_ip"), field: "ip") {
    f.textbox()
}

f.entry(title: _("servers_port"), field: "port") {
    f.textbox()
}

f.entry(title: _("servers_verbose"), field: "verbose") {
    f.checkbox()
}

f.block() {
    f.validateButton(
            title: _("test_server_connection"),
            progress: _("testing_server_connection"),
            method: "testConnection",
            with: "ip,port"
    )
}
