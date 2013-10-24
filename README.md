===============
TimeseriesDelta
===============
TimeseriesDelta is a Java MapReduce job which can be used to find outliers in time-series data. It uses a SlidingWindow over a single pass of the data to find any data points greater than the specified threshold over the average value of the metric and writes those points out to HDFS. 

----
### Build 

	$ git clone https://github.com/prateek/TimeseriesDelta.git
	$ cd TimeseriesDelta
	$ mvn package
	
### Usage
	$ cd TimeseriesDelta
	$ hadoop jar \
	  target/timeseries.delta-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
	  com.cloudera.sa.timeseries.mapreduce.RollingAverage
	  
	Usage: com.cloudera.sa.timeseries.mapreduce.RollingAverage \
		   <hdfs-input-path> <hdfs-output-path> <local-config-file>
		   

### Config File Format
The file is required to be a formatted as [Java Properties File][jpf_link] – which is, for all intents and purposes, a key-value list. The structure of file expected is described first, after which the semantics are explained with examples.

[jpf_link]: http://docs.oracle.com/javase/7/docs/api/java/util/Properties.html

#### File Structure 
Comprises three top-level attributes and a list of attributes for each metric (used synonymously with alias from here on). 

##### Three top-level attributes:
- `timeseries.file.delimiter`: the delimiter used to seperate fields in the input files
- `timeseries.time.column`: the column index (zero-based) which contains the unix timestamp. 
- `timeseries.compute.aliases`: a comma separated list of unique aliases (at least one alias is required)
	
##### For each unique *\<alias\>* 
The following attributes are required, unless explicitly mentioned otherwise:

- `timeseries.<alias>.category_column`: The grouping column index. 
- `timeseries.<alias>.metric_type`: Either "COUNT" or the column index of the metric to use
- `timeseries.<alias>.compute_type`: "AVG" or "AVG_DELTA"
- `timeseries.<alias>.window_size`: Sliding window width in seconds
- `timeseries.<alias>.slide_size`: Sliding window increment in seconds
- `timeseries.<alias>.alert_ratio`: The threshold to exceed before writing
- `timeseries.<alias>.time_based`: Optional field, whose presence is used to indicate if the metric being computed should be the number of occurrences or if the metric should be used as a rate per unit time.
		
#### File Semantics
Say our input is weblog data, where fields are tab-separated and this is the structure of each line:
	
	<unix_timestamp>	<ip-address>	<response-size-bytes>
	
##### Example 1: High User-Activity
Lets try to find all the users who's access patterns greatly diverged from their usual behavior. 

 Putting some numbers down, we're trying to find all (time, ip-address) pairs when the ip-address was mentioned in the logs 10 times more in 1hr than its average over course of a day. i.e. We're trying to find outliers based on the *global* dataset. The following config file, `samples/example1.properties` walks us thru how to set this computation up:

	# tab-delimited
	timeseries.file.delimiter = \\t 
	
	# timestamp column idx
	timeseries.time.column = 0
	
	# single alias: high_user_activity
	timeseries.compute.aliases = high_user_activity
	
	# compute the metric over the ip-address column
	timeseries.high_user_activity.category_column = 1 
	
	# we're interested in the number of mentions 
	timeseries.high_user_activity.metric_type = COUNT
	
	# indicate that we're looking for the average number 
	# of occurrences over the window
	timeseries.high_user_activity.compute_type = AVG
	
	# indicate the average should be a time-based rate
	# i.e. we want avg_num_mentions_per_window_size to exceed it's daily average
	timeseries.high_user_activity.time_based = AVG
	
	# 1hr window
	timeseries.high_user_activity.window_size = 3600
	
	# move forward by 5m increments
	timeseries.high_user_activity.slide_size = 300
	
	# threshold ratio to exceed
	timeseries.high_user_activity.alert_ratio = 10
	
Usage:

	$ hadoop jar \
	  target/timeseries.delta-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
	  com.cloudera.sa.timeseries.mapreduce.RollingAverage \
	  /home/web-log-path /home/user/example-1 	samples/example1.properties
	  
	# the following directory has all relevant points
	$ hadoop fs -ls /home/user/example-1/high_user_activity 

##### Example 2: Spikes in response size per user
After looking at example 1 results, we may notice that certain users are hitting the website a lot during the lunch hour and we want to analyze what kind of load they're putting on the server. 	

  A simple way to measure this could be to look at the response sizes the services are sending back and trying to see any sudden spikes. But instead of trying to find spikes as defined by the global dataset, lets try to measure these changes in the locality around each point. i.e. a *local* spike instead of a *global* spike. To do this, we're going to rely upon `samples/example2.properties`   
  
  	# tab-delimited
	timeseries.file.delimiter = \\t 
	
	# timestamp column idx
	timeseries.time.column = 0
	
	# two aliases this time
	timeseries.compute.aliases = high_user_activity,user_response_outlier
	
	... same as the high_user_activity attributes from example1.properties ...
	
	# compute the metric over the ip-address column
	timeseries.user_response_outlier.category_column = 1 
	
	# we're interested in the response size, i.e. column idx 2
	timeseries.user_response_outlier.metric_type = 2 
	
	# indicate that we're looking for the change in the metric
	# as the window slides forward
	timeseries.user_response_outlier.compute_type = AVG_DELTA
	
	# note the abscence of the time based flag, this is because
	# we want the computation to be based on the change in response size
	# over the window, not the change in response size per unit time within 
	# the window
	
	# 30m window
	timeseries.user_response_outlier.window_size = 1800
	
	# move forward by 2m increments
	timeseries.user_response_outlier.slide_size = 120
	
	# threshold ratio to exceed
	timeseries.user_response_outlier.alert_ratio = 3
	
 Usage:

	$ hadoop jar \
	  target/timeseries.delta-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
	  com.cloudera.sa.timeseries.mapreduce.RollingAverage \
	  /home/web-log-path /home/user/example-2 samples/example2.properties
	  
	# the following directory has all relevant points 
	# for the user response spikes
	$ hadoop fs -ls /home/user/example-1/user_response_outlier
	
	# But we specified two aliases, the other one is also computed
	# in the same MR pass.
	$ hadoop fs -ls /home/user/example-1/high_user_activity


