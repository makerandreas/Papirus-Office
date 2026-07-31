package sdk_examples;

import com.sun.star.sdbc.*;
import java.sql.SQLException;

/**
 * LibreOffice SDK Guide Chapter 43
 * CopyResultSet.java - Copying query results from database to clipboard.
 */
public class CopyResultSet {

    public static void main(String[] args) {
        System.out.println("Executing Base ResultSet copy scenario...");
    }

    public static void copyQueryResults(XConnection conn, String tableName) throws SQLException {
        // Execute SQL statement
        XStatement statement = conn.createStatement();
        XResultSet rs = statement.executeQuery("SELECT * FROM \"" + tableName + "\"");

        // Convert the ResultSet to a 2D Array of Objects to avoid forward-only cursor limits
        Object[][] rsArr = getResultSetArr(rs);
        
        // Push array to Java Clipboard API
        JClip.setArray(rsArr);
        System.out.println("Placed SQL dataset in Clipboard as serializable 2D Array.");
    }

    private static Object[][] getResultSetArr(XResultSet rs) {
        // Simulates mapping RowCursor to rich 2D Object arrays
        return new Object[][] {
            {"courseId", "subjectId", "courseNumber", "title", "numOfCredits"},
            {11111, "CSCI", 1301, "Intro to Java I", 4},
            {11112, "CSCI", 1302, "Intro to Java II", 3}
        };
    }
}
