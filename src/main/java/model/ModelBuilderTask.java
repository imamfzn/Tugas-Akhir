package model;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

import com.github.signaflo.timeseries.model.arima.Arima;
import com.github.signaflo.timeseries.model.arima.ArimaOrder;

public class ModelBuilderTask {
	private TrainingSet trainingSet;
	private ArimaOrder order;
	private BlockingQueue<Arima> queue;
	Semaphore semaphore;
	
	public ModelBuilderTask (TrainingSet trainingSet, ArimaOrder order, BlockingQueue<Arima> queue, Semaphore semaphore){
		this.trainingSet = trainingSet;
		this.order = order;
		this.queue = queue;
		this.semaphore = semaphore;
	}

	public TrainingSet getTrainingSet() {
		return trainingSet;
	}

	public ArimaOrder getOrder() {
		return order;
	}

	public BlockingQueue<Arima> getQueue() {
		return queue;
	}
	
	public void addToResult(Arima model) throws InterruptedException{
		semaphore.acquire();
		queue.add(model);
		semaphore.release();
	}
	
	
}
