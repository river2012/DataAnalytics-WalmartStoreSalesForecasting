package wekaexamples.classifiers.walmart;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimeZone;

public class BackfillMarkdowns {
  
  public static void main(String[] args) throws SQLException, ParseException {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    String url = "jdbc:postgresql://localhost/wekka";
    String username = "postgres";
    String password = "postgres";
    try (Connection conn = DriverManager.getConnection(url, username, password)) {
      conn.setAutoCommit(false);
      
      LinkedHashMap<Integer, LinkedHashMap<Date, Double[]>> markdowns = new LinkedHashMap<>();
      
      try (Statement stmt = conn.createStatement()) {
        ResultSet rs = stmt.executeQuery(
            "SELECT store, date, markdown1, markdown2, markdown3, markdown4, markdown5"
            + " FROM features ORDER BY store, date");
        while (rs.next()) {
          int store = rs.getInt("store");
          Date date = rs.getDate("date");
          Double[] markdownsForStoreDate = new Double[5];
          for (int i = 0; i < 5; i++) {
            Double markdown = rs.getDouble("markdown" + (i+1));
            if (rs.wasNull()) {
              markdown = null;
            }
            markdownsForStoreDate[i] = markdown;
          }
          LinkedHashMap<Date, Double[]> markdownsForStore = markdowns.get(store);
          if (markdownsForStore == null) {
            markdownsForStore = new LinkedHashMap<>();
            markdowns.put(store, markdownsForStore);
          }
          markdownsForStore.put(date, markdownsForStoreDate);
        }
      }
      
      // Fill the missing values
      try (Statement stmt = conn.createStatement()) {
        for (int store : markdowns.keySet()) {
          LinkedHashMap<Date, Double[]> markdownsForStore = markdowns.get(store);
          for (Date date : markdownsForStore.keySet()) {
            List<String> updates = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
              if (markdownsForStore.get(date)[i] == null) {
                Double guess = guessMarkdown(i, date, markdownsForStore);
                if (guess != null) {
                  updates.add("markdown" + (i+1) + " = " + guess);
                } else {
                  System.out.println("Could not guess markdown" + (i+1) +
                      " for store=" + store + " date=" + sdf.format(date));
                }
              }
            }
            if (!updates.isEmpty()) {
              String sql = "UPDATE features2 SET " + updates.get(0);
              for (int i = 1; i < updates.size(); i++) {
                sql += ", " + updates.get(i);
              }
              sql += " WHERE store = " + store + " AND date = '" + sdf.format(date) + "'";
              //System.out.println(sql);
              stmt.addBatch(sql);
            }
          }
        }
        int[] updates = stmt.executeBatch();
        System.out.println("Updated " + updates.length + " rows");
        conn.commit();
      }
    }
  }
  
  static Double guessMarkdown(int idx, Date date, LinkedHashMap<Date, Double[]> markdowns) {
    // Try date +- year, date +- 2 years..
    for (long x = 1; x < 5; x++) {
      for (long y : new long[] {364, 7, 357, 371}) {
        long deltaMillis = (x * y * 24L * 60L * 60L * 1000L);
        Date newDate = new Date(date.getTime() + deltaMillis);
        if (markdowns.containsKey(newDate) && (markdowns.get(newDate)[idx] != null)) {
          return markdowns.get(newDate)[idx];
        }
        newDate = new Date(date.getTime() - deltaMillis);
        if (markdowns.containsKey(newDate) && (markdowns.get(newDate)[idx] != null)) {
          return markdowns.get(newDate)[idx];
        }
      }
    }
    return null;
  }
}
