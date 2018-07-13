package model;
import helper.Statistic;
import com.github.signaflo.timeseries.*;
import com.github.signaflo.timeseries.model.arima.*;

public class TrainingSet {
	private Location location;
	private WeatherParameter weather;
	private TimeSeries series;
	private ArimaOrder order;
	private double[] sample;
	private double[] dataTest;
	private int d;
	private int pMax;
	private int qMax;
	
	public TrainingSet(Location location, WeatherParameter weather, double[] series){
		this.location = location;
		this.weather = weather;
		double[] seriesTrain = Statistic.get24HourDataTrain(series);
		this.series = TimeSeries.from(seriesTrain);
		this.dataTest = Statistic.get24HourDataTest(series);
		this.sample = Statistic.getSampleData(seriesTrain);
	}
	
	public TrainingSet(Location location, WeatherParameter weather, double[] series, int sampleSize){
		this.location = location;
		this.weather = weather;
		double[] seriesTrain = Statistic.get24HourDataTrain(series);
		this.series = TimeSeries.from(seriesTrain);
		this.dataTest = Statistic.get24HourDataTest(series);
		this.sample = Statistic.getSampleDataWithSize(seriesTrain, sampleSize);
	}
	
	public Location getLocation(){
		return location;
	}
	
	public WeatherParameter getWeatherParameter(){
		return weather;
	}
	
	public double[] getSample(){
		return this.sample;
	}
	
	public void setDifferencingValue(int d){
		this.d = d;
		this.sample = TimeSeries.from(sample).difference(1, d).asArray();
	}
	
	public void setIdentifiedARMA(int pMax, int qMax){
		this.pMax = pMax;
		this.qMax = qMax;
	}
	
	public int[] getIdentifiedOrder(){
		int[] order = {pMax, d, qMax};

		return order;		
	}
	
	public void setOrder(ArimaOrder order){
		this.order = order;
	}
	
	public ArimaOrder getOrder(){
		return this.order;
	}
	
	public int getDifferencingValue(){
		return this.d;
	}
	
	public TimeSeries getSeries(){
		return this.series;
	}
	
	public double[] getDataTest(){
		return this.dataTest;
	}
	
	public int getDataSize(){
		return this.series.size() - this.d;
	}
	
	public int getSampleSize(){
		return this.sample.length;
	}
	
	@Override
	public String toString(){
		return String.format("%s_%s", location.getCity().toUpperCase(), weather.toString());
	}
	
	public String toString2(){
		return String.format("%s\t%s", location.getCity().toUpperCase(), weather.toString());
	}

	public int getD() {
		return d;
	}

	public int getPMax() {
		return pMax;
	}

	public int getQMax() {
		return qMax;
	}
}
