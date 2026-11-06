import java.sql.*;

public class SqliteProbe {
    public static void main(String[] args) throws Exception {
        String driver = args[0];
        try {
            Class.forName(driver);
            System.out.println("Class.forName(" + driver + ") -> OK");
        } catch (Throwable t) {
            System.out.println("Class.forName(" + driver + ") -> FAILED: " + t);
            return;
        }
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + args[1])) {
            c.createStatement().execute("CREATE TABLE IF NOT EXISTS skin_cache (nickname TEXT PRIMARY KEY, "
                    + "value TEXT NOT NULL, signature TEXT NOT NULL, updated_at INTEGER NOT NULL)");
            c.createStatement().execute("INSERT OR REPLACE INTO skin_cache VALUES ('notch','v','s',1)");
            ResultSet rs = c.createStatement().executeQuery("SELECT COUNT(*) FROM skin_cache");
            rs.next();
            System.out.println("sqlite " + c.getMetaData().getDatabaseProductVersion()
                    + ", rows=" + rs.getInt(1) + " -> OK");
        }
    }
}
