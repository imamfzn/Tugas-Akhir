package parallel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import helper.DatabaseHelper;
import model.FittingSet;
import model.Location;
import model.ModelOrder;
import model.WeatherObservation;
import model.WeatherParameter;
import model.WorkerSemaphore;
import model.WorkerType;

public class DataProducerFit implements Runnable {
	private BlockingQueue<FittingSet> queue;
	private WorkerSemaphore semaphore;
	private DatabaseHelper dbHelper;
	private int size;
	private int workerId;
	private WorkerType workerType;
	private WorkerLogger logger;
	
	public DataProducerFit(int workerId, BlockingQueue<FittingSet> queue, WorkerSemaphore semaphore, DatabaseHelper dbHelper, int size){
		this.queue = queue;
		this.semaphore = semaphore;
		this.dbHelper = dbHelper;
		this.size = size;
		this.workerId = workerId;
		this.workerType = WorkerType.DATA_PRODUCER_FIT;
		this.logger = new WorkerLogger(workerType, this.workerId);
	}
	
	@Override
	public void run() {
		logger.logging("Getting all dataset");
		
		try {
			List<Location> locations = dbHelper.getAllLocation();

			for (Location location : locations){
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
				
				
				try {
					for (FittingSet fittingSet : fittingSets){
						semaphore.getEmptyCount().acquire();
						semaphore.getMutex().acquire();
						
						logger.logging(String.format("put %s to queue", fittingSet.toString()));
						
						queue.put(fittingSet);
						
						semaphore.getMutex().release();
						semaphore.getFillCount().release();
					}
				} catch (InterruptedException e){
					e.printStackTrace();
				}
				
			}
			
			logger.logging("All fitting set has been put to queue");
			
		} catch (Exception e){
			e.printStackTrace();
		}
		
	}
}
