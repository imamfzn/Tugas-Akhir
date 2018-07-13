//package com.imamfzn.app;
//import connector.*;
//import model.*;
//import helper.*;
//import parallel.*;
//
//import java.util.Date;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.ArrayBlockingQueue;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.Semaphore;
//
//import com.github.signaflo.timeseries.TimeSeries;
//import com.github.signaflo.timeseries.model.arima.Arima;
//import com.github.signaflo.timeseries.model.arima.ArimaOrder;
//
//
//public class App
//{
//	private final static int MAX_SIZE = 1024;
//    
//	public static void main(String[] args){
//    	try {
//    		String arguments = getArguments(args);
//			boolean isProduction = args.length > 0 && arguments.contains("production");
//			boolean isFindSampleSize = args.length > 0 && arguments.contains("sample");
//			boolean isTimeExecution = args.length > 0 && arguments.contains("time");
//			
//			if (isFindSampleSize){
//				int totalSize = Integer.parseInt(args[2]);
//				int sampleSize = Integer.parseInt(args[3]);
//				runFindSampleSize(isProduction, totalSize, sampleSize);
//			} else if (isTimeExecution){
//				runTimeExecution(isProduction);
//			} else {
//				System.out.println("put task to run as argument");
//			}
//			
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//    }
//    
//    private static String getArguments(String[] args){
//    	String arguments = "";
//    	
//    	for (String arg : args){
//    		arguments = arguments.concat(arg);
//    	}
//    	
//    	return arguments;
//    }
//    
//    private static void runFindSampleSize(boolean isProduction, int totalSize, int sampleSize) throws Exception{
//    	MysqlConnector connector = new MysqlConnector(isProduction);
//        DatabaseHelper dbHelper = new DatabaseHelper(connector.getConnection());
//        
//        int oneDay = 24;
//    	int oneMonth = 30 * oneDay;
//    	int[] sampleSizes = { 7 * oneDay, 14 * oneDay, oneMonth, 2 * oneMonth, 3 * oneMonth };
////    	int totalSize = 3 * oneMonth + 24;
//    	
////    	for (int sampleSize : sampleSizes){
//    		System.out.printf("Running training program using %d data and %d sample size\n", totalSize, sampleSize);
//    		
//
//    		List<Location> locations = dbHelper.getAllLocation();
//    		
//    		 for (Location location : locations){
//                 WeatherObservation weatherObservation = dbHelper.getWeatherObservation(location, totalSize);
//                 List<TrainingSet2> trainingSets = new ArrayList<>();
//
//                 trainingSets.add(new TrainingSet2(location, "temperature", weatherObservation.getTemperatureValues(), sampleSize));
//                 trainingSets.add(new TrainingSet2(location, "humidity", weatherObservation.getHumidityValues(), sampleSize));
//                 trainingSets.add(new TrainingSet2(location, "pressure", weatherObservation.getPressureValues(), sampleSize));
//                 trainingSets.add(new TrainingSet2(location, "wind_speed", weatherObservation.getWindSpeedValues(), sampleSize));
//                 trainingSets.add(new TrainingSet2(location, "wind_direction", weatherObservation.getWindDirectionValues(), sampleSize));
//
//                 for (TrainingSet2 trainingSet: trainingSets){
//                     Arima model = getModel(trainingSet);
//                     double[] predicted = model.forecast(24).pointEstimates().asArray();
//                     double rmse = Statistic.getRMSE(trainingSet.getDataTest(), predicted);
//                     
//                     System.out.printf("%s\t%d\t%s\t%s\t%.2f\t%.2f\n", new Date().toString(), sampleSize, trainingSet.toString(), model.order().toString(), model.aic(), rmse);
//                 }
//             }
//    		    		
////    	}
//    	
//    	System.out.printf("%s finish execution\n", new Date().toString());
//    }
//    
//    
//    private static void runTimeExecution(boolean isProduction) throws Exception{
//    	MysqlConnector connector = new MysqlConnector(isProduction);
//        DatabaseHelper dbHelper = new DatabaseHelper(connector.getConnection());
//        List<Location> locations = dbHelper.getAllLocation();
//        
//        int totalSize = 3 * 720;
//        int sampleSize = 1 * 720;
//		
//		 for (Location location : locations){
//            WeatherObservation weatherObservation = dbHelper.getWeatherObservation(location, totalSize);
//            List<TrainingSet2> trainingSets = new ArrayList<>();
//
//            trainingSets.add(new TrainingSet2(location, "temperature", weatherObservation.getTemperatureValues(), sampleSize));
//            trainingSets.add(new TrainingSet2(location, "humidity", weatherObservation.getHumidityValues(), sampleSize));
//            trainingSets.add(new TrainingSet2(location, "pressure", weatherObservation.getPressureValues(), sampleSize));
//            trainingSets.add(new TrainingSet2(location, "wind_speed", weatherObservation.getWindSpeedValues(), sampleSize));
//            trainingSets.add(new TrainingSet2(location, "wind_direction", weatherObservation.getWindDirectionValues(), sampleSize));
//
//            for (TrainingSet2 trainingSet: trainingSets){
//            	long startDifferencing = System.currentTimeMillis();
//                int d = Statistic.getDifferencingValue(trainingSet.getSample());
//                trainingSet.setDifferencingValue(d);
//                long timeDifferencing = System.currentTimeMillis() - startDifferencing;
//                
//                long startOrder = System.currentTimeMillis();
//                double[] acf = Statistic.getACF(trainingSet.getSample(), 20);
//        		double[] pacf = Statistic.getPACF(acf);
//        		int pMax = Statistic.getAROrder(pacf, trainingSet.getSampleSize());
//        		int qMax = Statistic.getMAOrder(acf, trainingSet.getSampleSize());
//        		long timeOrder = System.currentTimeMillis() - startOrder;
//        		
//        		long startGenerate = System.currentTimeMillis();
//        		List<ArimaOrder> arimaOrders = Statistic.getAllModelOrder(pMax, d, qMax);
//        		long timeGenerate = System.currentTimeMillis() - startGenerate;
//        		
//        		long startBest = System.currentTimeMillis();
//        		ArimaOrder bestOrder = arimaOrders.get(0);
//        		arimaOrders.remove(0);
//        	
//        		Arima bestModel = Arima.model(trainingSet.getSeries(), bestOrder);
//        		
//        		for (ArimaOrder order : arimaOrders){
//        			Arima model = Arima.model(trainingSet.getSeries(), order);
//        			
//        			if (model.aic() < bestModel.aic()){
//        				bestModel = model;
//        			}
//        		}
//
//        		long timeBest = System.currentTimeMillis() - startBest;
//                
//                double[] predicted = bestModel.forecast(24).pointEstimates().asArray();
//                double rmse = Statistic.getRMSE(trainingSet.getDataTest(), predicted);
//                
//                System.out.printf("%s\t%d\t%s\t%s\t%.2f\t%.2f\t%d\t%d\t%d\t%d\n", 
//                		new Date().toString(), sampleSize, trainingSet.toString(), 
//                		bestModel.order().toString(), bestModel.aic(), rmse,
//                		timeDifferencing, timeOrder, timeGenerate, timeBest);
//            }
//        }
//		 
//		 System.out.printf("%s finish execution\n", new Date().toString());
//    }
//
//    public static void runSequence(){
//        try {
//            System.out.println("starting sequence executor...");
//            MysqlConnector connector = new MysqlConnector(false);
//            DatabaseHelper dbHelper = new DatabaseHelper(connector.getConnection());
//
//            int size = 2 * 720;
//
//            long start = System.currentTimeMillis();
//
//            List<Location> locations = dbHelper.getAllLocation();
//
//            for (Location location : locations){
//                WeatherObservation weatherObservation = dbHelper.getWeatherObservation(location, size);
//                List<TrainingSet> trainingSets = new ArrayList<>();
//
//                trainingSets.add(new TrainingSet(location, "temperature", weatherObservation.getTemperatureValues()));
//                trainingSets.add(new TrainingSet(location, "humidity", weatherObservation.getHumidityValues()));
//                trainingSets.add(new TrainingSet(location, "pressure", weatherObservation.getPressureValues()));
//                trainingSets.add(new TrainingSet(location, "wind_speed", weatherObservation.getWindSpeedValues()));
//                trainingSets.add(new TrainingSet(location, "wind_direction", weatherObservation.getWindDirectionValues()));
//
//                for (TrainingSet trainingSet: trainingSets){
//                    training(trainingSet);
//                }
//            }
//
//            long time = System.currentTimeMillis() - start;
//
//            System.out.printf("Finish, time-execution: %d\n", time);
//
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    public static void runParallel(){
//        try {
//            System.out.println("starting paralel executor...");
//            MysqlConnector connector = new MysqlConnector(false);
//            DatabaseHelper dbHelper = new DatabaseHelper(connector.getConnection());
//
//            int size = 2 * 720; //arg.getSize();
//            int numOfBuilder = 10;// arg.getNumOfBestModelFinderWorker();
//            int numOfBestFinder = 5; //arg.getNumOfBuilderWorker();
//
//            BlockingQueue<TrainingSet> trainingSetQueue = new ArrayBlockingQueue<TrainingSet>(MAX_SIZE);
//            BlockingQueue<TrainingSet> identifiedModelQueue = new ArrayBlockingQueue<TrainingSet>(MAX_SIZE);
//
//            Semaphore trainingSetSemaphore = new Semaphore(1);
//            Semaphore identifiedModelSemaphore = new Semaphore(1);
//
//            new Thread(new DataProducer(trainingSetQueue, trainingSetSemaphore, dbHelper, size)).start();
//
//            for (int i = 0 ; i < 3 ; i++){
//                 new Thread(new ModelIdentifierWorker(trainingSetQueue, identifiedModelQueue, trainingSetSemaphore, identifiedModelSemaphore)).start();
//            }
//
//            for (int i = 0 ; i < numOfBestFinder ; i++){
//                new Thread(new BestModelFinderWorker(identifiedModelQueue, identifiedModelSemaphore, numOfBuilder)).start();
//            }
//
//        } catch (Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    private static Arima getModel(TrainingSet2 trainingSet){
//    	int d = Statistic.getDifferencingValue(trainingSet.getSample());
//    	
//    	trainingSet.setDifferencingValue(d);
//    	
//    	double[] acf = Statistic.getACF(trainingSet.getSample(), 20);
//		double[] pacf = Statistic.getPACF(acf);
//		int pMax = Statistic.getAROrder(pacf, trainingSet.getSampleSize());
//		int qMax = Statistic.getMAOrder(acf, trainingSet.getSampleSize());
//		List<ArimaOrder> arimaOrders = Statistic.getAllModelOrder(pMax, d, qMax);
//		
//		ArimaOrder bestOrder = arimaOrders.get(0);
//		arimaOrders.remove(0);
//	
//		Arima bestModel = Arima.model(trainingSet.getSeries(), bestOrder);
//		
//		for (ArimaOrder order : arimaOrders){
//			Arima model = Arima.model(trainingSet.getSeries(), order);
//			
//			if (model.aic() < bestModel.aic()){
//				bestModel = model;
//			}
//		}
//		
//		return bestModel;
//				
//    }
//   private static void training(TrainingSet trainingSet){
//   	    long startTime = System.currentTimeMillis();
////    	System.out.println("get differencing");
//    	int d = Statistic.getDifferencingValue(trainingSet.getSample());
//		trainingSet.setDifferencingValue(d);
////		System.out.println("get ACF and PACF");
//		double[] acf = Statistic.getACF(trainingSet.getSample(), 20);
//		double[] pacf = Statistic.getPACF(acf);
//
//		int pMax = Statistic.getAROrder(pacf, trainingSet.getSampleSize());
//		int qMax = Statistic.getMAOrder(acf, trainingSet.getSampleSize());
//
//        long end2 = System.currentTimeMillis() - startTime;
//        System.out.printf("indentifying %d\n", end2);
//
//		List<ArimaOrder> arimaOrders = Statistic.getAllModelOrder(pMax, d, qMax);
//		// testBestModelParalel(trainingSet, arimaOrders);
//		Arima model = testBestModelSequence(trainingSet.getSeries(), arimaOrders);
//
//		System.out.printf("%s\t%s\t%d\n",trainingSet.toString(), model.order().toString(), System.currentTimeMillis() - startTime);
//   }
//
//   private static Arima testBestModelSequence(TimeSeries series, List<ArimaOrder> arimaOrders){
//   	Arima bestModel = null;
//		double minAIC = 999999999.00;
//
//		for (ArimaOrder order : arimaOrders){
//			long start = System.currentTimeMillis();
//			Arima model = Arima.model(series, order);
//			long time = System.currentTimeMillis() - start;
//			double aic = model.aic();
//
////			System.out.printf("%s %.2f %d\n", order.toString(), aic, time);
//
//			if (aic < minAIC){
//				minAIC = aic;
////				bestOrder = order;
//				bestModel = model;
//			}
//		}
//
//		return bestModel;
//   }
//
////    private static void testBestModelParalel(TrainingSet trainingSet, List<ArimaOrder> arimaOrders){
////    	BlockingQueue<Arima> queue = new ArrayBlockingQueue<Arima>(128);
////    	ModelBuilderPool pool = new ModelBuilderPool(10);
////    	pool.execute(trainingSet, arimaOrders, queue);
////
////    	while (queue.size() != arimaOrders.size()){ }
////
////    	System.out.println("All worker has been finish");
////
////    	try {
////			Arima bestModel = queue.take();
////
////			while (!queue.isEmpty()){
////				Arima model = queue.take();
////
////				if (model.aic() < bestModel.aic()){
////					bestModel = model;
////				}
////			}
////
////			System.out.println("Best model:");
////			System.out.println(bestModel.order().toString());
////		} catch (InterruptedException e) {
////			// TODO Auto-generated catch block
////			e.printStackTrace();
////		}
////    }
//}
