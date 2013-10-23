package com.cloudera.sa.timeseries.mapreduce.config;
import java.lang.Exception;

public class TimeseriesConfigException extends Exception
{
  public TimeseriesConfigException(String e)
  {
    super(e);
  }
}
