package com.cloudera.sa.timeseries.mapreduce;

public class AveragePair {

	protected long numValues;
	protected double currentSum;

  public AveragePair() {
    this.numValues  = 0;
    this.currentSum = 0.0;
  }

  public AveragePair( double v ) {
    this.numValues  = 1;
    this.currentSum = v;
  }

  public void addValue( double val ) 
  {
    this.currentSum = this.currentSum + val;
    this.numValues  = this.numValues + 1;
  }

  public double getSum() 
  {
    return this.currentSum;
  }

  public double getNumber() 
  {
    return this.numValues;
  }

}
