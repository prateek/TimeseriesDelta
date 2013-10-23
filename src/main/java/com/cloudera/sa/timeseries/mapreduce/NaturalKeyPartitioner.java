package com.cloudera.sa.timeseries.mapreduce;

import org.apache.hadoop.mapreduce.Partitioner;

public class NaturalKeyPartitioner extends Partitioner<TimeseriesKey, TimeseriesDataPoint> {

	@Override
	public int getPartition(TimeseriesKey key, TimeseriesDataPoint value,
			int numPartitions) {
		return Math.abs(17*key.getGroup().hashCode() + key.getAlias().hashCode() ) % numPartitions;
	}

}
