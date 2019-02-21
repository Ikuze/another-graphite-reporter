package org.jenkinsci.plugins.another.graphite.servers;

import org.jenkinsci.plugins.another.graphite.metrics.GraphiteMetric.Snapshot;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.PrintStream;
import java.util.List;
import java.io.IOException;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.io.DataOutputStream;
import hudson.model.AbstractDescribableImpl;

import utils.GraphiteValidator;
import hudson.util.FormValidation;
import hudson.model.Descriptor;
import hudson.Extension;
import org.kohsuke.stapler.QueryParameter;

import java.util.List;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.DataBoundConstructor;


public abstract class Server extends AbstractDescribableImpl<Server> {

    protected String ip;
    protected String port;
    protected String id;


    public void setIp(@NonNull String ip) {
        this.ip = ip;
    }

    public String getIp() {
        return ip;
    }

    public void setPort(@NonNull String port) {
        this.port = port;
    }

    public String getPort() {
        return port;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void send(@NonNull List<Snapshot> snapshots, @NonNull long timestamp, 
                     PrintStream logger) throws UnknownHostException, IOException {
        for(Snapshot snapshot: snapshots){
            this.send(snapshot.getQueue(), snapshot.getValue(), timestamp, logger);
        }
    }

    public void send(@NonNull List<Snapshot> snapshots, PrintStream logger) throws UnknownHostException, IOException {
        long timestamp = System.currentTimeMillis()/1000;
        for(Snapshot snapshot: snapshots){
            this.send(snapshot.getQueue(), snapshot.getValue(), timestamp, logger);
        }
    }

    public void send(@NonNull Snapshot snapshot, @NonNull long timestamp, PrintStream logger) throws UnknownHostException, IOException {
        this.send(snapshot.getQueue(), snapshot.getValue(), timestamp, logger);
    }

    public void send(@NonNull Snapshot snapshot, PrintStream logger) throws UnknownHostException, IOException {
        long timestamp = System.currentTimeMillis()/1000;
        this.send(snapshot.getQueue(), snapshot.getValue(), timestamp, logger);
    }

    protected abstract void send(@NonNull String queue, @NonNull String value,
                               @NonNull long timestamp, PrintStream logger) throws UnknownHostException, IOException;

    public static abstract class DescriptorImpl extends Descriptor<Server> {
        protected GraphiteValidator validator = new GraphiteValidator();

        public abstract FormValidation doTestConnection(@QueryParameter("ip") final String ip,
                                                        @QueryParameter("port") final String port);

        public FormValidation doCheckIp(@QueryParameter final String value) {
            if (!validator.isIpPresent(value)) {
                return FormValidation.error("Please set an IP/Host");
            }
            if (!validator.validateIpFormat(value)) {
                return FormValidation.error("Cannot reach this IP/Host.");
            }

            return FormValidation.ok("IP/Host is correctly configured");
        }

        public FormValidation doCheckId(@QueryParameter final String value) {
            if (!validator.isIDPresent(value)) {
                return FormValidation.error("Please set an ID");
            }
            int length = 50;
            if (validator.isIDTooLong(value, length)) {
                return FormValidation.error(String.format("You should use less than %d characters", length));
            }

            return FormValidation.ok("ID is correctly configured");
        }

        public FormValidation doCheckPort(@QueryParameter final String value) {
            if (!validator.isPortPresent(value)) {
                return FormValidation.error("Please set a port");
            }

            if (!validator.validatePortFormat(value)) {
                return FormValidation.error("Please check the port format");
            }

            return FormValidation.ok("Port is correctly configured");
        }
    }
}
