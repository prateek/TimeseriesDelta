package com.cloudera.sa.timeseries.mapreduce;

import java.io.IOException;
import java.text.ParseException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Date;
import java.lang.Long;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.log4j.Logger;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.cloudera.sa.timeseries.mapreduce.RollingAverage;
import com.cloudera.sa.timeseries.mapreduce.config.TimeseriesConfig;
import com.cloudera.sa.timeseries.mapreduce.config.TimeseriesAlias;

public class RollingAverageReducer extends Reducer<TimeseriesKey, TimeseriesDataPoint, Text, Text> {
  
	private static final Logger LOG            = Logger.getLogger(RollingAverageReducer.class);
	private static final DateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
  private static final double NUM_MSEC_DAY   = 24 * 60 * 60 * 1000;
    
  MultipleOutputs< Text, Text > multipleOutputs;

  TimeseriesConfig tConfig;
  TimeseriesAlias  tAlias;

  String currentAlias="", currentGroup="";
  String currentPath;
  boolean deltaComputeType = false;

  int windowSize   = 600 * 1000 ; // 10 mins in ms
  int slideSize    = 60  * 1000 ; // 1 min in ms
  double threshold = 5 ;          // 5x the daily average for mention

  double dailyAverage;
  AveragePair averagePair;
  long minDate, maxDate;

  HashMap< Integer, AveragePair > averageBucket;
  
  // computes the hashmap bucket for the specified date
  // assumes date and minDate are in msec since epoch
  // slideSize is in msec
  protected int computeBucketNumber( long date, long minDate, long slideSize )
  {
    if( date < minDate ) 
      return -1;

    return (int) Math.floor( ( date - minDate ) / slideSize ); 
  }

  protected int getMinBucket(long date, long minDate, long slideSize)
  {
    return computeBucketNumber( date - windowSize, minDate, slideSize);
  }

  protected int getMaxBucket(long date, long minDate, long slideSize)
  {
    return computeBucketNumber( date + windowSize, minDate, slideSize);
  }

  protected String getDate( long ts ) 
  {
    return dateFormat.format( new Date(ts) ).toString();
  }

  protected long getBucketMinDate(int bucket, long minDate, long SlideSize, long windowSize)
  {
    return minDate + (slideSize * bucket);
  }

  protected long getBucketMaxDate(int bucket, long minDate, long SlideSize, long windowSize)
  {
    return minDate + (slideSize * bucket) + windowSize;
  }


  protected void initVariables() 
  {
    dailyAverage = 0.0;
    currentAlias = "";
    currentGroup = "";
    currentPath  = "";

    averagePair = new AveragePair();

    minDate = Long.MAX_VALUE;
    maxDate = Long.MIN_VALUE;

    averageBucket = new HashMap< Integer, AveragePair >();
	}

  protected void initVariables(String alias, String group) 
  {
    initVariables();

    tAlias       = tConfig.getAlias(alias);
    currentAlias = alias;
    currentGroup = group;
    currentPath  = String.format("%s/part", alias);

    deltaComputeType = tAlias.getComputeType().indexOf("DELTA") != -1;
		slideSize  = tAlias.getSlideSize()  * 1000 ; // convert to ms
	  windowSize = tAlias.getWindowSize() * 1000 ; // convert to ms
		threshold  = tAlias.getAlertRatio();
	}

	@Override
	public void reduce(TimeseriesKey key, Iterable<TimeseriesDataPoint> values, Context context) 
  throws IOException, InterruptedException 
  {
    String kAlias = key.getAlias();
    String kGroup = key.getGroup();

    if( currentAlias != kAlias  || 
        currentGroup != kGroup) 
    {
      initVariables( kAlias, kGroup );
    }
    
		for (TimeseriesDataPoint value : values) 
    {
      long cDate = value.lDateTime;
      double v   = value.fValue;
      minDate    = Math.min( minDate, cDate );
      maxDate    = Math.max( maxDate, cDate );

      averagePair.addValue(v);
      int minBucket = getMinBucket( cDate, minDate, slideSize );
      int maxBucket = getMaxBucket( cDate, minDate, slideSize );

      for(int b=minBucket; b<=maxBucket; ++b) 
      {
        AveragePair ap;
        Integer bucketKey = new Integer( b );
        if( averageBucket.containsKey( bucketKey ) ) 
        {
          ap = averageBucket.get( bucketKey );
          ap.addValue( v );

        } else {
          ap = new AveragePair( v );
        }
        averageBucket.put( bucketKey, ap );

      }
		}

    long diffDates = Math.max( 1, maxDate - minDate );
    dailyAverage   = averagePair.getSum() * 1.0 ;
    dailyAverage  /= tAlias.isTimebasedMetric() 
                   ? diffDates 
                   : averagePair.getNumber();

    double prevAvg             = Double.MIN_VALUE;
    ArrayList<Integer> buckets = new ArrayList<Integer>( averageBucket.keySet() );
    Collections.sort( buckets );
    for( Integer bucket: buckets )
    {
      int b = bucket.intValue();
      if( b == -1 ) 
        continue;

      long minBucketDate = getBucketMinDate( b, minDate, slideSize, windowSize);
      long maxBucketDate = getBucketMaxDate( b, minDate, slideSize, windowSize);
      double avg = averageBucket.get(bucket).getSum() ;
      avg /= tAlias.isTimebasedMetric()
           ? ( maxBucketDate - minBucketDate )
           : averageBucket.get(bucket).getNumber();

      if( !deltaComputeType && avg >= threshold * dailyAverage ) 
      {
        Text alert = new Text( " breached threshold limit between " + getDate( minBucketDate) 
                   + " and " + getDate(maxBucketDate) + ", it avg was "
                   + avg + " in that period, " + dailyAverage 
                   + " is the dailyAverage "); 

        multipleOutputs.write( new Text( currentGroup ), alert, currentPath );
      }
      else if( deltaComputeType && avg >= threshold * prevAvg ) 
      {
        Text alert = new Text( " breached threshold limit between " + getDate( minBucketDate) 
                   + " and " + getDate(maxBucketDate) + ", it avg was "
                   + avg + " in that period, " + prevAvg 
                   + " is the previous window's average "); 

        multipleOutputs.write( new Text( currentGroup ), alert, currentPath );
      }

    }
	}

	@Override
	public void cleanup(Context context) throws IOException, InterruptedException
  {
      multipleOutputs.close();
	}

	@Override
	protected void setup(Context context) throws IOException, InterruptedException 
  {
		super.setup(context);

    tConfig         = new TimeseriesConfig( context.getConfiguration() );
    multipleOutputs = new MultipleOutputs<Text, Text>(context);
  }
}
