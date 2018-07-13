package controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.github.signaflo.timeseries.model.arima.Arima;
import com.github.signaflo.timeseries.model.arima.ArimaOrder;


import connector.*;
import helper.*;
import model.*;

public class PreExperimen {
	private static DatabaseHelper dbHelper;
	private static Argument argument;
	
	public static void main(String[] args) throws Exception {
		argument = new Argument(args);
		MysqlConnector connector = new MysqlConnector();
        dbHelper = new DatabaseHelper(connector.getConnection());

        if (args.length > 1){
        	if (args[1].contains("sample")){
        		runSampleSizeExperimen();
        	} else {
        		runTimePreExperimen();
        	}
        }
	}
	
	private static void runSampleSizeExperimen() throws Exception {
		int oneDay = 24;
	    int oneMonth = 30 * oneDay;
		int totalSize = 6 * oneMonth + 24;
		int sampleSize = argument.getSize();
		
		List<Location> locations = dbHelper.getAllLocation();
		
		for (Location location : locations){
          WeatherObservation weatherObservation = dbHelper.getWeatherObservation(location, totalSize);
          List<TrainingSet> trainingSets = new ArrayList<>();

          trainingSets.add(new TrainingSet(location, WeatherParameter.TEMPERATURE, weatherObservation.getTemperatureValues(), sampleSize));
          trainingSets.add(new TrainingSet(location, WeatherParameter.HUMIDITY, weatherObservation.getHumidityValues(), sampleSize));
          trainingSets.add(new TrainingSet(location, WeatherParameter.PRESSURE, weatherObservation.getPressureValues(), sampleSize));
          trainingSets.add(new TrainingSet(location, WeatherParameter.WIND_SPEED, weatherObservation.getWindSpeedValues(), sampleSize));
          trainingSets.add(new TrainingSet(location, WeatherParameter.WIND_DIRECTION, weatherObservation.getWindDirectionValues(), sampleSize));
    
          for (TrainingSet trainingSet: trainingSets){
        	  train(trainingSet); 
          }
      }
		
	}
	
	private static void runTimePreExperimen() throws Exception{
		System.out.printf("%s Running pre-experimen using %d data\n", 
				 new Date().toString(), argument.getSize());
        
        int size = argument.getSize();
		List<Location> locations = dbHelper.getAllLocation();
		
		for (Location location : locations){
			WeatherObservation weatherObservation = dbHelper.getWeatherObservation(location, size);
			List<TrainingSet> trainingSets = new ArrayList<>();
			
			trainingSets.add(new TrainingSet(location, WeatherParameter.TEMPERATURE, weatherObservation.getTemperatureValues()));
            trainingSets.add(new TrainingSet(location, WeatherParameter.HUMIDITY, weatherObservation.getHumidityValues()));
            trainingSets.add(new TrainingSet(location, WeatherParameter.PRESSURE, weatherObservation.getPressureValues()));
            trainingSets.add(new TrainingSet(location, WeatherParameter.WIND_SPEED, weatherObservation.getWindSpeedValues()));
            trainingSets.add(new TrainingSet(location, WeatherParameter.WIND_DIRECTION, weatherObservation.getWindDirectionValues()));
            
            for (TrainingSet trainingSet: trainingSets){
            	train(trainingSet);    
            }
		}
		
		 System.out.printf("%s Finished\n", new Date().toString());
	}
	
	private static void train(TrainingSet trainingSet){
		long start = System.currentTimeMillis();
		int d = Statistic.getDifferencingValue(trainingSet.getSample());
    	trainingSet.setDifferencingValue(d);
    	long timeDiff = System.currentTimeMillis() - start;
    	
    	long startOrder = System.currentTimeMillis();
    	double[] acf = Statistic.getACF(trainingSet.getSample(), 20);
		double[] pacf = Statistic.getPACF(acf);
		int pMax = Statistic.getAROrder(pacf, trainingSet.getSampleSize());
		int qMax = Statistic.getMAOrder(acf, trainingSet.getSampleSize());
		long timeOrder = System.currentTimeMillis() - startOrder;
		
		long startGen = System.currentTimeMillis();
		List<ArimaOrder> arimaOrders = Statistic.getAllModelOrder(pMax, d, qMax);
		long timeGen = System.currentTimeMillis() - startGen;
		
		long startBest = System.currentTimeMillis();
		ArimaOrder bestOrder = arimaOrders.get(0);
		arimaOrders.remove(0);
	
		Arima bestModel = Arima.model(trainingSet.getSeries(), bestOrder);
		
		for (ArimaOrder order : arimaOrders){
			Arima model = Arima.model(trainingSet.getSeries(), order);
			
			if (model.aic() < bestModel.aic()){
				bestModel = model;
			}
		}
		
		long timeBest = System.currentTimeMillis() - startBest;
		
		double[] predicted = bestModel.forecast(24).pointEstimates().asArray();
    	double rmse = Statistic.getRMSE(trainingSet.getDataTest(), predicted);
    	long time = System.currentTimeMillis() - start;
    	
    	System.out.printf("%s\t%s\t%s\t%.2f\t%d\t%d\t%d\t%d\t%d\n", new Date().toString(), trainingSet.toString(), 
    			Statistic.toArimaOrderString(bestModel.order().toString()), rmse, 
    			time, timeDiff, timeOrder, timeGen, timeBest);

	}

}
