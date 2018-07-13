package parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import helper.DatabaseHelper;
import model.Location;
import model.TrainingSet;
import model.WeatherObservation;
import model.WeatherParameter;
import model.WorkerSemaphore;
import model.WorkerType;

public class DataProducerTrainTest implements Runnable {
	private BlockingQueue<TrainingSet> queue;
	private WorkerSemaphore semaphore;
	private DatabaseHelper dbHelper;
	private int size;
	private int workerId;
	private WorkerType workerType;
	private WorkerLogger logger;
	
	public DataProducerTrainTest(int workerId, BlockingQueue<TrainingSet> queue, WorkerSemaphore semaphore, DatabaseHelper dbHelper, int size){
		this.queue = queue;
		this.semaphore = semaphore;
		this.dbHelper = dbHelper;
		this.size = size;
		this.workerId = workerId;
		this.workerType = WorkerType.DATA_PRODUCER;
		this.logger = new WorkerLogger(workerType, this.workerId);
	}

	@Override
	public void run() {
		logger.logging("Getting all dataset");
		
		try {
			List<Location> locations = dbHelper.getAllLocation();

			for (int i = 0 ; i < 2; i++){
				Location location = locations.get(i);
				WeatherObservation weatherObservation = dbHelper.getWeatherObservation(location, size);
				List<TrainingSet> trainingSets = new ArrayList<>();
				
				trainingSets.add(new TrainingSet(location, WeatherParameter.TEMPERATURE, weatherObservation.getTemperatureValues()));
	            trainingSets.add(new TrainingSet(location, WeatherParameter.HUMIDITY, weatherObservation.getHumidityValues()));
	            trainingSets.add(new TrainingSet(location, WeatherParameter.PRESSURE, weatherObservation.getPressureValues()));
	            trainingSets.add(new TrainingSet(location, WeatherParameter.WIND_SPEED, weatherObservation.getWindSpeedValues()));
	            trainingSets.add(new TrainingSet(location, WeatherParameter.WIND_DIRECTION, weatherObservation.getWindDirectionValues()));
				
				try {
					for (TrainingSet trainingSet : trainingSets){
						semaphore.getEmptyCount().acquire();
						semaphore.getMutex().acquire();
						
						logger.logging(String.format("put %s to queue", trainingSet.toString()));
						
						queue.put(trainingSet);
						
						semaphore.getMutex().release();
						semaphore.getFillCount().release();
					}
				} catch (InterruptedException e){
					e.printStackTrace();
				}
				
			}
			
			logger.logging("All training set has been put to queue");
			
		} catch (Exception e){
			e.printStackTrace();
		}
		
	}
}
