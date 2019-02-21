package org.jenkinsci.plugins.another.graphite.metrics;

import hudson.Extension;
import hudson.model.Run;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.io.PrintStream;


@Extension
public class MetricDuration extends GraphiteMetric {

    @Override
    public String getName(){
        return "duration";
    }

    @Override
    public List<Snapshot> getSnapshots(@NonNull Run run, PrintStream logger){
        String queueName = "duration";
        String duration = null;

        // Depending on the jenkins version build duration will have a value or not
        //  if the build has not finished. If there is no value, we calculate it.
        if(run.getDuration() != 0){
            duration = String.valueOf((new Long(run.getDuration()).intValue() / 1000));
            this.log(logger, "Metric Duration: " + duration + " seconds.");
        }
        else{
            duration = String.valueOf((new Long((System.currentTimeMillis() - run.getStartTimeInMillis())/1000)));
            this.log(logger, "Metric Calculated Duration: " + duration + " seconds.");
        }

        Snapshot snapshot = new Snapshot(queueName,
                                         duration).rebaseQueue(run);

        ArrayList<Snapshot> snapshots = new ArrayList<Snapshot>();
        snapshots.add(snapshot);

        return snapshots;
    }
}
