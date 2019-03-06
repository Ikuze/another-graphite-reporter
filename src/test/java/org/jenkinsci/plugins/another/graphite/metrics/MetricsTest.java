package org.jenkinsci.plugins.another.graphite.metrics;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import org.junit.Rule;
import org.junit.Test;
import org.junit.Assert;
import org.hamcrest.CoreMatchers;
import hudson.model.Result;
import java.util.List;

import org.jvnet.hudson.test.JenkinsRule;


public class MetricsTest
{
    @Rule
    public JenkinsRule j = new JenkinsRule();


    @Test
    public void duration() throws Exception{
        long sleepFor = 3;
        WorkflowJob pipeline = j.jenkins.createProject(WorkflowJob.class, "TestDurationJob");
        String pipelineScript = this.getOnlyResulPipeline(Result.SUCCESS, sleepFor);
        System.out.println(pipelineScript);
        pipeline.setDefinition(new CpsFlowDefinition(pipelineScript, true)); 
        WorkflowRun run = pipeline.scheduleBuild2(0).get();

        MetricDuration metricDuration = new MetricDuration();
        List<GraphiteMetric.Snapshot> snapshots = metricDuration.getSnapshots(run, null);

        j.assertBuildStatusSuccess(run);
        Assert.assertThat(snapshots.size(), CoreMatchers.is(1));
        GraphiteMetric.Snapshot snapshot = snapshots.get(0);
        Assert.assertThat(snapshot.getQueue(), CoreMatchers.is("TestDurationJob.duration"));
        Assert.assertThat(snapshot.getValue(), CoreMatchers.is(String.valueOf(sleepFor)));
    }


    private String getOnlyResulPipeline(Result result, long duration){
        String pipelineScript = ""+
            "sleep " + String.valueOf(duration) + " \n" + 
            "currentBuild.result = '" + result.toString() + "'\n";

        return pipelineScript;
    }



}
