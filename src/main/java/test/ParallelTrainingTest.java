package test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

import connector.MysqlConnector;
import helper.DatabaseHelper;
import model.TrainingSet;
import model.WorkerSemaphore;
import parallel.BestModelFinderWorker;
import parallel.DataProducerTrainTest;
import parallel.ModelIdentifierWorker;

public class ParallelTrainingTest {
	private static final int MAX_SIZE = 180;
	
	public static void main(String[] args) throws Exception {
		// Argument argument = new Argument(args);
		MysqlConnector connector = new MysqlConnector();
		DatabaseHelper dbHelper = new DatabaseHelper(connector.getConnection());

		
		int size = 2160;//argument.getSize();
		int numOfModelIdentiderWorker = 2;//argument.getNumOfIdentifierWorker();
		int numOfBestModelFinderWorker = 2;//argument.getNumOfBestModelFinderWorker();
		int numOfModelBuilderWorker = 5;//argument.getNumOfBuilderWorker();

		BlockingQueue<TrainingSet> trainingSetQueue = new ArrayBlockingQueue<TrainingSet>(MAX_SIZE);
		BlockingQueue<TrainingSet> identifiedModelQueue = new ArrayBlockingQueue<TrainingSet>(MAX_SIZE);

		Semaphore tsMutex = new Semaphore(1);
		Semaphore tsFill = new Semaphore(0);
		Semaphore tsEmpty = new Semaphore(MAX_SIZE);
		WorkerSemaphore tsSemaphore = new WorkerSemaphore(tsMutex, tsFill, tsEmpty);

		Semaphore imMutex = new Semaphore(1);
		Semaphore imFill = new Semaphore(0);
		Semaphore imEmpty = new Semaphore(MAX_SIZE);
		WorkerSemaphore imSemaphore = new WorkerSemaphore(imMutex, imFill, imEmpty);

		new Thread(new DataProducerTrainTest(1, trainingSetQueue, tsSemaphore, dbHelper, size)).start();

		for (int i = 0; i < numOfModelIdentiderWorker; i++) {
			new Thread(
					new ModelIdentifierWorker(i + 1, trainingSetQueue, identifiedModelQueue, tsSemaphore, imSemaphore))
							.start();
		}

		for (int i = 0; i < numOfBestModelFinderWorker; i++) {
			new Thread(new BestModelFinderWorker(i + 1, identifiedModelQueue, imSemaphore, numOfModelBuilderWorker))
					.start();
		}

	}

}
