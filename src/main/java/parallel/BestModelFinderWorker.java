package parallel;


import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.github.signaflo.timeseries.model.arima.Arima;
import com.github.signaflo.timeseries.model.arima.ArimaOrder;
import model.TrainingSet;
import model.WorkerSemaphore;
import model.WorkerType;
import helper.*;

public class BestModelFinderWorker implements Runnable {
	private BlockingQueue<TrainingSet> identifiedModelQueue;
	private WorkerSemaphore semaphore;
	private int workerId;
	private WorkerType workerType;
	private ModelBuilderPool pool;
	private WorkerLogger logger;

	public BestModelFinderWorker(int workerId, BlockingQueue<TrainingSet> identifiedModelQueue,
			WorkerSemaphore semaphore, int numOfModelBuilderWorker){

		this.identifiedModelQueue = identifiedModelQueue;
		this.semaphore = semaphore;
		this.pool = new ModelBuilderPool(workerId, numOfModelBuilderWorker);
		this.workerId = workerId;
		this.workerType = WorkerType.BEST_MODEL_FINDER;
		this.logger = new WorkerLogger(this.workerType, this.workerId);
	}

	@Override
	public void run(){
		while (true){
			try {
				if (identifiedModelQueue.isEmpty()){
					logger.logging(String.format("waiting for task, current time ms: %d", System.currentTimeMillis()));
				}

				// enter critical section of identified model queue
				semaphore.getFillCount().acquire();
				semaphore.getMutex().acquire();

				TrainingSet trainingSet = identifiedModelQueue.take();

				semaphore.getMutex().release();
				semaphore.getEmptyCount().release();
				// end of critical section of identified model queue

				logger.logging(String.format("finding best model %s", trainingSet.toString()));
				long start = System.currentTimeMillis();

				int pMax = trainingSet.getPMax();
				int d = trainingSet.getD();
				int qMax = trainingSet.getQMax();

				List<ArimaOrder> orders = Statistic.getAllModelOrder(pMax, d, qMax);
				BlockingQueue<Arima> resultQueue = new ArrayBlockingQueue<Arima>(128);

				logger.logging(String.format("sending %d order of %s to builder (%d %d %d)",
						orders.size(), trainingSet.toString(), trainingSet.getPMax(), trainingSet.getD(), trainingSet.getQMax()));

				pool.execute(trainingSet, orders, resultQueue);
				logger.logging(String.format("Waiting for builder completing %s task", trainingSet.toString()));

				while (resultQueue.size() != orders.size()){ }
				logger.logging(String.format("All builder has been finished for %s task", trainingSet.toString()));

				Arima bestModel = resultQueue.take();

				while (!resultQueue.isEmpty()){
					Arima model = resultQueue.take();

					if (model.aic() < bestModel.aic()){
						bestModel = model;
					}
				}

				double[] predicted = bestModel.forecast(24).pointEstimates().asArray();
		    	double rmse = Statistic.getRMSE(trainingSet.getDataTest(), predicted);

				long time = System.currentTimeMillis() - start;

				logger.logging(String.format("Best model of %s : %s, rmse : %.2f, time: %d",
						trainingSet.toString(),
						Statistic.toArimaOrderString(bestModel.order().toString()),
						rmse,
						time
				));

			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}
}
