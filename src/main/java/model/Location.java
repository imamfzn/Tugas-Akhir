package model;

public class Location {
	private int idLocation;
	private String city;
	private String country;
	private double latitude;
	private double longitude;
	private String timezone;
	private int timezoneValue;

	public Location(int idLocation, String city, String country, double latitude, double longitude, String timezone, int timezoneValue){
		this.idLocation = idLocation;
		this.city = city;
		this.country = country;
		this.latitude = latitude;
		this.longitude = longitude;
		this.timezone = timezone;
		this.timezoneValue = timezoneValue;
	}

	public int getIdLocation() {
		return idLocation;
	}
	
	public String getLocationName(){
		return city;
	}
	
	@Override
	public String toString(){
		return String.format("%s %s %f %f", city, country, latitude, longitude);
	}

	public String getCity() {
		return city;
	}

	public String getCountry() {
		return country;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public String getTimezone() {
		return timezone;
	}

	public int getTimezoneValue() {
		return timezoneValue;
	}
}
