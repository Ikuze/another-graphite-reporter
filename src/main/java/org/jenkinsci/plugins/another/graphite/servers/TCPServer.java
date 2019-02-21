package org.jenkinsci.plugins.another.graphite.servers;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.PrintStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.net.Socket;
import java.io.DataOutputStream;
import hudson.util.FormValidation;
import hudson.Extension;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.DataBoundConstructor;


public class TCPServer extends Server {

    @DataBoundConstructor
    public TCPServer(@NonNull String ip, @NonNull String port,
                     @NonNull String id){
        this.ip = ip;
        this.id = id;
        this.port = port;
    }

    @Override
    protected void send(@NonNull String queue, @NonNull String value,
                      @NonNull long timestamp, PrintStream logger) throws UnknownHostException, IOException  {
        Socket conn = new Socket(this.getIp(), Integer.parseInt(this.getPort()));
        
        DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
        String data = queue + " " + value + " " + timestamp + "\n";

        if(logger != null){
            logger.println("TCP SENT DATA: " + data);
        }

        dos.writeBytes(data);
        conn.close();
    }

    @Extension
    public static class TCPDescriptorImpl extends DescriptorImpl {
        @Override
        public String getDisplayName() {
            return "TCP Graphite Server";
        }

        @Override
        public FormValidation doTestConnection(@QueryParameter("ip") final String ip,
                                               @QueryParameter("port") final String port) {
            if (!validator.isIpPresent(ip) || !validator.isPortPresent(port)
                    || !validator.isListening(ip, Integer.parseInt(port))) {
                return FormValidation.error("Server is not listening... Or ip:port are not correctly filled");
            }

            return FormValidation.ok("Server is listening");
        }
    }
}
