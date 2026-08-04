import java.sql.*;

public class DbQuery {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://localhost:5432/apricorn";
        String user = "apricorn";
        String password = "apricorn";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("=== PART D: ALL PROBLEMS ===");
            ResultSet rsP = stmt.executeQuery("SELECT id, slug, title, input_format FROM problems");
            while (rsP.next()) {
                System.out.println("id=" + rsP.getInt("id") + ", slug=" + rsP.getString("slug") + ", input_format=" + rsP.getString("input_format"));
            }
        }
    }
}
