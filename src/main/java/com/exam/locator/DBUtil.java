package com.exam.locator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DBUtil {

    private static final String URL = "jdbc:mysql://localhost:3306/exam_locator_db?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root"; // Default XAMPP username
    private static final String PASS = "";     // Default XAMPP password is blank

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Database Driver Class Not Found: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void ensureSchema() throws SQLException {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {



            st.executeUpdate("CREATE TABLE IF NOT EXISTS Students ("
                    + "USN VARCHAR(30) PRIMARY KEY,"
                    + "Name VARCHAR(120) NOT NULL,"
                    + "Email VARCHAR(160),"
                    + "Department VARCHAR(80),"
                    + "Semester INT DEFAULT 0"
                    + ")");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS Rooms ("
                    + "RoomID VARCHAR(60) PRIMARY KEY,"
                    + "Capacity INT NOT NULL,"
                    + "Floor VARCHAR(80)"
                    + ")");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS Exams ("
                    + "ExamID INT AUTO_INCREMENT PRIMARY KEY,"
                    + "Subject VARCHAR(160) NOT NULL,"
                    + "ExamDate DATE NOT NULL,"
                    + "ExamTime VARCHAR(40) NOT NULL,"
                    + "Semester INT DEFAULT 0,"
                    + "CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ")");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS Seat_Allocations ("
                    + "AllocationID INT AUTO_INCREMENT PRIMARY KEY,"
                    + "ExamID INT NULL,"
                    + "USN VARCHAR(30) NOT NULL,"
                    + "RoomID VARCHAR(60) NOT NULL,"
                    + "BenchNumber INT NOT NULL,"
                    + "SeatPosition INT DEFAULT 1,"
                    + "CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + "INDEX idx_allocation_exam (ExamID),"
                    + "INDEX idx_allocation_usn (USN)"
                    + ")");

            addColumnIfMissing(st, "Students", "Department VARCHAR(80)");
            addColumnIfMissing(st, "Students", "Semester INT DEFAULT 0");
            normalizeRoomColumns(st);
            addColumnIfMissing(st, "Rooms", "Floor VARCHAR(80)");
            addColumnIfMissing(st, "Seat_Allocations", "AllocationID INT NULL");
            addColumnIfMissing(st, "Seat_Allocations", "ExamID INT NULL");
            addColumnIfMissing(st, "Seat_Allocations", "SeatPosition INT DEFAULT 1");
            addColumnIfMissing(st, "Seat_Allocations", "CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

            normalizeSeatAllocationKeys(st);


            st.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");
        }
    }
    private static void addColumnIfMissing(Statement st, String table, String columnDefinition) throws SQLException {
        try {
            st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + columnDefinition);
        } catch (SQLException e) {
            String state = e.getSQLState();
            int code = e.getErrorCode();
            if (code != 1060 && code != 1068 && !"42S21".equals(state)) {
                throw e;
            }
        }
    }

    private static void normalizeSeatAllocationKeys(Statement st) throws SQLException {
        if (primaryKeyDoesNotUseAllocationId(st)) {
            try {
                st.executeUpdate("ALTER TABLE Seat_Allocations DROP PRIMARY KEY");
            } catch (SQLException e) {
                if (e.getErrorCode() != 1091) {
                    throw e;
                }
            }
        }

        try {
            st.executeUpdate("ALTER TABLE Seat_Allocations MODIFY AllocationID INT NOT NULL AUTO_INCREMENT PRIMARY KEY");
        } catch (SQLException e) {
            int code = e.getErrorCode();
            if (code != 1068 && code != 1060) {
                throw e;
            }
        }

        dropLegacyUsnOnlyUniqueIndexes(st);
        removeDuplicateAllocations(st);

        try {
            st.executeUpdate("CREATE UNIQUE INDEX uniq_allocation_exam_usn ON Seat_Allocations (ExamID, USN)");
        } catch (SQLException e) {
            if (e.getErrorCode() != 1061) {
                throw e;
            }
        }
    }

    private static void normalizeRoomColumns(Statement st) throws SQLException {
        boolean hasRoomId = columnExists(st, "Rooms", "RoomID");
        boolean hasRoomNumber = columnExists(st, "Rooms", "RoomNumber");

        if (!hasRoomId && hasRoomNumber) {
            st.executeUpdate("ALTER TABLE Rooms CHANGE COLUMN RoomNumber RoomID VARCHAR(60) NOT NULL");
            return;
        }

        if (!hasRoomId) {
            addColumnIfMissing(st, "Rooms", "RoomID VARCHAR(60)");
        }

        if (hasRoomNumber) {
            st.executeUpdate("UPDATE Rooms SET RoomID = RoomNumber WHERE (RoomID IS NULL OR RoomID = '') AND RoomNumber IS NOT NULL");
        }
    }

    private static boolean columnExists(Statement st, String table, String column) throws SQLException {
        try (ResultSet rs = st.executeQuery("SHOW COLUMNS FROM " + table + " LIKE '" + column + "'")) {
            return rs.next();
        }
    }

    private static boolean primaryKeyDoesNotUseAllocationId(Statement st) throws SQLException {
        try (ResultSet rs = st.executeQuery("SHOW KEYS FROM Seat_Allocations WHERE Key_name = 'PRIMARY'")) {
            boolean hasPrimary = false;
            while (rs.next()) {
                hasPrimary = true;
                if ("AllocationID".equalsIgnoreCase(rs.getString("Column_name"))) {
                    return false;
                }
            }
            return hasPrimary;
        }
    }

    private static void dropLegacyUsnOnlyUniqueIndexes(Statement st) throws SQLException {
        List<String> indexesToDrop = new ArrayList<>();
        try (ResultSet rs = st.executeQuery("SHOW INDEX FROM Seat_Allocations WHERE Non_unique = 0")) {
            String currentIndex = "";
            int columnCount = 0;
            boolean onlyUsn = false;

            while (rs.next()) {
                String indexName = rs.getString("Key_name");
                if (!indexName.equals(currentIndex)) {
                    if (shouldDropLegacyUsnIndex(currentIndex, columnCount, onlyUsn)) {
                        indexesToDrop.add(currentIndex);
                    }
                    currentIndex = indexName;
                    columnCount = 0;
                    onlyUsn = true;
                }

                columnCount++;
                if (!"USN".equalsIgnoreCase(rs.getString("Column_name"))) {
                    onlyUsn = false;
                }
            }

            if (shouldDropLegacyUsnIndex(currentIndex, columnCount, onlyUsn)) {
                indexesToDrop.add(currentIndex);
            }
        }

        for (String indexName : indexesToDrop) {
            dropIndex(st, indexName);
        }
    }

    private static boolean shouldDropLegacyUsnIndex(String indexName, int columnCount, boolean onlyUsn) {
        return indexName != null
                && !indexName.isEmpty()
                && !"PRIMARY".equalsIgnoreCase(indexName)
                && columnCount == 1
                && onlyUsn;
    }

    private static void dropIndex(Statement st, String indexName) throws SQLException {
        try {
            st.executeUpdate("ALTER TABLE Seat_Allocations DROP INDEX `" + indexName.replace("`", "``") + "`");
        } catch (SQLException e) {
            if (e.getErrorCode() != 1091) {
                throw e;
            }
        }
    }

    private static void removeDuplicateAllocations(Statement st) throws SQLException {
        st.executeUpdate("DELETE a1 FROM Seat_Allocations a1 "
                + "JOIN Seat_Allocations a2 ON COALESCE(a1.ExamID, 0) = COALESCE(a2.ExamID, 0) "
                + "AND a1.USN = a2.USN "
                + "AND a1.AllocationID > a2.AllocationID");
    }
}
