package org.jenkinsci.plugins.another.graphite;

import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.Plugin;
import jenkins.model.Jenkins;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;

import org.jenkinsci.plugins.another.graphite.metrics.GraphiteMetric;

public class GraphitePlugin extends Plugin {

    private static final Logger LOGGER = Logger.getLogger(GraphitePlugin.class.getName());
    public static final ArrayList<GraphiteMetric> allMetrics = new ArrayList<GraphiteMetric>();

    @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED, before = InitMilestone.JOB_LOADED)
    public static void afterExtensionsAugmented() {
        LOGGER.log(Level.FINER, "Registering metrics provided by extension points...");
        Jenkins jenkins = Jenkins.getInstance();
        GraphitePlugin plugin = jenkins == null ? null : jenkins.getPlugin(GraphitePlugin.class);
        if (plugin == null) {
            LOGGER.log(Level.WARNING, "Could not register metrics plugin appears to be disabled");
            return;
        }

        for (GraphiteMetric metric : Jenkins.getInstance().getExtensionList(GraphiteMetric.class)) {
            LOGGER.log(Level.FINER, "Registering metric {0} (type {1})", new Object[]{metric, metric.getClass()});
            plugin.allMetrics.add(metric); 
        }

        LOGGER.log(Level.FINE, "Extensions registered");
    }
}
