package edu.cmu.lloyddsilva;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.SMOreg;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.AdditiveRegression;
import weka.classifiers.meta.RandomCommittee;
import weka.classifiers.meta.Vote;
import weka.classifiers.timeseries.WekaForecaster;
import weka.classifiers.timeseries.core.TSLagMaker;
import weka.classifiers.timeseries.core.TSLagMaker.Periodicity;
import weka.classifiers.trees.RandomTree;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.SelectedTag;

//Author: Lloyd D'Silva / lloyd.dsilva@sv.cmu.edu

public class WalmartForecast {
	SimpleDateFormat dmyDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	Map<String, List<String>> trainingSalesData = new LinkedHashMap<String, List<String>>();
	Map<String,List<String>> testData = new LinkedHashMap<String,List<String>>();

	private void generateArffPerStore() throws FileNotFoundException {
		PrintWriter output;
		List<String> perDeptSales = null;
		
		//Create individual .arff files for each store and dept combo, coz they have different models each
		Iterator<String> storeDeptIterator = trainingSalesData.keySet().iterator();
		
		//header for arff files to describe the columns
		String attributes = "@attribute Store numeric" + "\n"
				+ "@attribute Dept numeric" + "\n"
				+ "@attribute Date date 'yyyy-MM-dd'" + "\n"
				+ "@attribute Weekly_Sales numeric" + "\n"
				+ "@attribute IsHoliday {FALSE,TRUE}";
		
		while (storeDeptIterator.hasNext()) {
			String store_dept = storeDeptIterator.next();
			output = new PrintWriter("data/store" + File.separator + store_dept + ".arff");
			String relation = "@relation " + store_dept;
			output.append(relation + "\n" + "\n");
			output.append(attributes + "\n" + "\n" + "@data" + "\n");
			perDeptSales = trainingSalesData.get(store_dept);
			for (String data : perDeptSales) {
				output.append(data + "\n");
			}
			output.close();
		}
	}

	private void readTrainData(String filename) throws FileNotFoundException {
		Scanner scanner;
		
		List<String> perDeptSales = null;
		
		//read all the training data and populate trainingSalesData
		scanner = new Scanner(new File(filename));
		while (scanner.hasNext()) {
			
			String inputLine = scanner.nextLine().trim();
			String[] partsOfInputLine = inputLine.split(",");
			String store_dept = partsOfInputLine[0] + "_" + partsOfInputLine[1];
			
			if (trainingSalesData.containsKey(store_dept)) {
				perDeptSales = trainingSalesData.get(store_dept);
			} else {
				perDeptSales = new ArrayList<String>();
			}
			
			perDeptSales.add(inputLine);
			trainingSalesData.put(store_dept, perDeptSales);
		}
		scanner.close();
		
	}

	private void readTestData(String filename) throws FileNotFoundException {
		Scanner scanner;
		
		List<String> requiredPerStoreDeptPrediction = null;

		scanner = new Scanner(new File(filename));
		while (scanner.hasNext()) {
			String inputLine = scanner.nextLine().trim();
			String[] partsOfInputLine= inputLine.split(",");
			String store_dept = partsOfInputLine[0] + "_" + partsOfInputLine[1];	
			String date = partsOfInputLine[2];

			if(testData.containsKey(store_dept)){
				requiredPerStoreDeptPrediction = testData.get(store_dept);
			}else{
				requiredPerStoreDeptPrediction = new ArrayList<String>();
			}
			requiredPerStoreDeptPrediction.add(date);
			testData.put(store_dept, requiredPerStoreDeptPrediction);
		}
		scanner.close();
		
	}

