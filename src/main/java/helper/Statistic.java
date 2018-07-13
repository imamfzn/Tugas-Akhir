package helper;
import com.numericalmethod.suanshu.stats.test.timeseries.adf.AugmentedDickeyFuller;
import com.github.signaflo.timeseries.model.arima.ArimaOrder;
import com.github.signaflo.timeseries.TimeSeries;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Statistic {
	private final static double P_VALUE = -3.45;
	private final static double NOT_COMPUTED_VALUE = 999999.00;
	private final static int MAX_LAGS = 8;
	private final static int ONE_DAY = 24;
	private final static int ONE_WEEK = 7 * ONE_DAY;
	private final static int SAMPLE_SIZE = 2 * ONE_WEEK;
	
	// check ts in stationary using augmented dickey fuller test
	public static boolean isStasionary(double[] timeSeriesData){
		double [] data = timeSeriesData;
		AugmentedDickeyFuller adfTestResult = null;
		
		try {
			adfTestResult = new AugmentedDickeyFuller(data);
		} catch (Exception e){
			String errorMessage = e.getMessage();
			
			if (errorMessage.contains("not invertible") || errorMessage.contains("no solution")){
				return true;
			} else {
				throw e;
			}
		}
		
//		System.out.printf("TEST_STATISTIC: %.2f\n", adfTestResult.statistics());
		
		return adfTestResult.statistics() < P_VALUE;
	}
	
	public static int getDifferencingValue(double[] sampleData){
		TimeSeries tsSample = TimeSeries.from(sampleData);
		int d = 0;

		while (!Statistic.isStasionary(tsSample.asArray())){
			tsSample = tsSample.difference();
			d++;
		}
		
		return d;
	}
	
	public static double getRMSE(double[] observations, double[] predicted){
		double sum = 0;
		for (int i = 0 ; i < predicted.length ; i++){
			sum += (observations[i] - predicted[i]) * (observations[i] - predicted[i]);
		}
		
		return Math.sqrt(sum / predicted.length);
	}
	
	public static int getSampleSize(int numOfObservations){
		return numOfObservations < SAMPLE_SIZE ? numOfObservations : SAMPLE_SIZE;
	}
	
	public static double[] getSampleData(double[] data){
		int sampleSize = getSampleSize(data.length);
		
		return Arrays.copyOfRange(data, data.length-sampleSize, data.length);
	}
	
	public static double[] getSampleDataWithSize(double[] data, int sampleSize){		
		return Arrays.copyOfRange(data, data.length-sampleSize, data.length);
	}
	
	public static double[] get24HourDataTest(double[] data){
		return Arrays.copyOfRange(data, data.length-24, data.length);
	}
	
	public static double[] get24HourDataTrain(double[] data){
		return Arrays.copyOfRange(data, 0, data.length-24);
	}
	
	public static List<ArimaOrder> getAllModelOrder(int maxP, int d, int maxQ){
		List<ArimaOrder> arimaOrders = new ArrayList<ArimaOrder>();
		
		for (int p = 0 ; p <= maxP ; p++){
			for (int q = 0 ; q <= maxQ ; q++){
				if (p != 0 || q!= 0){
					ArimaOrder order = ArimaOrder.order(p, d, q);
					arimaOrders.add(order);
				}
			}
		}
		
		return arimaOrders;
	}
	
	public static int getOrder(double[] values, int numOfObservations){
		double significantValue = getACFSignificantValue(numOfObservations);
		int order = 0;
		
		for (int i = 1 ; i <= MAX_LAGS ; i++){
			if (order != 0){
				if (values[i] > -significantValue && values[i] < significantValue){
					return order;
				}
			}
			
			order++;
		}
		
		return order;
	}
	
	private static boolean isSignificantLag(double lagValue, double significantValue){
		return (lagValue <= -significantValue) || (lagValue >= significantValue);
	}
	
	public static int getAROrder(double[] pacf, int numOfObservations){
	    double significantValue = getACFSignificantValue(numOfObservations);
	    int arOrder = 0;
	    
	    for (int i = 1; i <= MAX_LAGS; i++){
	    	if (!isSignificantLag(pacf[i], significantValue)){
	    		if (arOrder == 0){
	    			
	    			// find siginificant lag
	    			for (i = i + 1 ; i <= MAX_LAGS ; i++){
	    				arOrder++;
	    				
	    				if (isSignificantLag(pacf[i], significantValue)){
	    					break;
	    				}	    				
	    			}
	    		} else {
	    			return arOrder;
	    		}
	    	}
	    	arOrder++;
	    }
	    
	    return arOrder;
	}

	
	public static int getMAOrder(double[] acf, int numOfObservations){
	    double significantValue = getACFSignificantValue(numOfObservations);
	    int maOrder = 0;
	    
	    for (int i = 1; i <= MAX_LAGS; i++){
	    	if (!isSignificantLag(acf[i], significantValue)){
	    		if (maOrder == 0){
	    			
	    			// find siginificant lag
	    			for (i = i + 1 ; i <= MAX_LAGS ; i++){
	    				maOrder++;
	    				
	    				if (isSignificantLag(acf[i], significantValue)){
	    					break;
	    				}
	    			}
	    		} else {
	    			return maOrder;
	    		}
	    	}
	    	maOrder++;
	    }
	    
	    return maOrder;
	}
		
	public static double getACFSignificantValue(int sampleSize){
		return 1.96 / Math.sqrt(sampleSize);
	}
	
	public static double[] getACF(double[] sample, int numOfLags){
		TimeSeries sampleTimeSeries = TimeSeries.from(sample);
		
		return sampleTimeSeries.autoCorrelationUpToLag(numOfLags);
	}
	
	public static double[] getPACF(double[] acf){
		int numOfLags = acf.length-1;
		double[][] phi = getInitialPACF(numOfLags);
		double [] pacf = new double[numOfLags+1];
		
		phi[1][1] = acf[1];
		pacf[0] = acf[0];
		pacf[1] = acf[1];
		
		for (int i = 2 ; i <= numOfLags ; i++){
			double upper = acf[i];
			double lower = 1;
			
			for (int j = 1 ; j <= i - 1 ; j++){
				if (phi[i-1][j] == NOT_COMPUTED_VALUE){
					phi[i-1][j] = phi[i-2][j] - (phi[i-1][i-1] * phi[i-2][i-1-j]);
				}
				
				double sigma = phi[i-1][j] * acf[i-j];
				upper -= sigma;
				lower -= sigma;
			}
			
			phi[i][i] = upper / lower;
			pacf[i] = phi[i][i];
		}
		
		return pacf;
	}
	
	private static double[][] getInitialPACF(int numOfLags){
		double[][] phi = new double[numOfLags+1][numOfLags+1];
		
		for (int i = 1 ; i <= numOfLags ; i++){
			for (int j = 1 ; j <= numOfLags ; j++){
				phi[i][j] = NOT_COMPUTED_VALUE;
			}
		}

		return phi;
	}
	
	public static String toArimaOrderString(String order){
		return order.split(" with ")[0];
	}
}
