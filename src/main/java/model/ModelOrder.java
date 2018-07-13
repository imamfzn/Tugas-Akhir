package model;

import com.github.signaflo.timeseries.model.arima.ArimaOrder;

public class ModelOrder {
	private int p;
	private int d;
	private int q;
	private ArimaOrder order;
	private WeatherParameter weather;
	private Location location;
	
	public ModelOrder(Location location, WeatherParameter weather, int p, int d, int q){
		this.weather = weather;
		this.p = p;
		this.d = d;
		this.q = q;
		this.order = ArimaOrder.order(p, d, q);
	}
	
	public ModelOrder(int p, int d, int q){
		this.p = p;
		this.d = d;
		this.q = q;
		this.order = ArimaOrder.order(p, d, q);
	}

	public int getP() {
		return p;
	}

	public int getD() {
		return d;
	}

	public int getQ() {
		return q;
	}
	
	public ArimaOrder getArimaOrder(){
		return order;
	}
	
	public Location getLocation(){
		return location;
	}
	
	public WeatherParameter getWeatherParameter() {
		return weather;
	}
	
}
