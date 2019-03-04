package org.jenkinsci.plugins.another.graphite.metrics;

import hudson.Extension;
import hudson.model.Run;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.io.PrintStream;

import hudson.tasks.test.AbstractTestResultAction;


@Extension
public class MetricTests extends GraphiteMetric {

    @Override
    public String getName(){
        return "tests";
    }

    @Override
    public List<Snapshot> getSnapshots(@NonNull Run run, PrintStream logger){
        String queue = this.getName();

        AbstractTestResultAction action = run.getAction(AbstractTestResultAction.class);

        ArrayList<Snapshot> snapshots = new ArrayList<Snapshot>();
        if(action != null){
            Snapshot snapshot = new Snapshot("skipped",
                                             Integer.toString(action.getSkipCount()));
            snapshots.add(snapshot.rebaseQueue(queue).rebaseQueue(run));

            snapshot = new Snapshot("failed",
                                    Integer.toString(action.getFailCount()));
            snapshots.add(snapshot.rebaseQueue(queue).rebaseQueue(run));

            snapshot = new Snapshot("total",
                                    Integer.toString(action.getTotalCount()));
            snapshots.add(snapshot.rebaseQueue(queue).rebaseQueue(run));
        }
        else{
            this.log(logger, "No tests found! Nothing to report.");
        }

        return snapshots;
    }
}
