package parallel;

import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

import com.github.signaflo.timeseries.model.arima.Arima;

import helper.Statistic;
import model.FittingSet;
import model.WeatherParameter;
import model.WorkerSemaphore;
import model.WorkerType;

public class ModelBuilderWorker implements Runnable {
	private BlockingQueue<FittingSet> queue;
	private WorkerSemaphore semaphore;
	private HashMap<Integer, HashMap<WeatherParameter, Arima>> modelMap;
	private Semaphore modelMutex;
	private int workerId;
	private WorkerType workerType;
	private WorkerLogger logger;

	public ModelBuilderWorker(int workerId, BlockingQueue<FittingSet> queue, WorkerSemaphore semaphore, 
			HashMap<Integer, HashMap<WeatherParameter, Arima>> modelMap, Semaphore modelMutex){
		
		this.queue = queue;
		this.semaphore = semaphore;
		this.modelMap = modelMap;
		this.modelMutex = modelMutex;
		this.workerId = workerId;
		this.workerType = WorkerType.MODEL_BUILDER_FIT;
		this.logger = new WorkerLogger(this.workerType, this.workerId);
	}

	@Override
	public void run() {
		while (true) {
			try {
				if (queue.isEmpty()){
					logger.logging(String.format("waiting for task, current time ms: %d", System.currentTimeMillis()));
				}
				
				semaphore.getFillCount().acquire();
				semaphore.getMutex().acquire();
				
				FittingSet fittingSet = queue.take();
	
				semaphore.getMutex().release();
				semaphore.getEmptyCount().release();
				
				logger.logging(String.format("building %s", fittingSet.toString()));
				
				long start = System.currentTimeMillis();
				Arima model = Arima.model(fittingSet.getSeries(), fittingSet.getModelOrder().getArimaOrder());
				long time =  System.currentTimeMillis() - start;
				
				String order = Statistic.toArimaOrderString(fittingSet.getModelOrder().getArimaOrder().toString());
				logger.logging(String.format("%s model %s has been build, time: %d", fittingSet.toString(), order, time));
				
				modelMutex.acquire();
				
				int idLocation = fittingSet.getLocation().getIdLocation();
				
				if (!modelMap.containsKey(modelMap.containsKey(idLocation))){
					HashMap<WeatherParameter, Arima> weatherModelMap = new HashMap<>();
					modelMap.put(idLocation, weatherModelMap);
				}
				
				WeatherParameter weatherParameter = fittingSet.getWeatherParameter();
				modelMap.get(idLocation).put(weatherParameter, model);
				
				modelMutex.release();
				
			} catch (InterruptedException e){
				e.printStackTrace();
			}
		}
	}
}