	private void runForecast(String filename) throws FileNotFoundException {
		PrintWriter output = new PrintWriter(new FileOutputStream(new File(filename)), true);
		output.append("Id,Weekly_Sales" + "\n");			
		Iterator<String> testDataIterator = testData.keySet().iterator();
		//int step =0;		
		double value = 0.0;
		int i = 0;
		Instances instances = null;
		Map<String,Double> forecastValues = null;
		
		while(testDataIterator.hasNext()){				
			String store_dept = testDataIterator.next();
			System.out.println(store_dept);
			
			List<String> perStoreDeptForecast = testData.get(store_dept);
			
			if(trainingSalesData.containsKey(store_dept)) {
				List<String> salesData = trainingSalesData.get(store_dept);
				instances = getInstanceFromList(salesData);
				if(instances!=null){//error didn't happen					
					forecastValues = doForecastForInstances(instances,52);	
					Iterator<String> temp = forecastValues.keySet().iterator();
					
					for(i=0;i<perStoreDeptForecast.size();i++){
						String date = perStoreDeptForecast.get(i);								
						if(!forecastValues.containsKey(date)){
							value = 0.0;	
							System.out.println(date);
						}else{
							value = forecastValues.get(date);
							if(value<0){
								value = 0.0;
							}								
						}
						output.append( store_dept + "_" + date + "," + value + "\n");
					}						
				}else{
					System.out.println("Error in creating instance for: " + store_dept);
				}
			}else{
				//Don't have train data, so output is 0.0. 
				//Could not predict based on history 
				//Have to find another way
				for(i=0;i<perStoreDeptForecast.size();i++){
					String date = perStoreDeptForecast.get(i);
					value = 0.0;
					output.append( store_dept + "_" + date + "," + value + "\n");
				}				
			}					
		}			
		output.close();
		System.out.println("Done");

	}
	
	private Map<String,Double> doForecastForInstances(Instances instance, int step) {
		Map<String,Double> result = new LinkedHashMap<String,Double>();
		try {		
			WekaForecaster forecaster = new WekaForecaster();
			forecaster.setFieldsToForecast("Weekly_Sales");
			forecaster.getTSLagMaker().setTimeStampField("Date");

			SMOreg smoreg = new SMOreg();
			smoreg.setOptions(weka.core.Utils.splitOptions("-C 1.0 -N 0 -I \"weka.classifiers.functions.supportVector.RegSMOImproved -T 0.001 -V -P 1.0E-12 -L 0.001 -W 1\" -K \"weka.classifiers.functions.supportVector.PolyKernel -E 1.0 -C 250007\""));
			forecaster.setBaseForecaster(smoreg);
			
//			LinearRegression linearReg = new LinearRegression();
//			linearReg.setOptions(weka.core.Utils.splitOptions("-S 0 -R 1.0E-8"));
//			forecaster.setBaseForecaster(linearReg);
			
//			RandomTree randomTree = new RandomTree();
//			randomTree.setOptions(weka.core.Utils.splitOptions("-K 0 -M 1.0 -V 0.001 -S 1"));
//			forecaster.setBaseForecaster(randomTree);
			
//			LinearRegression linearReg = new LinearRegression();
//			linearReg.setOptions(weka.core.Utils.splitOptions("-S 0 -R 1.0E-8"));
//			forecaster.setBaseForecaster(linearReg);
			
//			IBk ibk = new IBk();
//			ibk.setOptions(weka.core.Utils.splitOptions("-K 1 -W 0 -A \"weka.core.neighboursearch.LinearNNSearch -A \\\"weka.core.EuclideanDistance -R first-last\\\"\""));
//			forecaster.setBaseForecaster(ibk);
			
//			RandomCommittee randomComm = new RandomCommittee();
//			randomComm.setOptions(weka.core.Utils.splitOptions("-S 1 -num-slots 1 -I 25 -W weka.classifiers.trees.RandomTree -- -K 0 -M 1.0 -V 0.001 -S 1"));
//			forecaster.setBaseForecaster(randomComm);
			
//			Vote vote = new Vote();
//			vote.setOptions(weka.core.Utils.splitOptions("-S 1 -B \"weka.classifiers.functions.SMOreg -C 1.0 -N 0 -I \\\"weka.classifiers.functions.supportVector.RegSMOImproved -T 0.001 -V -P 1.0E-12 -L 0.001 -W 1\\\" -K \\\"weka.classifiers.functions.supportVector.PolyKernel -E 1.0 -C 250007\\\"\" -B \"weka.classifiers.trees.RandomTree -K 0 -M 1.0 -V 0.001 -S 1\" -B \"weka.classifiers.lazy.IBk -K 1 -W 0 -A \\\"weka.core.neighboursearch.LinearNNSearch -A \\\\\\\"weka.core.EuclideanDistance -R first-last\\\\\\\"\\\"\" -R MAX"));
//			vote.setOptions(weka.core.Utils.splitOptions("-S 1 -B \"weka.classifiers.lazy.IBk -K 1 -W 0 -A \\\"weka.core.neighboursearch.LinearNNSearch -A \\\\\\\"weka.core.EuclideanDistance -R first-last\\\\\\\"\\\"\" -B \"weka.classifiers.functions.SMOreg -C 1.0 -N 0 -I \\\"weka.classifiers.functions.supportVector.RegSMOImproved -T 0.001 -V -P 1.0E-12 -L 0.001 -W 1\\\" -K \\\"weka.classifiers.functions.supportVector.NormalizedPolyKernel -E 2.0 -C 250007\\\"\" -R AVG"));
//			forecaster.setBaseForecaster(vote);
			
//			AdditiveRegression addReg = new AdditiveRegression();
//			addReg.setOptions(weka.core.Utils.splitOptions("-S 1.0 -I 10 -W weka.classifiers.trees.RandomTree -- -K 0 -M 1.0 -V 0.001 -S 1"));
//			forecaster.setBaseForecaster(addReg);
			
			forecaster.getTSLagMaker().setMinLag(51);
			forecaster.getTSLagMaker().setMaxLag(53);
//			forecaster.getTSLagMaker().setAddDayOfMonth(true);
			forecaster.getTSLagMaker().setAddMonthOfYear(true);
			forecaster.getTSLagMaker().setAddQuarterOfYear(true);	
			forecaster.getTSLagMaker().setPeriodicity(Periodicity.WEEKLY);	
			
			forecaster.buildForecaster(instance);
			forecaster.primeForecaster(instance);
			Date currentDt = getCurrentDateTime(forecaster.getTSLagMaker());
			currentDt = advanceTime(forecaster.getTSLagMaker(), currentDt);
			List<List<NumericPrediction>> forecast = forecaster.forecast(step,System.out);				
			for (int i = 1; i <= step; i++) {						
				List<NumericPrediction> predsAtStep = forecast.get(i-1);				
				NumericPrediction predForTarget = predsAtStep.get(0);	
				result.put(dmyDateFormat.format(currentDt), predForTarget.predicted());
			    currentDt = advanceTime(forecaster.getTSLagMaker(), currentDt);
			}			
		} catch (Exception ex) {			
			ex.printStackTrace();			
		}
		return result;
	}
	
