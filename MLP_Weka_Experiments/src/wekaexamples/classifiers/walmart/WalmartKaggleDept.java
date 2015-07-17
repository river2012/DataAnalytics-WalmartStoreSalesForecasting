package wekaexamples.classifiers.walmart;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import weka.classifiers.functions.MLPRegressor;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.experiment.InstanceQuery;

/**
This version of the experiment takes in the per department partition
*/
public class WalmartKaggleExample {
  
  /**
  Preparing the department instance set
  */
  static List<Integer> getDeptIds(InstanceQuery query) throws Exception {
    query.setQuery("SELECT DISTINCT dept FROM test ORDER BY dept");
    Instances instances = query.retrieveInstances();
    List<Integer> depts = new ArrayList<>(instances.numInstances());
    for (Instance instance : instances) {
      depts.add((int) instance.value(0));
    }
    return depts;
  }
  
  /**
  Setting up the Weka Instance with the data partition and feature set per the SQL query
  Imperative to set the class index to point to the attribute we are aiming to fit for
  */
  static final int CLASS_INDEX = 10; // index of weeklySales
  static Instances getInstances(InstanceQuery query, boolean trainTable, int deptId) throws Exception {
    query.setQuery(
          "SELECT s.size, w.week, w.holiday,\n"
        + "CASE WHEN s.type='A' THEN 1 WHEN s.type='B' THEN 2 ELSE 3 END AS type,\n"
        + "f.temperature, f.markdown1, f.markdown2, f.markdown3, f.markdown4, f.markdown5,"
        + (trainTable ? "t.weeklySales\n" : "0 AS weeklySales, t.store, t.date\n")
        + (trainTable ? "FROM train t\n" : "FROM test t\n")
        + "JOIN features2 f ON (f.store = t.store AND f.date = t.date)\n"
        + "JOIN stores s ON (s.store = t.store)\n"
        + "JOIN weeks w ON (w.date = t.date)\n"
        + "WHERE t.dept = " + deptId + "\n"
        + "ORDER BY t.store, t.date"
    );
    Instances instances = query.retrieveInstances();
    instances.setClassIndex(CLASS_INDEX);
    return instances;
  }
  
  public static void main(String[] args) throws Exception {
    // normalize the timezone to take care of day light saving discrepancies
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    Properties dbProperties = new Properties();
    dbProperties.load(WalmartKaggleExample.class.getResourceAsStream("/weka/experiment/DatabaseUtils.props.postgresql"));
    InstanceQuery query = new InstanceQuery();
    
    query.initialize(dbProperties);
    query.setDatabaseURL("jdbc:postgresql://localhost/wekka");
    query.setUsername("postgres");
    query.setPassword("postgres");
    
    // Setting up the kaggle acceptable csv output file format
    FFile testOutputFile = new File("/Users/Neha/Desktop/Results/testOut_MLPReg_store_dept.csv");
    try (BufferedWriter testOutputWriter = new BufferedWriter(new FileWriter(testOutputFile))) {
      testOutputWriter.write("Id,Weekly_Sales\n");
      List<Integer> depts = getDeptIds(query);
      double sumActual = 0, sumActualSquared = 0;
      double sumPredicted = 0, sumPredictedSquared = 0;
      double sumProduct = 0;
      int totalNumInstances = 0;

      //Training on the chosen classifier/regression method for each partition (by department)
      for (Integer dept : deptAndStores.keySet()) {
        for (Integer store : deptAndStores.get(dept)) {
          log("Getting training instances for " + dept + " " + store);
          Instances training = getInstances(query, true, dept, store);

          // The method to use to train the data partition
          MLPRegressor m5p = null;

          // Choose to train the network beyond a base threshold number of enteries, consider 20 instead of 2
          if (training.numInstances() > 2){
            log("Starting classification for " + dept + " " + store);

            //set up the MLP method
            m5p = new MLPRegressor();

            //alternate options form for the classifier
            /**
            String[] options = {"-C", "1.2", 
                "-N", "0",
                "-I", "weka.classifiers.functions.supportVector.RegSMOImproved -T 0.001 -V -P 1.0E-12 -L 0.001 -W 1",
                "-K", "weka.classifiers.functions.supportVector.RBFKernel -G 0.05 -C 250007"
                };
            */

            m5p.setOptions("-N 6 -R 0.05 -O 1.0E-4 -G -P 1 -E 1 -S 1".split(" "));
        
            // run the classification train as per the options set
            m5p.buildClassifier(training);
            log("Getting training instances for " + dept + " " + store);
            Instances training2 = getInstances(query, true, dept, store);
            for (Instance train : training2) {
        
            train.setDataset(training);
            double weeklySalesActual = train.classValue();
            double weeklySalesPredicted = m5p.classifyInstance(train);
            sumActual += weeklySalesActual;
            sumActualSquared += (weeklySalesActual * weeklySalesActual);
            sumPredicted += weeklySalesPredicted;
            sumPredictedSquared += (weeklySalesPredicted * weeklySalesPredicted);
            sumProduct += (weeklySalesActual * weeklySalesPredicted);
            totalNumInstances++;
          }
          log("Getting test instances for " + dept);
          Instances testing = getInstances(query, false, dept);
          // run the obtained model on each test data set data point and write results to the output file in the Kaggle accepted format
          for (Instance test : testing) {
            test.setDataset(null);
            int store = (int) test.value(CLASS_INDEX + 1);
            Date date = new Date((long) test.value(CLASS_INDEX + 2));
            test.deleteAttributeAt(CLASS_INDEX + 1);
            test.deleteAttributeAt(CLASS_INDEX + 1);
            test.setDataset(training);
            double weeklySalesPredicted = m5p.classifyInstance(test);
            testOutputWriter.write(store + "_" + dept + "_" + sdf.format(date)
              + "," + Utils.doubleToString(weeklySalesPredicted, 3) + "\n");
          }
        }
      }
      // put out the data partition training measures
      double varActual = sumActualSquared - (sumActual * sumActual) / totalNumInstances;
      double varPredicted = sumPredictedSquared - (sumPredicted * sumPredicted) / totalNumInstances;
      double varProduct = sumProduct - (sumActual * sumPredicted) / totalNumInstances;
      double correlation = varProduct / Math.sqrt(varActual * varPredicted);
      System.out.println(correlation); // 0.9793168835126127
    } finally {
      query.disconnectFromDatabase();
    }
  }
  
  static final long START = System.currentTimeMillis();
  static void log(String s) {
    System.out.println("[" + (System.currentTimeMillis() - START) + "] " + s);
  }
}
