package controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

import com.github.signaflo.timeseries.model.arima.Arima;
import com.github.signaflo.timeseries.model.arima.ArimaOrder;

import connector.*;
import helper.*;
import model.*;
import parallel.BestModelFinderWorker;
import parallel.DataProducer;
import parallel.ModelIdentifierWorker;

public class Training {
	private static final int MAX_SIZE = 180;
	private static DatabaseHelper dbHelper;
	private static Argument argument;

	public static void main(String[] args) throws Exception {
		argument = new Argument(args);
		MysqlConnector connector = new MysqlConnector();
		dbHelper = new DatabaseHelper(connector.getConnection());

		if (argument.isParallel()) {
			System.out.printf(
					"%s Running experimen using Parallel Training with %d data, %d model identifier, %d best model finder, %d model builder, current time ms: %d\n",
					new Date().toString(), argument.getSize(), argument.getNumOfIdentifierWorker(),
					argument.getNumOfBestModelFinderWorker(), argument.getNumOfBuilderWorker(),
					System.currentTimeMillis());

			runParallelTraining();
		} else {
			System.out.printf("%s Running experimen using Sequence Training with %d data\n", new Date().toString(),
					argument.getSize());

			long start = System.currentTimeMillis();
			runSequenceTraining();
			long time = System.currentTimeMillis() - start;

			System.out.printf("%s Finished, time execution: %d\n", new Date().toString(), time);
		}

	}

	private static void runSequenceTraining() throws Exception {
		int size = argument.getSize();
		List<Location> locations = dbHelper.getAllLocation();

		for (Location location : locations) {
			WeatherObservation weatherObservation = dbHelper.getWeatherObservation(location, size);
			List<TrainingSet> trainingSets = new ArrayList<>();

			trainingSets.add(
					new TrainingSet(location, WeatherParameter.TEMPERATURE, weatherObservation.getTemperatureValues()));
			trainingSets
					.add(new TrainingSet(location, WeatherParameter.HUMIDITY, weatherObservation.getHumidityValues()));
			trainingSets
					.add(new TrainingSet(location, WeatherParameter.PRESSURE, weatherObservation.getPressureValues()));
			trainingSets.add(
					new TrainingSet(location, WeatherParameter.WIND_SPEED, weatherObservation.getWindSpeedValues()));
			trainingSets.add(new TrainingSet(location, WeatherParameter.WIND_DIRECTION,
					weatherObservation.getWindDirectionValues()));

			for (TrainingSet trainingSet : trainingSets) {
				train(trainingSet);
			}
		}
	}

	private static void train(TrainingSet trainingSet) {
		long start = System.currentTimeMillis();
		int d = Statistic.getDifferencingValue(trainingSet.getSample());

		trainingSet.setDifferencingValue(d);

		double[] acf = Statistic.getACF(trainingSet.getSample(), 20);
		double[] pacf = Statistic.getPACF(acf);
		int pMax = Statistic.getAROrder(pacf, trainingSet.getSampleSize());
		int qMax = Statistic.getMAOrder(acf, trainingSet.getSampleSize());

		List<ArimaOrder> arimaOrders = Statistic.getAllModelOrder(pMax, d, qMax);
		ArimaOrder bestOrder = arimaOrders.get(0);
		arimaOrders.remove(0);

		Arima bestModel = Arima.model(trainingSet.getSeries(), bestOrder);

		for (ArimaOrder order : arimaOrders) {
			Arima model = Arima.model(trainingSet.getSeries(), order);

			if (model.aic() < bestModel.aic()) {
				bestModel = model;
			}
		}

		double[] predicted = bestModel.forecast(24).pointEstimates().asArray();
		double rmse = Statistic.getRMSE(trainingSet.getDataTest(), predicted);
		long time = System.currentTimeMillis() - start;

		System.out.printf("%s\t%s\t%s\t%.2f\t%d\n", new Date().toString(), trainingSet.toString2(),
				Statistic.toArimaOrderString(bestModel.order().toString()), rmse, time);
	}

	private static void runParallelTraining() throws Exception {
		int size = argument.getSize();
		int numOfModelIdentiderWorker = argument.getNumOfIdentifierWorker();
		int numOfBestModelFinderWorker = argument.getNumOfBestModelFinderWorker();
		int numOfModelBuilderWorker = argument.getNumOfBuilderWorker();

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

		new Thread(new DataProducer(1, trainingSetQueue, tsSemaphore, dbHelper, size)).start();

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
