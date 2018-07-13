package test;

import java.util.ArrayList;
import java.util.List;

import com.github.signaflo.timeseries.model.arima.Arima;
import com.github.signaflo.timeseries.model.arima.ArimaOrder;

import connector.*;
import helper.*;
import model.*;

public class FunctionalTest {
	private final static int SIZE = 720;

	public static void main(String[] args) throws Exception {
		MysqlConnector connector = new MysqlConnector();
		DatabaseHelper dbHelper = new DatabaseHelper(connector.getConnection());
		
		Location location = dbHelper.getLocation(26);
		WeatherObservation weatherObservation = dbHelper.getWeatherObservation(location, SIZE);		
		List<TrainingSet> trainingSets = new ArrayList<>();

		trainingSets.add(
				new TrainingSet(location, WeatherParameter.TEMPERATURE, weatherObservation.getTemperatureValues()));
		trainingSets
				.add(new TrainingSet(location, WeatherParameter.HUMIDITY, weatherObservation.getHumidityValues()));
		trainingSets
				.add(new TrainingSet(location, WeatherParameter.PRESSURE, weatherObservation.getPressureValues()));
		trainingSets.add(
				new TrainingSet(location, WeatherParameter.WIND_SPEED, weatherObservation.getWindSpeedValues()));
		trainingSets.add(new TrainingSet(location, WeatherParameter.WIND_DIRECTION,
				weatherObservation.getWindDirectionValues()));
		
		System.out.printf("Test using %d data (3 months) for each weather parameters in %s\n\n", SIZE, location.getCity());
		
		for (TrainingSet trainingSet : trainingSets) {
			System.out.printf("Running Test on %s\n", trainingSet.toString());
			System.out.println("ADF Test P-VALUE : -3.45");
			System.out.printf("Sample size: %d\n", trainingSet.getSampleSize());
			System.out.printf("ACF & PACF significant value : %.2f\n\n", Statistic.getACFSignificantValue(trainingSet.getSampleSize()));
	
			System.out.printf("Stationary identifer:\n");
			int d = Statistic.getDifferencingValue(trainingSet.getSample());
			trainingSet.setDifferencingValue(d);
			System.out.printf("d : %d\n\n", d);
			
			System.out.println("Order identifier:");
			double[] acf = Statistic.getACF(trainingSet.getSample(), 20);
			double[] pacf = Statistic.getPACF(acf);
			int pMax = Statistic.getAROrder(pacf, trainingSet.getSampleSize());
			int qMax = Statistic.getMAOrder(acf, trainingSet.getSampleSize());
			
			
			System.out.println("PACF values:");
			for (int i = 0 ; i < 11 ; i++){
				System.out.printf("Lag %d: %.2f\n", i, pacf[i]);
			}
			
			System.out.println();
			
			System.out.println("ACF values:");			
			for (int i = 0 ; i < 11 ; i++){
				System.out.printf("Lag %d: %.2f\n", i, acf[i]);
			}
			System.out.println();
			
			System.out.printf("max p : %d\n", pMax);
			System.out.printf("max q : %d\n", qMax);
			System.out.printf("num of model order : %d\n\n", (pMax+1)*(qMax+1) -1);
			
			System.out.println("Generate all model order:");
			List<ArimaOrder> arimaOrders = Statistic.getAllModelOrder(pMax, d, qMax);
			for (int i = 0 ; i < arimaOrders.size() ; i++){
				ArimaOrder order = arimaOrders.get(i);
				System.out.printf("%d. %s\n", i+1, Statistic.toArimaOrderString(order.toString()));
			}
			
			System.out.println();
			
			System.out.println("AIC for each model order:");
			
			ArimaOrder bestOrder = arimaOrders.get(0);
			arimaOrders.remove(0);

			Arima bestModel = Arima.model(trainingSet.getSeries(), bestOrder);
			
			int numOrder = 1;

			System.out.printf("%d. %s : %.4f\n",numOrder++, Statistic.toArimaOrderString(bestModel.order().toString()), bestModel.aic());

			for (ArimaOrder order : arimaOrders) {
				Arima model = Arima.model(trainingSet.getSeries(), order);
				System.out.printf("%d. %s : %.4f\n",numOrder++, Statistic.toArimaOrderString(model.order().toString()), model.aic());

				if (model.aic() < bestModel.aic()) {
					bestModel = model;
				}
			}
			
			System.out.printf("Best model: %s\n\n", Statistic.toArimaOrderString(bestModel.order().toString()));
		}
	}
	

}
