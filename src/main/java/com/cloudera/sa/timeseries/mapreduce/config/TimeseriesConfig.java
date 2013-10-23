package com.cloudera.sa.timeseries.mapreduce.config;

import java.util.Map;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.lang.Integer;

import org.apache.hadoop.conf.Configuration;

public class TimeseriesConfig 
{
  protected Properties props;

  public static final String PREFIX          = "timeseries";
  public static final String DELIMITER       = "timeseries.file.delimiter";
  public static final String TIME_COLUMN     = "timeseries.time.column";
  public static final String COMPUTE_ALIASES = "timeseries.compute.aliases";

  protected static final String[] RequiredFields = {
    DELIMITER, TIME_COLUMN, COMPUTE_ALIASES
  };

  public String getDelimiter() { return props.getProperty( DELIMITER ); }

  public int getTimeColumn() { 
    return Integer.parseInt( props.getProperty( TIME_COLUMN ) ); 
  }

  public String getAliasString() { 
    return props.getProperty( COMPUTE_ALIASES );
  }

  public String[] getAliases() { 
    String[] aliases = props.getProperty( COMPUTE_ALIASES ).split(","); 
    for(int i=0; i<aliases.length; ++i) 
    {
      aliases[i] = aliases[i].trim();
    }
    return aliases;
  }

  public TimeseriesAlias getAlias( String alias ) {
    return new TimeseriesAlias( alias, props );
  }

  public TimeseriesConfig(Configuration conf) throws IOException
  {
    props = new Properties();
    Map< String, String > timeseriesEntires = conf.getValByRegex(PREFIX);
    for( Map.Entry<String, String> entry : timeseriesEntires.entrySet() )
    {
      props.setProperty( entry.getKey(), entry.getValue() );
    }

    validate();
    validateAliases();
  }

  public TimeseriesConfig(String propFile) throws IOException
  {
    props = new Properties();
    FileInputStream in = new FileInputStream(propFile);
    props.load(in);
    in.close();

    validate();
    validateAliases();
  }

  public Properties getProps() 
  {
    return props;
  }

  protected void validateAliases() throws IOException
  {
    String[] aliases = getAliases();
    for(String alias: aliases)
    {
      TimeseriesAlias a = new TimeseriesAlias( alias, props );
      a.validate();
    }
  }

  protected void validate() throws IOException
  {
    for(int i=0; i<RequiredFields.length; ++i)
    {
      if( ! props.containsKey( RequiredFields[i] ) )
      {
        throw new IOException( "Missing field: " + RequiredFields[i] );      
      }
    }
  }
  
  public String toString()
  {
    return props.toString();
  }
}
