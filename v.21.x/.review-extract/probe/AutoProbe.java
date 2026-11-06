import java.sql.*;
public class AutoProbe {
  public static void main(String[] a) throws Exception {
    System.out.println("registered drivers: " + java.util.Collections.list(DriverManager.getDrivers()));
  }
}