package org.jenkinsci.plugins.another.graphite.pipeline;

import com.google.common.collect.ImmutableSet;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import hudson.Extension;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import org.kohsuke.stapler.DataBoundConstructor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.TaskListener;

import hudson.model.Run;
import jenkins.model.GlobalConfiguration;

import org.jenkinsci.plugins.another.graphite.metrics.GraphiteMetric;
import org.jenkinsci.plugins.another.graphite.servers.Server;
import org.jenkinsci.plugins.another.graphite.GlobalConfig;
import org.jenkinsci.plugins.another.graphite.GraphitePlugin;


public class MetricReporterStep extends Step {

    private List<String> servers;
    List<String> metricNames;
    boolean fail;


    @DataBoundConstructor
    public MetricReporterStep(@NonNull List <String> servers,
                              @NonNull List <String> metricNames,
                              @NonNull boolean fail) {
        this.servers = servers;
        this.metricNames = metricNames;
        this.fail = fail;
    }

    public List<String> getServers(){
        return this.servers;
    }

    public void setServers(List<String> servers){
        this.servers = servers;
    }

    public List<String> getMetricNames(){
        return this.metricNames;
    }

    public void setMetricNames(List<String> metricNames){
        this.metricNames = metricNames;
    }

    public boolean getFail(){
        return this.fail;
    }

    public void setFail(boolean fail){
        this.fail = fail;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(this.servers, this.metricNames, this.fail, context);
    }


    @Extension
    public static final class StepDescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "graphite";
        }

        @Override
        public String getDisplayName() {
            return "Report single data to graphite server";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class);
        }
    }


    public static class Execution extends SynchronousStepExecution<Void> {
        
        @SuppressFBWarnings(value="SE_TRANSIENT_FIELD_NOT_RESTORED", justification="Only used when starting.")
        private transient final List<String> serverIds;
        private transient final List<String> metricNames;
        private transient final boolean fail;

        Execution(List<String> serverIds, List<String> metricNames, boolean fail, StepContext context) {
            super(context);
            this.serverIds = serverIds;
            this.metricNames = metricNames;
            this.fail = fail;
        }

        @Override
        protected Void run() throws Exception {
            TaskListener listener = getContext().get(TaskListener.class);
            Run run = getContext().get(Run.class);

            try{
                String baseQueueName = this.getBaseQueueName();

                ArrayList<GraphiteMetric.Snapshot> snapshots = new ArrayList<GraphiteMetric.Snapshot>();

                for(GraphiteMetric metric : GraphitePlugin.allMetrics){
                    if (metricNames.contains(metric.getName())){
                        for(GraphiteMetric.Snapshot snapshot : metric.getSnapshots(run,
                                                                                   listener.getLogger())){
                            snapshots.add(snapshot.rebaseQueue(baseQueueName));
                        }
                    }
                }

                long timestamp = System.currentTimeMillis()/1000;
                for(String serverId : this.serverIds){
                    listener.getLogger().println("Sending data to graphite server: " + serverId);
                    Server server = this.getServerById(serverId);
                    if(server == null){
                        listener.getLogger().println("Server does not exist! Server not configured!");
                    }
                    server.send(snapshots, timestamp, listener.getLogger());
                }
            }
            catch(Exception e){
                if(this.fail){
                    listener.getLogger().println("EXCEPTION THROWN: " + e);
                    throw e;
                }
                else{
                    listener.getLogger().println("Data could not be sent. Not failing because of the configuration.");
                    listener.getLogger().println("EXCEPTION THROWN: " + e);
                }
            }
            return null;
        }

        @NonNull
        public Server getServerById(@NonNull String serverId) {
            GlobalConfig globalConfig = GlobalConfiguration.all().get(GlobalConfig.class);

            List<Server> servers = globalConfig.getServers();
            for (Server server : servers) {
                if (server.getId().equals(serverId)) {
                    return server;
                }
            }
            return null;
        }

        @NonNull
        public String getBaseQueueName() {
            GlobalConfig globalConfig = GlobalConfiguration.all().get(GlobalConfig.class);
            return globalConfig.getBaseQueueName();
        }
    }
}
