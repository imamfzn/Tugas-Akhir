package model;
import java.util.List;

public class WeatherObservation {
	private Location location;
	private List<ObservationValue> temperatureObservation;
	private List<ObservationValue> humidityObservation;
	private List<ObservationValue> pressureObservation;
	private List<ObservationValue> windSpeedObservation;
	private List<ObservationValue> windDirectionObservation;
	
	private double[] temperatureValues;
	private double[] humidityValues;
	private double[] pressureValues;
	private double[] windSpeedValues;
	private double[] windDirectionValues;
	
	public WeatherObservation(Location location,
			List<ObservationValue> temperatureObservation, List<ObservationValue> humidityObservation, 
			List<ObservationValue> pressureObservation, List<ObservationValue> windSpeedObservation, 
			List<ObservationValue> windDirectionObservation){
		
		this.temperatureObservation = temperatureObservation;
		this.humidityObservation = humidityObservation;
		this.pressureObservation = pressureObservation;
		this.windSpeedObservation = windSpeedObservation;
		this.windDirectionObservation = windDirectionObservation;
		
		this.temperatureValues = toArrayOfDouble(temperatureObservation);
		this.humidityValues = toArrayOfDouble(humidityObservation);
		this.pressureValues = toArrayOfDouble(pressureObservation);
		this.windSpeedValues = toArrayOfDouble(windSpeedObservation);
		this.windDirectionValues = toArrayOfDouble(windDirectionObservation);
	}
	
	private double[] toArrayOfDouble(List<ObservationValue> observationValues){
		double[] values = new double[observationValues.size()];
		int index = 0;
		
		for (ObservationValue observationValue : observationValues){
			values[index++] = observationValue.getValue();
		}
		
		return values;
	}

	public Location getLocation() {
		return location;
	}

	public List<ObservationValue> getTemperatureObservation() {
		return temperatureObservation;
	}

	public List<ObservationValue> getHumidityObservation() {
		return humidityObservation;
	}

	public List<ObservationValue> getPressureObservation() {
		return pressureObservation;
	}

	public List<ObservationValue> getWindSpeedObservation() {
		return windSpeedObservation;
	}

	public List<ObservationValue> getWindDirectionObservation() {
		return windDirectionObservation;
	}

	public double[] getTemperatureValues() {
		return temperatureValues;
	}

	public double[] getHumidityValues() {
		return humidityValues;
	}

	public double[] getPressureValues() {
		return pressureValues;
	}

	public double[] getWindSpeedValues() {
		return windSpeedValues;
	}

	public double[] getWindDirectionValues() {
		return windDirectionValues;
	}
	
}
