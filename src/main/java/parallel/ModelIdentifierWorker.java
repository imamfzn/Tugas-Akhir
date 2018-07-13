package parallel;

import java.util.concurrent.BlockingQueue;

import model.TrainingSet;
import model.WorkerSemaphore;
import model.WorkerType;
import helper.*;

public class ModelIdentifierWorker implements Runnable{
	private BlockingQueue<TrainingSet> trainingSetQueue;
	private BlockingQueue<TrainingSet> identifiedModelQueue;
	private WorkerSemaphore tsSemaphore;
	private WorkerSemaphore imSemaphore;
	private int workerId;
	private WorkerType workerType;
	private WorkerLogger logger;

	public ModelIdentifierWorker(int workerId, BlockingQueue<TrainingSet> trainingSetQueue, BlockingQueue<TrainingSet> identifiedModelQueue,
			WorkerSemaphore tsSemaphore, WorkerSemaphore imSemaphore){

		this.trainingSetQueue = trainingSetQueue;
		this.identifiedModelQueue = identifiedModelQueue;
		this.tsSemaphore = tsSemaphore;
		this.imSemaphore = imSemaphore;
		this.workerId = workerId;
		this.workerType = WorkerType.MODEL_IDENTIFIER;
		this.logger = new WorkerLogger(this.workerType, this.workerId);
	}

	@Override
	public void run(){
		while (true){
			try {
				if (trainingSetQueue.isEmpty()){
					logger.logging("waiting for task");
				}
				
				// enter critical section of training set queue
				tsSemaphore.getFillCount().acquire();
				tsSemaphore.getMutex().acquire();
				
				TrainingSet trainingSet = trainingSetQueue.take();
	
				tsSemaphore.getMutex().release();
				tsSemaphore.getEmptyCount().release();
				// end of critical section of training set queue
				
				logger.logging(String.format("identifying %s", trainingSet.toString()));
				long start =  System.currentTimeMillis();
				
				int d = Statistic.getDifferencingValue(trainingSet.getSample());
				trainingSet.setDifferencingValue(d);
				
				double[] sample = trainingSet.getSample();
				double[] acf = Statistic.getACF(sample, 20);
				double[] pacf = Statistic.getPACF(acf);
				
				int maxP = Statistic.getAROrder(pacf, trainingSet.getSampleSize());
				int maxQ = Statistic.getMAOrder(acf, trainingSet.getSampleSize());
				
				trainingSet.setIdentifiedARMA(maxP, maxQ);
				
				long time =  System.currentTimeMillis() - start;
				
				logger.logging(String.format("add %s to queue, time: %d", trainingSet.toString(), time));
				
				// enter critical section of identified model queue
				imSemaphore.getEmptyCount().acquire();
				imSemaphore.getMutex().acquire();
				
				identifiedModelQueue.put(trainingSet);
				
				imSemaphore.getMutex().release();
				imSemaphore.getFillCount().release();
				// end of critical section of identifed model queue
				

			} catch (InterruptedException e){
				e.printStackTrace();
			}
		}
	}
}
