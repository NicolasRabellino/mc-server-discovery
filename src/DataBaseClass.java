import java.sql.*;


public class DataBaseClass {
    private static Connection connection;

    public static void init() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:servers.db");


            try (Statement statement = connection.createStatement()) {

                statement.execute("PRAGMA journal_mode=WAL");
                statement.execute("PRAGMA synchronous=NORMAL");

                statement.executeUpdate("""
                            CREATE TABLE IF NOT EXISTS server (
                                ip TEXT PRIMARY KEY,
                                description TEXT,
                                max INTEGER,
                                online INTEGER,
                                version TEXT
                            )
                        """);
            }

            System.out.println("iniciada DB");

        } catch (SQLException e) {
            throw new RuntimeException("no anda la DB ", e);
        }

    }

    public static synchronized void insertServer(
            String ip,
            String description,
            int max,
            int online,
            String version
    ) {
        try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO server (ip, description, max, online, version)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT(ip) DO UPDATE SET
                        online = excluded.online,
                        max = excluded.max,
                        version = excluded.version,
                        description = excluded.description
                """)) {

            ps.setString(1, ip);
            ps.setString(2, description);
            ps.setInt(3, max);
            ps.setInt(4, online);
            ps.setString(5, version);

            ps.executeUpdate();

            System.out.println("DB INSERT -> " + ip);

        } catch (SQLException e) {
            System.err.println("No pudo meter -> " + ip);
            e.printStackTrace();
        }
    }

    public static void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }



}
