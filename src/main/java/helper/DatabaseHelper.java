package helper;

import java.sql.*;
import model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class DatabaseHelper {
	private Connection db;
	public DatabaseHelper(Connection db){
		this.db = db;
	}
	
	public Location getLocation(int idLocation) throws Exception{
		String sql = String.format("SELECT * FROM location WHERE id_location = %d", idLocation);
		Location location = null;

		try {
			Statement stmt = db.createStatement();
	        ResultSet rows = stmt.executeQuery(sql);
	        rows.next();
	        location = getLocationRow(rows);
	        rows.close();
		} catch (Exception e){
			throw e;
		}
		
		return location;
	}
	
	public List<Location> getAllLocation() throws Exception{
		List<Location> locations = new ArrayList<>();
    	try {
            String sql = "SELECT * FROM location";
            Statement stmt = db.createStatement();
            ResultSet rows = stmt.executeQuery(sql);
            
            while (rows.next()){
            	Location location = getLocationRow(rows);
            	locations.add(location);
            }
            
            rows.close();
    
    	} catch (Exception e){
    		throw e;
    	}
    	
    	return locations;
	}
	
	public List<Location> getAllLocation(int numOfLocation) throws Exception{
		List<Location> locations = new ArrayList<>();
    	try {
            String sql = "SELECT * FROM location ORDER BY id_location LIMIT " + numOfLocation;
            Statement stmt = db.createStatement(); 
            ResultSet rows = stmt.executeQuery(sql);
            
            while (rows.next()){
            	Location location = getLocationRow(rows);
            	locations.add(location);
            }
            
            rows.close();
    
    	} catch (Exception e){
    		throw e;
    	}
    	
    	return locations;
	}
	
	public WeatherObservation getWeatherObservation(Location location, int size) throws Exception{
		int idLocation = location.getIdLocation();
		String sql = String.format("SELECT * FROM weather_observation WHERE id_location = %d  ORDER BY datetime_observation ASC LIMIT %d", idLocation, size);
		
		List<ObservationValue> temperatureObservations = new ArrayList<>();
		List<ObservationValue> humidityObservations = new ArrayList<>();
		List<ObservationValue> pressureObservations = new ArrayList<>();
		List<ObservationValue> windSpeedObservations = new ArrayList<>();
		List<ObservationValue> windDirectionObservations = new ArrayList<>();
		
		try {
			Statement stmt = db.createStatement();
			ResultSet rows = stmt.executeQuery(sql);
			
			while (rows.next()){
				Date datetime= rows.getDate("datetime_observation");
				double temperature = rows.getDouble("temperature");
				double humidity = rows.getDouble("humidity");
				double pressure = rows.getDouble("pressure");
				double windSpeed = rows.getDouble("wind_speed");
				double windDirection = rows.getDouble("wind_direction");
				
				ObservationValue temperatureObservation = new ObservationValue(datetime, temperature);
				ObservationValue humidityObservation = new ObservationValue(datetime, humidity);
				ObservationValue pressureObservation = new ObservationValue(datetime, pressure);
				ObservationValue windSpeedObservation = new ObservationValue(datetime, windSpeed);
				ObservationValue windDirectionObservation = new ObservationValue(datetime, windDirection);
				
				temperatureObservations.add(temperatureObservation);
				humidityObservations.add(humidityObservation);
				pressureObservations.add(pressureObservation);
				windSpeedObservations.add(windSpeedObservation);
				windDirectionObservations.add(windDirectionObservation);
			}
			
			rows.close();
		} catch (Exception e){
			throw e;
		}
		
		return new WeatherObservation(location, 
				temperatureObservations, humidityObservations, pressureObservations, 
				windSpeedObservations, windDirectionObservations);
		
	}
	
	private Location getLocationRow(ResultSet row) throws SQLException{
		int id = row.getInt(1);
     	String city = row.getString(2);
     	String country = row.getString(3);
     	double latitude = row.getDouble("latitude");
     	double longitude = row.getDouble("longitude");
     	String timezone = row.getString("timezone");
     	int timezoneValue = row.getInt("timezone_value");
     	
     	return new Location(id, city, country, latitude, longitude, timezone, timezoneValue);
	}
	
	public HashMap<WeatherParameter, ModelOrder> getModelOrderMap(Location location, int size) throws Exception{
		int idLocation = location.getIdLocation();
		String sql = String.format("SELECT * FROM model_order WHERE id_location = %d AND size = %d", idLocation, size);
		
		Statement stmt = db.createStatement();
		ResultSet rows = stmt.executeQuery(sql);
		
		HashMap<WeatherParameter, ModelOrder> modelOrderMap = new HashMap<>();
		
		while (rows.next()){
			int p = rows.getInt("p");
			int d = rows.getInt("d");
			int q = rows.getInt("q");
			WeatherParameter weatherParameter = WeatherParameter.valueOf(rows.getString("weather_parameter"));
			ModelOrder modelOrder = new ModelOrder(p, d, q);
			
			modelOrderMap.put(weatherParameter, modelOrder);
			
		}
		
		rows.close();
		
		return modelOrderMap;
		
	}
}
