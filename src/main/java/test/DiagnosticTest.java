package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.github.signaflo.timeseries.TimeSeries;
import com.github.signaflo.timeseries.model.arima.Arima;

import connector.MysqlConnector;
import helper.DatabaseHelper;
import helper.Statistic;
import model.FittingSet;
import model.Location;
import model.ModelOrder;
import model.WeatherObservation;
import model.WeatherParameter;

public class DiagnosticTest {
	private static int SIZE = 720;

	public static void main(String[] args) throws Exception {
		MysqlConnector connector = new MysqlConnector();
		DatabaseHelper dbHelper = new DatabaseHelper(connector.getConnection());
		List<Location> locations = dbHelper.getAllLocation();

		for (Location location : locations){
			WeatherObservation weatherObservation = dbHelper.getWeatherObservation(location, SIZE+24);
			HashMap<WeatherParameter, ModelOrder> modelOrderMap = dbHelper.getModelOrderMap(location, SIZE);

			List<FittingSet> fittingSets = new ArrayList<>();

			fittingSets.add(
					new FittingSet(location, WeatherParameter.TEMPERATURE, weatherObservation.getTemperatureValues(),
							modelOrderMap.get(WeatherParameter.TEMPERATURE))
			);

			fittingSets.add(
					new FittingSet(location, WeatherParameter.HUMIDITY, weatherObservation.getHumidityValues(),
							modelOrderMap.get(WeatherParameter.HUMIDITY))
			);

			fittingSets.add(
					new FittingSet(location, WeatherParameter.PRESSURE, weatherObservation.getPressureValues(),
							modelOrderMap.get(WeatherParameter.PRESSURE))
			);

			fittingSets.add(
					new FittingSet(location, WeatherParameter.WIND_SPEED, weatherObservation.getWindSpeedValues(),
							modelOrderMap.get(WeatherParameter.WIND_SPEED))
			);

			fittingSets.add(
					new FittingSet(location, WeatherParameter.WIND_DIRECTION, weatherObservation.getWindDirectionValues(),
							modelOrderMap.get(WeatherParameter.WIND_DIRECTION))
			);

			for (FittingSet fittingSet : fittingSets){
//				String loc = fittingSet.getLocation().getCity();
				double[] seriesDouble = fittingSet.getSeries().asArray();
				double[] seriesDoubleTest = Arrays.copyOfRange(seriesDouble, seriesDouble.length-24, seriesDouble.length);
				double[] predictedValues = new double[24];

				String order = Statistic.toArimaOrderString(fittingSet.getModelOrder().getArimaOrder().toString());

				for (int i = 0 ; i < 24 ; i++){
					double[] seriesDoubleTrain = Arrays.copyOfRange(seriesDouble, 0, seriesDouble.length-24 + i);
					TimeSeries seriesTrain = TimeSeries.from(seriesDoubleTrain);
					Arima model = Arima.model(seriesTrain, fittingSet.getModelOrder().getArimaOrder());
					predictedValues[i] =  model.forecast(1).pointEstimates().at(0);

					System.out.printf("[FORECAST-RESULT]\t%d\t%s\t%.4f\t%.4f\n", i, fittingSet.toString(),
							predictedValues[i], seriesDoubleTest[i]);
				}

				double rmse = Statistic.getRMSE(seriesDoubleTest, predictedValues);

				System.out.printf("[RMSE-RESULT]\t%s\t%s\t%.4f\n", fittingSet.toString(), order, rmse);

			}

		}

	}

}
