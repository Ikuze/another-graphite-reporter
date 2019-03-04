package org.jenkinsci.plugins.another.graphite.metrics;

import hudson.Extension;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.List;
import java.io.PrintStream;
import edu.umd.cs.findbugs.annotations.NonNull;

import utils.StageChunkFinder;

import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graphanalysis.ForkScanner;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.graphanalysis.StandardChunkVisitor;
import org.jenkinsci.plugins.workflow.graphanalysis.MemoryFlowChunk;


@Extension
public class MetricStagesDuration extends GraphiteMetric {

    @Override
    public String getName(){
        return "stages";
    }

    @Override
    public List<Snapshot> getSnapshots(@NonNull Run run, PrintStream logger){
        String queueName = "stages";
        ArrayList<Snapshot> snapshots = new ArrayList<Snapshot>();

        ArrayList<MemoryFlowChunk> stages = this.getStagesChunks((WorkflowRun)run);

        if(stages.size() == 0){
            this.log(logger, "No stages found! Nothing to report.");
        }

        for(MemoryFlowChunk stage : stages){
            //remove spaces from name
            String stageName = this.getNameFromStageChunk(stage).replace(" ", ""); 
            String stageDuration = String.valueOf(this.getDurationFromStageChunk(stage));
            Snapshot snapshot = new Snapshot(stageName,
                                             stageDuration).rebaseQueue(queueName)
                                             .rebaseQueue(run);
            snapshots.add(snapshot);
        }

        return snapshots;
    }

    private ArrayList<MemoryFlowChunk> getStagesChunks(@NonNull WorkflowRun run) {
        FlowExecution execution = run.getExecution();

        CollectingChunkVisitor visitor = new CollectingChunkVisitor();
        StageChunkFinder finder = new StageChunkFinder();
        ForkScanner.visitSimpleChunks(execution.getCurrentHeads(), visitor, finder);

        return visitor.getChunks();
    }

    private String getNameFromStageChunk(@NonNull MemoryFlowChunk stageChunk){
        return stageChunk.getFirstNode().getAction(LabelAction.class).getDisplayName();
    }

    private long getDurationFromStageChunk(@NonNull MemoryFlowChunk stageChunk){
        long startTime = stageChunk.getFirstNode().getAction(TimingAction.class).getStartTime();

        long endTime = 0;
        // If we have a stage chunk, we have a first node, but we dont have an end node
        //  it means that this step is bein executed in the middle of this stage. The stage is still running.
        // Hence, we count the time up to now.
        if(stageChunk.getNodeAfter() != null){
            endTime = stageChunk.getNodeAfter().getAction(TimingAction.class).getStartTime();
        }
        else{
            endTime = System.currentTimeMillis();
        }

        //returned in seconds
        return (endTime - startTime)/1000;
    }


    private static class CollectingChunkVisitor extends StandardChunkVisitor {
        ArrayList<MemoryFlowChunk> allChunks = new ArrayList<MemoryFlowChunk>();
        ArrayDeque<MemoryFlowChunk> parallelHandler = new ArrayDeque<MemoryFlowChunk>();
    
        public ArrayList<MemoryFlowChunk> getChunks() {
            return new ArrayList<MemoryFlowChunk>(allChunks);
        }
    
        protected void handleChunkDone(@NonNull MemoryFlowChunk chunk) {
            allChunks.add(chunk);
            this.chunk = new MemoryFlowChunk();
        }

        @Override
        public void parallelStart(@NonNull FlowNode parallelStartNode, @NonNull FlowNode branchNode, @NonNull ForkScanner scanner) {
            this.chunk = parallelHandler.pop();
        }

        @Override
        public void parallelEnd(@NonNull FlowNode parallelStartNode, @NonNull FlowNode parallelEndNode, @NonNull ForkScanner scanner) {
            parallelHandler.push(this.chunk);
            this.chunk = new MemoryFlowChunk();
        }
    }
}
