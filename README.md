===============
TimeseriesDelta
===============
TimeseriesDelta is a Java MapRedue job which can be used to find outliers in timeseries data. It uses a SlidingWindow over a single pass of the data to find any data points greater than the specified threshold over the average value of the metric and writes those points out to HDFS. 

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
- `timeseries.compute.aliases`: a comma seperated list of unique aliases (at least one alias is required)
	
##### For each unique *\<alias\>* 
The following attributes are required, unless explicitly mentioned otherwise:

- `timeseries.<alias>.category_column`: The grouping column index. 
- `timeseries.<alias>.metric_type`: Either "COUNT" or the column index of the metric to use
- `timeseries.<alias>.compute_type`: "AVG" or "AVG_DELTA"
- `timeseries.<alias>.window_size`: Sliding window width in seconds
- `timeseries.<alias>.slide_size`: Sliding window increment in seconds
- `timeseries.<alias>.alert_ratio`: The threshold to exceed before writing
- `timeseries.<alias>.time_based`: Optional field, whose presence is used to indicate if the metric being computed should be the number of occurences or if the metric should be used as a rate per unit time.
		
#### File Semantics
Say our input is weblog data, where fields are tab-seperated and this is the structure of each line:
	
	<unix_timestamp>	<ip-address>	<response-size-bytes>
	
##### Example 1: High User-Acitivity
If we want to find all (time, ip-address) pairs when the ip-address was mentioned in the logs 10 times more in 1hr than its average over course of a day, we would use the following config file:

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
	# of occurences over the window
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
	  /home/web-log-path /home/user/example-1 config.properties
	  
	# the following directory has all relevant points
	$ hadoop fs -ls /home/user/example-1/high_user_activity 

