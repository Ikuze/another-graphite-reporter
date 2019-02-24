# Another Jenkins Graphite Reporter

Plugin to report data to a graphite server.

Inspired by  [graphite-plugin](https://github.com/jenkinsci/graphite-plugin) I rewrote it and added support to pipeline steps. 

This plugin does not support freestyle build steps.

The best way to use this plugin is in a `declarative` pipeline.


# Build and Install
In order to build the plugin install maven and run in the directory where pom.xml file is located (the root directory):

    mvn clean install -DskipTests
    
It will generated the another-graphite-reporter.hpi file in the "target" directory. Go to Manage Jenkins -> Manage Plugins -> Advanced and Upload Plugin.

Or copy the .hpi file in the host plugin folder directly.


# Graphite Servers Configuration

Go to Manage Jenkins -> Configure System and fill in the graphite servers form.

It's pretty straight forward. Remember that the IDs will be used later on to tell the steps the servers that you want to use in the reporting, so use IDs that make sense.

You can select a TCP server or a UDP server.

The base queue name will identify the jenkins server, and will be prepended to every data queue sent in this jenkins instance.


# Pipeline Steps

Two pipeline steps are provided:

### graphiteData

The step sends to the graphite server any custom data given by the pipeline.

Parameter:

- **servers**: List of Strings. List of graphite servers ids configured in the global configuration.
- **dataQueue**: String. The queue for the data to be sent. It will be concatenated with the global base queue name defined in the global configuration, but it will NOT use the run/job name in it, so that different jobs can send data to the same queue.
 
- **data**: String. The data to be sent. It must be a string, and it must be a parsable float. "777,77" (comma) is NOT good. "777.77" (point) is good.
- **fail**: Boolean. Whether to fail the build or not if there is a problem reporting to the graphite server. If it is set to false, the build will continue ok no matter what.


### graphite

The step sends to the graphite server the result of the selected metrics. This step should be used in the "post" section of a declarative pipeline, since it reads the "RESULT" of the build. If it is not used like that (look at the example), it's you the one who should handle the result of the build before reporting (try-catch or whatever method you prefer).

Parameters:

- **servers**: List of Strings. List of graphite servers ids configured in the global configuration.
- **metricNames**: List of Strings. List of the metrics to be reported. There are three metrics implemented:
  *  "duration": reports the duration of the build since it began up to this moment. If used in the "post" section, it will report the duration of the whole build.
  *  "result": the result of the build at the moment of the reporting. If used in the "post" section, it will report the final result of the build. If not, you need to handle the result by yourself. It reports a "1" only for the actual result.
  *  "tests": reports the number of failed, skipped and total test cases executed in the build. Be aware that the "passed" test number needs to be calculated: passed = total - skipped - failed.

- **fail**: Boolean. Whether to fail the build or not if there is a problem reporting to the graphite server. If it is set to false, the build will continue ok no matter what.




# Example

    pipeline{

      agent any
    
      stages{
          stage("first"){
              steps{
                graphiteData  servers:["myserver_one"], 
                              dataQueue:"testqueue.testvalue", 
                              data:"777.77",
                              fail: true
              }
          }    
      }

      post{
          always{
               graphite (servers:["myserver_two"], metricNames: ["result", "duration"], fail: true)
          }       
      } 

    }

This example will report `777.77` value to `$globalqueuename.testqueue.testvalue` queue in the  `server_one` server configured in the global jenkins configuration.

It will report `1` value to `$globalqueuename.$job_name.result.SUCCESS` queue in `server_two`.

It will report the duration of the build in seconds to `$globalqueuename.$job_name.duration` queue in `server_two`.

Had the build reported junit tests, we could have added the "results" metric to the metricNames list.

# Extend Metrics

Remember that you can add your own metrics if you extend [GraphiteMetric](src/main/java/org/jenkinsci/plugins/another/graphite/metrics/GraphiteMetric.java) extension point.

The value returned by `getName()` in the extension point will be the metric name to be used in metricNames parameter.
