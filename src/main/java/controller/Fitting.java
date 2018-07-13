package controller;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.github.signaflo.timeseries.model.arima.Arima;

import connector.MysqlConnector;
import helper.*;
import model.Argument;
import model.FittingSet;
import model.Location;
import model.ModelOrder;
import model.WeatherObservation;
import model.WeatherParameter;
import model.WorkerSemaphore;
import parallel.DataProducerFit;
import parallel.ModelBuilderWorker;

public class Fitting {
	private static final int MAX_SIZE = 180;
	private static DatabaseHelper dbHelper;
	private static Argument argument;
	
	public static void main(String[] args) throws Exception{
		argument = new Argument(args);
		MysqlConnector connector = new MysqlConnector();
		dbHelper = new DatabaseHelper(connector.getConnection());
		
		if (argument.isParallel()) {
			runParallelFitting();
		} else {
			runSequenceFitting();
		}
	}
	
	private static void runSequenceFitting() throws Exception{
		int size = argument.getSize();
		List<Location> locations = dbHelper.getAllLocation();
		HashMap<Integer, HashMap<WeatherParameter, Arima>> modelMap = new HashMap<>();
		
		System.out.printf("%s Running fitting with %d data\n", new Date().toString(),size);
		long startFitting = System.currentTimeMillis();
		
		for (Location location : locations) {
			WeatherObservation weatherObservation = dbHelper.getWeatherObservation(location, size);
			HashMap<WeatherParameter, ModelOrder> modelOrderMap = dbHelper.getModelOrderMap(location, size);
			
			List<FittingSet> fittingSets = new ArrayList<>();
			
			fittingSets.add(
					new FittingSet(location, WeatherParameter.TEMPERATURE, weatherObservation.getTemperatureValues(), 
							modelOrderMap.get(WeatherParameter.TEMPERATURE))
			);
			
			fittingSets.add(
					new FittingSet(location, WeatherParameter.HUMIDITY, weatherObservation.getHumidityValues(), 
							modelOrderMap.get(WeatherParameter.HUMIDITY))
			);
			
			fittingSets.add(
					new FittingSet(location, WeatherParameter.PRESSURE, weatherObservation.getPressureValues(), 
							modelOrderMap.get(WeatherParameter.PRESSURE))
			);
			
			fittingSets.add(
					new FittingSet(location, WeatherParameter.WIND_SPEED, weatherObservation.getWindSpeedValues(), 
							modelOrderMap.get(WeatherParameter.WIND_SPEED))
			);
			
			fittingSets.add(
					new FittingSet(location, WeatherParameter.WIND_DIRECTION, weatherObservation.getWindDirectionValues(), 
							modelOrderMap.get(WeatherParameter.WIND_DIRECTION))
			);
			
			HashMap<WeatherParameter, Arima> weatherModelMap = new HashMap<>();
			modelMap.put(location.getIdLocation(), weatherModelMap);
			
			for (FittingSet fittingSet : fittingSets){
				long startBuild = System.currentTimeMillis();
				Arima model = Arima.model(fittingSet.getSeries(), fittingSet.getModelOrder().getArimaOrder());
				long timeBuild = System.currentTimeMillis() - startBuild;
				String order = Statistic.toArimaOrderString(fittingSet.getModelOrder().getArimaOrder().toString());
				System.out.printf("%s\t%s\t%s\t%d\n", new Date().toString(), fittingSet.toString(), order, timeBuild);
				modelMap.get(location.getIdLocation()).put(fittingSet.getWeatherParameter(), model);	
			}
		}
		
		long timeFitting = System.currentTimeMillis() - startFitting;
		System.out.printf("%s\tfiting finished, time execution : %d\n",new Date().toString(),timeFitting);
	}
	
	private static void runParallelFitting(){
		int size = argument.getSize();
		int numOfModelBuilderWorker = argument.getNumOfBuilderWorker();
		
		BlockingQueue<FittingSet> queue = new ArrayBlockingQueue<FittingSet>(MAX_SIZE);
		HashMap<Integer, HashMap<WeatherParameter, Arima>> modelMap = new HashMap<>();
		
		Semaphore modelMutex = new Semaphore(1);
		Semaphore fitMutex = new Semaphore(1);
		Semaphore fitFill = new Semaphore(0);
		Semaphore fitEmpty = new Semaphore(MAX_SIZE);
		WorkerSemaphore fitSemaphore = new WorkerSemaphore(fitMutex, fitFill, fitEmpty);
		
		System.out.printf("%s\tRunning fitting using parallel processing, with %d data and %d builder. current time ms: %d\n", 
				new Date().toString(), size, numOfModelBuilderWorker, System.currentTimeMillis());
		
		new Thread(new DataProducerFit(1, queue, fitSemaphore, dbHelper, size)).start();
		
		for (int i = 1; i <= numOfModelBuilderWorker; i++) {
			new Thread(new ModelBuilderWorker(i, queue, fitSemaphore, modelMap, modelMutex)).start();
		}
	}
	

}
