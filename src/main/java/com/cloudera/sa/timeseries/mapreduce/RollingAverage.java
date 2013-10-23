package com.cloudera.sa.timeseries.mapreduce;

import java.util.Properties;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import com.cloudera.sa.timeseries.mapreduce.config.TimeseriesConfig;

public class RollingAverage extends Configured implements Tool {

	private Configuration config = new Configuration();
	
	/**
	 * Tool interface
	 */
	public Configuration getConf() 
	{
		return config;
	}

	/**
	 * Tool interface
	 */
	public void setConf(Configuration config) 
	{
		this.config = config;
	}

	public int run(String[] args) throws Exception 
	{	
		Job job = Job.getInstance(config);
		job.setJarByClass(RollingAverage.class);
		job.setJobName("RollingAverage");
		
    if( args.length != 4 ) {
      System.out.printf("Usage: %s <hdfs-input-dir> <hdfs-output-dir> <local-config-file>\n" , args[0]);
      return -1;
    }

		job.setOutputFormatClass(TextOutputFormat.class);
		job.setMapOutputKeyClass(TimeseriesKey.class);
		job.setMapOutputValueClass(TimeseriesDataPoint.class);
		
		job.setMapperClass(RollingAverageMapper.class);
		job.setReducerClass(RollingAverageReducer.class);

		job.setPartitionerClass(NaturalKeyPartitioner.class);
		job.setSortComparatorClass(CompositeKeyComparator.class);
    job.setGroupingComparatorClass(NaturalKeyGroupingComparator.class);

		Path inputPath = new Path(args[1]);
		MultipleInputs.addInputPath(job, inputPath, TextInputFormat.class, RollingAverageMapper.class);
		
		TextOutputFormat.setOutputPath(job, new Path(args[2]));
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(Text.class);
	  
    loadJobConfig( job, args[3] );  
		return (job.waitForCompletion(true) ? 0 : 1);
	}
	
  public static void loadJobConfig(Job job, String filename) throws Exception
  {
    TimeseriesConfig tc = new TimeseriesConfig( filename );
    Properties p = tc.getProps();
    
    Set< String > propNames = p.stringPropertyNames();
    for(String name: propNames)
    {
			job.getConfiguration().set(name, p.getProperty(name) );
    }
  }
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception
	{
		RollingAverage processor = new RollingAverage();
		String[] otherArgs = new GenericOptionsParser(processor.getConf(), args).getRemainingArgs();
		
		System.exit(ToolRunner.run(processor.getConf(), processor, otherArgs));
	}

}
