package model;
import java.sql.Date;

public class ObservationValue {
	private Date datetime;
	private double value;
	
	public ObservationValue(Date datetime, double value){
		this.datetime = datetime;
		this.value = value;
	}

	public Date getDatetime() {
		return datetime;
	}

	public double getValue() {
		return value;
	}
	
	@Override
	public String toString(){
		return datetime + " " + value;
	}

}
