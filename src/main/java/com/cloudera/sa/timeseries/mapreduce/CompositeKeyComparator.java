package com.cloudera.sa.timeseries.mapreduce;

import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;

public class CompositeKeyComparator extends WritableComparator {

	protected CompositeKeyComparator() {
		super(TimeseriesKey.class, true);
	}

	@Override
	public int compare(WritableComparable w1, WritableComparable w2) {

		TimeseriesKey ip1 = (TimeseriesKey) w1;
		TimeseriesKey ip2 = (TimeseriesKey) w2;

		int cmp = ip1.getGroup().compareTo(ip2.getGroup());
		if (cmp != 0) {
			return cmp;
		}

		cmp = ip1.getAlias().compareTo(ip2.getAlias());
		if (cmp != 0) {
			return cmp;
		}

		return ip1.getTimestamp() == ip2.getTimestamp() ? 0 : (ip1
				.getTimestamp() < ip2.getTimestamp() ? -1 : 1);

	}

}
