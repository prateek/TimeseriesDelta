package com.cloudera.sa.timeseries.mapreduce;

import java.io.IOException;
import java.lang.Double;
import java.lang.NumberFormatException;

import org.apache.log4j.Logger;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import com.cloudera.sa.timeseries.mapreduce.config.TimeseriesConfig;
import com.cloudera.sa.timeseries.mapreduce.config.TimeseriesAlias;

public class RollingAverageMapper extends Mapper<LongWritable, Text, TimeseriesKey, TimeseriesDataPoint>
{
  private static final Logger LOG = Logger.getLogger(RollingAverageMapper.class);
  protected TimeseriesConfig tConfig;

  @Override
  protected void setup(Context context) throws IOException
  {
    tConfig = new TimeseriesConfig( context.getConfiguration() );
  }

  @Override
  public void map(LongWritable unusedKey, Text value, Context context) 
  throws IOException, InterruptedException
  {
    String stringValue = new String(value.getBytes(), 0, value.getLength());
    String delimiter   = tConfig.getDelimiter();
    String[] tokens    = stringValue.split(delimiter);

    int timeIDX      = tConfig.getTimeColumn();
    String[] aliases = tConfig.getAliases();

    long tStamp = 0;
    try {
      String time = tokens[timeIDX];
      tStamp      = (long) (Double.parseDouble(time));
    } catch ( NumberFormatException e ) {
    }

    for(String alias: aliases)
    {
      TimeseriesAlias tAlias  = tConfig.getAlias( alias );

      TimeseriesKey key = new TimeseriesKey();
      int groupIdx      = tAlias.getCategoryColumn();
      String category   = tokens[groupIdx];
      key.set( category, alias, tStamp );

      int valueIdx            = tAlias.getMetricType();
      TimeseriesDataPoint val = new TimeseriesDataPoint();
      val.fValue              = ( valueIdx == TimeseriesAlias.COUNT_METRIC )
                              ?  1.0 
                              :  Double.parseDouble( tokens[valueIdx] );
      val.lDateTime = tStamp;

      context.write(key, val);
    }
  }    
}