	private Instances getInstanceFromList(List<String> data){
		// setup
		FastVector attributes = new FastVector();
		Attribute weightAtt = new Attribute("Weekly_Sales");
		Attribute dateAtt = new Attribute("Date", "yyyy-MM-dd");		
		attributes.addElement(weightAtt);
		attributes.addElement(dateAtt);
		Instances instances = new Instances("timeseries", attributes, 0);
		double[] values;
		String[] partsOfLine = null;
		try {
			for(String line: data){
				partsOfLine = line.split(",");
				String date = partsOfLine[2];				
				double weight = Double.parseDouble(partsOfLine[3]);
				values = new double[instances.numAttributes()];
				values[0]= weight;
				values[1] = instances.attribute(1).parseDate(date);
				instances.add(new DenseInstance(1.0, values));
			}			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return instances;
	}
	
	private Date getCurrentDateTime(TSLagMaker lm) throws Exception {
	    return new Date((long)lm.getCurrentTimeStampValue());
	}

	private Date advanceTime(TSLagMaker lm, Date dt) {
		    return new Date((long)lm.advanceSuppliedTimeValue(dt.getTime()));
	}

	public static void main(String[] args) {
		WalmartForecast forecast = new WalmartForecast();
		try {
			forecast.readTrainData("train.csv");
			forecast.generateArffPerStore();
			forecast.readTestData("test.csv");
			forecast.runForecast("data/output.csv");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
