package parallel;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

import com.github.signaflo.timeseries.model.arima.Arima;
import com.github.signaflo.timeseries.model.arima.ArimaOrder;

import helper.Statistic;
import model.TrainingSet;
import model.WorkerSemaphore;
import model.WorkerType;
import model.ModelBuilderTask;

public class ModelBuilderPool {
	private int maxWorker;
	private int bestFinderId;
	private PoolWorker[] threads;
	private BlockingQueue<ModelBuilderTask> taskQueue;
	private WorkerSemaphore semaphore;

	public ModelBuilderPool(int bestFinderId, int maxWorker){
		this.bestFinderId = bestFinderId;
		this.maxWorker = maxWorker;
		taskQueue = new ArrayBlockingQueue<ModelBuilderTask>(64);
		threads = new PoolWorker[maxWorker];

		Semaphore mutex = new Semaphore(1);
		Semaphore fillCount = new Semaphore(0);
		Semaphore emptyCount = new Semaphore(64);

		this.semaphore = new WorkerSemaphore(mutex, fillCount, emptyCount);

		for (int i = 0 ; i < this.maxWorker ; i++){
			String workerName = String.format("%s-%d-%d", WorkerType.MODEL_BUILDER.toString(), this.bestFinderId, i+1);
			threads[i] = new PoolWorker(workerName);
			threads[i].start();
		}
	}

	public void execute(TrainingSet trainingSet, List<ArimaOrder> orders, BlockingQueue<Arima> resultQueue) throws InterruptedException {
		Semaphore mutex = new Semaphore(1);

		for (ArimaOrder order : orders){
			semaphore.getEmptyCount().acquire();
			semaphore.getMutex().acquire();

			taskQueue.add(new ModelBuilderTask(trainingSet, order, resultQueue, mutex));

			semaphore.getMutex().release();
			semaphore.getFillCount().release();
		}
	}

	private class PoolWorker extends Thread {
		private String workerName;
		
		public PoolWorker(String workerName){
			super(workerName);
			this.workerName = workerName;
		}

		public void run(){
			while (true){
				try {
					semaphore.getFillCount().acquire();
					semaphore.getMutex().acquire();

					ModelBuilderTask task = taskQueue.take();

					semaphore.getMutex().release();
					semaphore.getEmptyCount().release();
					
					String trainingSetName = task.getTrainingSet().toString();
					String orderName = Statistic.toArimaOrderString(task.getOrder().toString());
					
					logging(String.format("building %s %s", trainingSetName, orderName));

					Arima model = Arima.model(task.getTrainingSet().getSeries(), task.getOrder());
					
					logging(String.format("%s %s has been built", trainingSetName, orderName));
					
					task.addToResult(model);

				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		
		public void logging(String message){
			String format = "%s\t[%s] : %s";
			String log = String.format(format, new Date().toString(), this.workerName, message);
			
			System.out.println(log);

		}
	}
}
