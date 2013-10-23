package com.cloudera.sa.timeseries.mapreduce.config;
import java.util.Properties;
import java.io.IOException;

public class TimeseriesAlias
{
  public static final int COUNT_METRIC = -1;

  protected final Properties props;
  protected final String aliasName;

  // the grouping category, eg: ip address column idx
  protected static final String CATEGORY_COLUMN = "timeseries.%s.category_column";

  // AVG | AVG_DELTA
  protected static final String COMPUTE_TYPE    = "timeseries.%s.compute_type";

  // COUNT | <int-column to compute value for>
  protected static final String METRIC_TYPE     = "timeseries.%s.metric_type";

  // REQUIRED
  protected static final String WINDOW_SIZE     = "timeseries.%s.window_size";
  protected static final String SLIDE_SIZE      = "timeseries.%s.slide_size";
  protected static final String ALERT_RATIO     = "timeseries.%s.alert_ratio";
  
  // NOT REQUIRED 
  protected static final String TIME_BASED = "timeseries.%s.time_based"; // presence indicates metric should be computed based on time

  protected static final String[] RequiredFields = {
    CATEGORY_COLUMN, COMPUTE_TYPE, METRIC_TYPE, WINDOW_SIZE, SLIDE_SIZE, ALERT_RATIO
  };

  public TimeseriesAlias( String name, Properties properties )
  {
    props = properties;
    aliasName = name;
  }

  public boolean isTimebasedMetric() {
    String field = String.format( TIME_BASED, aliasName );
    return props.containsKey( field );
  }

  public int getCategoryColumn() {
    String field = String.format( CATEGORY_COLUMN, aliasName );
    return Integer.parseInt( props.getProperty( field ) );
  }

  public int getWindowSize() {
    String field = String.format( WINDOW_SIZE, aliasName );
    return Integer.parseInt( props.getProperty( field ) );
  }

  public int getSlideSize() {
    String field = String.format( SLIDE_SIZE, aliasName );
    return Integer.parseInt( props.getProperty( field ) );
  }

  public double getAlertRatio() {
    String field = String.format( ALERT_RATIO, aliasName );
    return Double.parseDouble( props.getProperty( field ) );
  }

  public String getComputeType() {
    String field = String.format( COMPUTE_TYPE, aliasName );
    return props.getProperty( field );
  }

  public int getMetricType() {
    String field = String.format( METRIC_TYPE, aliasName );
    String prop  = props.getProperty( field );
    if( prop.equals( "COUNT" ) ) 
      return COUNT_METRIC;
    else 
      return Integer.parseInt( props.getProperty( field ) );
  }

  public void validate() throws IOException
  {
    for(int i=0; i<RequiredFields.length; ++i)
    {
      String _field = RequiredFields[i];
      String field = String.format( RequiredFields[i], aliasName );

      if( ! props.containsKey(field) )
      {
        throw new IOException( "Missing alias field: " + field + " for alias: " + aliasName );      
      }

      if( _field.equals( WINDOW_SIZE ) || 
          _field.equals( SLIDE_SIZE )  ||
          _field.equals( CATEGORY_COLUMN ) )
      {
        Integer.parseInt( props.getProperty(field) );
      }

      if( _field.equals( ALERT_RATIO ) )
      {
        Double.parseDouble( props.getProperty(field) );
      }

      if( _field.equals( METRIC_TYPE ) )
      {
        if( !props.getProperty( field ).equals( "COUNT" ) )
        {
          Integer.parseInt( props.getProperty(field) );
        }
      }

      if( _field.equals( COMPUTE_TYPE ) )
      {
        if( !props.getProperty( field ).equals( "AVG" ) && 
            !props.getProperty( field ).equals( "AVG_DELTA" ) )
        {
          throw new IOException( "Bad compute.type for " + aliasName );
        }
      }
    }
  }

}
