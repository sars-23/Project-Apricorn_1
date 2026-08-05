import java.sql.*;

public class DbCheck {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://localhost:5432/apricorn";
        String user = "apricorn";
        String password = "apricorn";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("=== TEST CASES COUNT PER PROBLEM ===");
            ResultSet rsTc = stmt.executeQuery("SELECT problem_id, COUNT(*) as c FROM test_cases GROUP BY problem_id");
            while (rsTc.next()) {
                System.out.println("prob_id=" + rsTc.getInt("problem_id") + ", count=" + rsTc.getInt("c"));
            }
        }
    }
}
