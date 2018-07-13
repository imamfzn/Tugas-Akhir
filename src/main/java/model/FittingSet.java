package model;

import com.github.signaflo.timeseries.TimeSeries;

public class FittingSet {
	private Location location;
	private WeatherParameter weatherParameter;
	private TimeSeries series;
	private ModelOrder modelOrder;
	
	public FittingSet(Location location, WeatherParameter weatherParameter, double[] series, ModelOrder modelOrder){
		this.location = location;
		this.weatherParameter = weatherParameter;
		this.series = TimeSeries.from(series);
		this.modelOrder = modelOrder;
	}

	public Location getLocation() {
		return location;
	}

	public WeatherParameter getWeatherParameter() {
		return weatherParameter;
	}

	public TimeSeries getSeries() {
		return series;
	}

	public ModelOrder getModelOrder() {
		return modelOrder;
	}
	
	@Override
	public String toString(){
		return String.format("%s_%s", location.getCity().toUpperCase(), weatherParameter.toString().toUpperCase());
	}
}
