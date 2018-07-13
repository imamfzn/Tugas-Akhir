package test;

import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

import com.github.signaflo.timeseries.model.arima.Arima;

import connector.MysqlConnector;
import helper.DatabaseHelper;
import model.FittingSet;
import model.WeatherParameter;
import model.WorkerSemaphore;
import parallel.DataProducerFitTest;
import parallel.ModelBuilderWorker;

public class ParallelFittingTest {
	private static final int MAX_SIZE = 180;
	
	public static void main(String[] args) throws Exception{
		MysqlConnector connector = new MysqlConnector();
		DatabaseHelper dbHelper = new DatabaseHelper(connector.getConnection());
		
		int size =  2160;
		int numOfModelBuilderWorker = 5;
		
		BlockingQueue<FittingSet> queue = new ArrayBlockingQueue<FittingSet>(MAX_SIZE);
		HashMap<Integer, HashMap<WeatherParameter, Arima>> modelMap = new HashMap<>();
		
		Semaphore modelMutex = new Semaphore(1);
		Semaphore fitMutex = new Semaphore(1);
		Semaphore fitFill = new Semaphore(0);
		Semaphore fitEmpty = new Semaphore(MAX_SIZE);
		WorkerSemaphore fitSemaphore = new WorkerSemaphore(fitMutex, fitFill, fitEmpty);
		
		System.out.printf("%s\tRunning fitting using parallel processing, with %d data and %d builder. current time ms: %d\n", 
				new Date().toString(), size, numOfModelBuilderWorker, System.currentTimeMillis());
		
		new Thread(new DataProducerFitTest(1, queue, fitSemaphore, dbHelper, size)).start();
		
		for (int i = 1; i <= numOfModelBuilderWorker; i++) {
			new Thread(new ModelBuilderWorker(i, queue, fitSemaphore, modelMap, modelMutex)).start();
		}
	}
}
