package wekaexamples.classifiers.walmart;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.TreeMap;

import weka.classifiers.functions.MLPRegressor;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.functions.MultilayerPerceptronCS;
import weka.classifiers.trees.M5P;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.experiment.InstanceQuery;

public class WalmartKaggleExample2 {
  
  static Map<Integer, List<Integer>> getDeptAndStoreIds(InstanceQuery query) throws Exception {
    query.setQuery("SELECT DISTINCT dept,store FROM test ORDER BY dept,store");
    Instances instances = query.retrieveInstances();
    Map<Integer, List<Integer>> deptAndStores = new TreeMap<>();
    for (Instance instance : instances) {
      int dept = (int) instance.value(0);
      int store = (int) instance.value(1);
      List<Integer> stores = deptAndStores.get(dept);
      if (stores == null) {
        stores = new ArrayList<>();
        deptAndStores.put(dept, stores);
      }
      stores.add(store);
    }
    return deptAndStores;
  }
  
  static final int CLASS_INDEX = 13; // index of weeklySales
  static Instances getInstances(InstanceQuery query, boolean trainTable, int deptId, int storeId) throws Exception {
    query.setQuery(
          "SELECT s.size, w.week, w.holiday1, w.holiday2, w.holiday3, w.holiday4,\n"
        + "CASE WHEN s.type='A' THEN 1 WHEN s.type='B' THEN 2 ELSE 3 END AS type,\n"
        + "f.fuelPrice, f.markdown1, f.markdown2, f.markdown3, f.markdown4, f.markdown5,"
        + (trainTable ? "t.weeklySales\n" : "0 AS weeklySales, t.date\n")
        + (trainTable ? "FROM train t\n" : "FROM test t\n")
        + "JOIN features f ON (f.store = t.store AND f.date = t.date)\n"
        + "JOIN stores s ON (s.store = t.store)\n"
        + "JOIN weeks w ON (w.date = t.date)\n"
        + "WHERE t.dept = " + deptId + "\n"
        + "  AND t.store = " + storeId + "\n"
        + "ORDER BY t.store, t.date"
    );
    Instances instances = query.retrieveInstances();
    instances.setClassIndex(CLASS_INDEX);
    return instances;
  }
  
  public static void main(String[] args) throws Exception {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Properties dbProperties = new Properties();
    dbProperties.load(WalmartKaggleExample.class.getResourceAsStream("/weka/experiment/DatabaseUtils.props.postgresql"));
    InstanceQuery query = new InstanceQuery();
    query.initialize(dbProperties);
    query.setDatabaseURL("jdbc:postgresql://localhost/wekka");
    query.setUsername("postgres");
    query.setPassword("postgres");
    File testOutputFile = new File("/Users/Neha/Kaggle_Walmart/testOut_mlpr111.csv");
    try (BufferedWriter testOutputWriter = new BufferedWriter(new FileWriter(testOutputFile))) {
      testOutputWriter.write("Id,Weekly_Sales\n");
      Map<Integer, List<Integer>> deptAndStores = getDeptAndStoreIds(query);
      double sumActual = 0, sumActualSquared = 0;
      double sumPredicted = 0, sumPredictedSquared = 0;
      double sumProduct = 0;
      int totalNumInstances = 0;
      for (Integer dept : deptAndStores.keySet()) {
        for (Integer store : deptAndStores.get(dept)) {
          log("Getting training instances for " + dept + " " + store);
          Instances training = getInstances(query, true, dept, store);
          MLPRegressor mlpr = null;
          // MultilayerPerceptron 3748.58707
          // MultilayerPerceptronCS 3748.58707
          // setOptions("-L 0.025 -M 0.4 -N 500 -V 0 -S 0 -E 20 -H 7".split(" "));
          // MLPRegressor -N 6 -R 0.05 -O 1.0E-4 -G -P 1 -E 1 -S 0 5342.77965
          if (training.numInstances() > 0) {
            log("Starting classification for " + dept + " " + store);
            mlpr = new MLPRegressor();
            mlpr.setOptions("-N 6 -R 0.05 -O 1.0E-4 -G -P 1 -E 1 -S 0".split(" "));
            mlpr.buildClassifier(training);
            /*log("Getting training instances for " + dept + " " + store);
            Instances training2 = getInstances(query, true, dept, store);
            for (Instance train : training2) {
              train.setDataset(training);
              double weeklySalesActual = train.classValue();
              double weeklySalesPredicted = mper.classifyInstance(train);
              sumActual += weeklySalesActual;
              sumActualSquared += (weeklySalesActual * weeklySalesActual);
              sumPredicted += weeklySalesPredicted;
              sumPredictedSquared += (weeklySalesPredicted * weeklySalesPredicted);
              sumProduct += (weeklySalesActual * weeklySalesPredicted);
              totalNumInstances++;
            }*/
          }
          log("Getting test instances for " + dept + " " + store);
          Instances testing = getInstances(query, false, dept, store);
          for (Instance test : testing) {
            test.setDataset(null);
            Date date = new Date((long) test.value(CLASS_INDEX + 1));
            test.deleteAttributeAt(CLASS_INDEX + 1);
            test.setDataset(training);
            double weeklySalesPredicted = (mlpr == null) ? 0 : mlpr.classifyInstance(test);
            testOutputWriter.write(store + "_" + dept + "_" + sdf.format(date)
                + "," + Utils.doubleToString(weeklySalesPredicted, 3) + "\n");
          }
        }
      }
      double varActual = sumActualSquared - (sumActual * sumActual) / totalNumInstances;
      double varPredicted = sumPredictedSquared - (sumPredicted * sumPredicted) / totalNumInstances;
      double varProduct = sumProduct - (sumActual * sumPredicted) / totalNumInstances;
      double correlation = varProduct / Math.sqrt(varActual * varPredicted);
      System.out.println(correlation); // 3748.58707
    } finally {
      query.disconnectFromDatabase();
    }
  }
  
  static final long START = System.currentTimeMillis();
  static void log(String s) {
    System.out.println("[" + (System.currentTimeMillis() - START) + "] " + s);
  }
}
