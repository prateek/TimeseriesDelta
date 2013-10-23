package com.cloudera.sa.timeseries.mapreduce;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;

/**
 * 
 * TimeseriesKey
 * 
 * The key type for this Map Reduce application.
 * 
 * This particular key has two parts, the String Group and long Timestamp.
 * 
 */
public class TimeseriesKey implements WritableComparable<TimeseriesKey> {

	private String Group   =  "";
	private String Alias   =  "";
	private long Timestamp =  0;

	public void set(String group, String alias, long lTS) {

		this.Group =  group;
		this.Alias =  alias;
		this.Timestamp =  lTS;

	}

	public String getGroup() {
		return this.Group;
	}

	public String getAlias() {
		return this.Alias;
	}

	public long getTimestamp() {
		return this.Timestamp;
	}

	@Override
	public void readFields(DataInput in) throws IOException {

		this.Group     =  in.readUTF();
		this.Alias        =  in.readUTF();
		this.Timestamp =  in.readLong();

	}

	@Override
	public void write(DataOutput out) throws IOException {

		out.writeUTF(Group);
		out.writeUTF(Alias);
		out.writeLong(this.Timestamp);
	}

	@Override
	public int compareTo(TimeseriesKey other) {

		if (this.Group.compareTo(other.Group) != 0) {
			return this.Group.compareTo(other.Group);
		} else if (this.Alias.compareTo(other.Alias) != 0) {
			return this.Alias.compareTo(other.Alias);
		} else if (this.Timestamp != other.Timestamp) {
			return Timestamp < other.Timestamp ? -1 : 1;
		} else {
			return 0;
		}

	}

	public static class TimeSeriesKeyComparator extends WritableComparator {
		public TimeSeriesKeyComparator() {
			super(TimeseriesKey.class);
		}

		public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
			return compareBytes(b1, s1, l1, b2, s2, l2);
		}
	}

	static { // register this comparator
		WritableComparator.define(TimeseriesKey.class,
				new TimeSeriesKeyComparator());
	}

}
