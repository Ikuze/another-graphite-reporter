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
        String pipelineScript = this.getOnlyResultPipeline(Result.SUCCESS, sleepFor);
        System.out.println(pipelineScript);
        pipeline.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun run = pipeline.scheduleBuild2(0).get();

        MetricDuration metricDuration = new MetricDuration();
        List<GraphiteMetric.Snapshot> snapshots = metricDuration.getSnapshots(run, null);

        j.assertBuildStatusSuccess(run);
        // Only one value was returned
        Assert.assertThat(snapshots.size(), CoreMatchers.is(1));
        GraphiteMetric.Snapshot snapshot = snapshots.get(0);
        // The value is sent toe the expected path
        Assert.assertThat(snapshot.getQueue(), CoreMatchers.is("TestDurationJob.duration"));
        // The value is OK
        Assert.assertThat(snapshot.getValue(), CoreMatchers.is(String.valueOf(sleepFor)));
    }


    @Test
    public void resultSuccess() throws Exception{
        this.genericResultTest("TestSuccessJob", Result.SUCCESS);
    }

    @Test
    public void resultFailed() throws Exception{
        this.genericResultTest("TestFailedJob", Result.FAILURE);
    }

    @Test
    public void resultUnstable() throws Exception{
        this.genericResultTest("TestUnstableJob", Result.UNSTABLE);
    }


    @Test
    public void resultAborted() throws Exception{
        this.genericResultTest("TestAbortedJob", Result.ABORTED);
    }

    @Test
    public void testResults() throws Exception{
        WorkflowJob pipeline = j.jenkins.createProject(WorkflowJob.class, "TestResultsJob");
        String pipelineScript = this.getJunitTestsPipeline();
        System.out.println(pipelineScript);
        pipeline.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun run = pipeline.scheduleBuild2(0).get();

        System.out.println(run.getActions().toString());
        MetricTests metricTests = new MetricTests();
        List<GraphiteMetric.Snapshot> snapshots = metricTests.getSnapshots(run, null);

        // the build run as expected
        j.assertBuildStatus(Result.UNSTABLE, run);
        // 3 snapshots returned, SKIP, FAIL, TOTAL
        Assert.assertThat(snapshots.size(), CoreMatchers.is(3));

        // Check skipped
        GraphiteMetric.Snapshot snapshotSkipped = this.getByResult(snapshots, "skipped");
        Assert.assertNotNull(snapshotSkipped);
        // The value is sent toe the expected path
        Assert.assertThat(snapshotSkipped.getQueue(), CoreMatchers.is("TestResultsJob.tests.skipped"));
        // The value is OK
        Assert.assertThat(snapshotSkipped.getValue(), CoreMatchers.is(String.valueOf(5)));


        // Check failed
        GraphiteMetric.Snapshot snapshotFailed = this.getByResult(snapshots, "failed");
        Assert.assertNotNull(snapshotFailed);
        // The value is sent toe the expected path
        Assert.assertThat(snapshotFailed.getQueue(), CoreMatchers.is("TestResultsJob.tests.failed"));
        // The value is OK
        Assert.assertThat(snapshotFailed.getValue(), CoreMatchers.is(String.valueOf(4)));

        // Check total
        GraphiteMetric.Snapshot snapshotTotal = this.getByResult(snapshots, "total");
        Assert.assertNotNull(snapshotTotal);
        // The value is sent toe the expected path
        Assert.assertThat(snapshotTotal.getQueue(), CoreMatchers.is("TestResultsJob.tests.total"));
        // The value is OK
        Assert.assertThat(snapshotTotal.getValue(), CoreMatchers.is(String.valueOf(10)));


    }

    private GraphiteMetric.Snapshot getByResult(List<GraphiteMetric.Snapshot> snapshots, String result){
        for(GraphiteMetric.Snapshot snapshot : snapshots){
            if(snapshot.getQueue().contains(result.toString())){
                return snapshot;
            }
        }

        return null;
    }

    private void genericResultTest(String jobName, Result result) throws Exception{
        long sleepFor = 0;
        WorkflowJob pipeline = j.jenkins.createProject(WorkflowJob.class, jobName);
        String pipelineScript = this.getOnlyResultPipeline(result, sleepFor);
        System.out.println(pipelineScript);
        pipeline.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun run = pipeline.scheduleBuild2(0).get();

        MetricResult metricResult = new MetricResult();
        List<GraphiteMetric.Snapshot> snapshots = metricResult.getSnapshots(run, null);

        j.assertBuildStatus(result, run);
        // Only one value was returned
        Assert.assertThat(snapshots.size(), CoreMatchers.is(1));
        GraphiteMetric.Snapshot snapshot = snapshots.get(0);
        // The value is sent toe the expected path
        Assert.assertThat(snapshot.getQueue(), CoreMatchers.is(jobName + ".result." + result.toString()));
        // The value is 1
        Assert.assertThat(snapshot.getValue(), CoreMatchers.is("1"));
    }


    private String getOnlyResultPipeline(Result result, long duration){
        String pipelineScript = ""+
            "sleep " + String.valueOf(duration) + " \n" +
            "currentBuild.result = '" + result.toString() + "'\n";

        return pipelineScript;
    }

    private String getJunitTestsPipeline(){

        String pipelineScript = ""+
            "node{\n" +
            "writeFile file: 'junitfile.xml', text:\"" + this.getFakeXMLJunit() + "\" \n" +
            "junit 'junitfile.xml' \n" +
            "}\n";

        return pipelineScript;
    }

    private String getFakeXMLJunit(){
        // TODO: write a better and generic way to generate the xml file
        String fakeXML = "<?xml version='1.0' encoding='UTF-8'?><testsuites>" +
          "<testsuite name='mysuitename' time='80.0' tests='4' errors='0' failures='2' skipped='2'>" +
          "  <testcase name='testname 1' time='23'>" +
          "    <skipped/>" +
          "  </testcase>" +
          "  <testcase name='testname 2' time='17'>" +
          "    <failure message='this is my error message'/>" +
          "  </testcase>" +
          "  <testcase name='testname 3' time='23' classname='shtest'>" +
          "    <skipped/>" +
          "  </testcase>" +
          "  <testcase name='testname 4' time='17' classname='shtest'>" +
          "    <failure message='this is my error message'/>" +
          "  </testcase>" +
          "</testsuite>" +
          "<testsuite name='mysuitename2' time='103.0' tests='6' errors='0' failures='2' skipped='3'>" +
          "  <testcase name='testname 5' time='23' classname='shtest'>" +
          "    <skipped/>" +
          "  </testcase>" +
          "  <testcase name='testname 6' time='17' classname='shtest'>" +
          "    <failure message='this is my error message'/>" +
          "  </testcase>" +
          "  <testcase name='testname 7' time='23'>" +
          "    <skipped/>" +
          "  </testcase>" +
          "  <testcase name='testname 8' time='17'>" +
          "    <failure message='this is my error message'/>" +
          "  </testcase>" +
          "  <testcase name='testname 9' time='20'/>" +
          "  <testcase name='testname 10' time='23'>" +
          "    <skipped/>" +
          "  </testcase>" +
          "</testsuite>" +
          "</testsuites>";

        return fakeXML;
    }

}
