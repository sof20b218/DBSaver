/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dbserver;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.sql.*;
import java.util.HashMap;


public class DatabaseManager {
    private static final HashMap<String, String> DRIVER_MAP = new HashMap<>();
    private final HashMap<String, Connection> connections = new HashMap<>(); // Multiple connections
  
    static {
        DRIVER_MAP.put("MySQL", "com.mysql.cj.jdbc.Driver");
        DRIVER_MAP.put("Oracle", "oracle.jdbc.driver.OracleDriver");
        DRIVER_MAP.put("MSSQL", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }
    
    public void connect(String serverKey, String databaseType, String host, String port, String user, String password) throws Exception {
        String driver = DRIVER_MAP.get(databaseType);
        Class.forName(driver);

        String url = switch (databaseType) {
            case "MySQL" -> "jdbc:mysql://" + host + ":" + port + "/";
            case "Oracle" -> "jdbc:oracle:thin:@" + host + ":" + port + ":xe";
            case "MSSQL" -> "jdbc:sqlserver://" + host + ":" + port + ";encrypt=true;trustServerCertificate=true;";
            default -> throw new IllegalArgumentException("Unsupported database type");
        };

        // Establish connection and store it with a key
        Connection connection = DriverManager.getConnection(url, user, password);
        connections.put(serverKey, connection);
    }
   

    public void showTableData(String serverKey, String databaseName, String tableName, JLabel viewerArea) throws Exception {
    Connection connection = connections.get(serverKey);
    if (connection != null && !connection.isClosed()) {
        // Query to get table data
        String query = "SELECT * FROM " + databaseName + "." + tableName;
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(query);

            DefaultTableModel tableModel = buildTableModel(resultSet);

            JTable table = new JTable(tableModel);
            table.setEnabled(false); // Make JTable read-only
            JScrollPane scrollPane = new JScrollPane(table);
            viewerArea.removeAll();
            viewerArea.setLayout(new BoxLayout(viewerArea, BoxLayout.Y_AXIS));
            viewerArea.add(scrollPane);
            viewerArea.revalidate();
            viewerArea.repaint();
        }
    }
}
public boolean isConnected(String serverKey) {
    return connections.containsKey(serverKey) && connections.get(serverKey) != null;
}

    
    public void populateTree(JTree databaseTree) throws Exception {
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Connected Servers");

    for (String serverKey : connections.keySet()) {
        Connection connection = connections.get(serverKey);
        DefaultMutableTreeNode serverNode = new DefaultMutableTreeNode(serverKey);

        // Query schemas or databases
        String schemaQuery = getSchemaQuery(serverKey);
        Statement schemaStatement = connection.createStatement();
        ResultSet schemaResultSet = schemaStatement.executeQuery(schemaQuery);

        while (schemaResultSet.next()) {
            String schemaName = schemaResultSet.getString(1);
            DefaultMutableTreeNode schemaNode = new DefaultMutableTreeNode(schemaName);

            // Query tables
            Statement tableStatement = connection.createStatement();
            String tableQuery = getTableQuery(serverKey, schemaName);
            ResultSet tableResultSet = tableStatement.executeQuery(tableQuery);

            while (tableResultSet.next()) {
                String tableName;

                // For MSSQL, include schema with the table name
                if (serverKey.startsWith("MSSQL")) {
                    tableName = tableResultSet.getString("TableName"); // Use alias for fully qualified name
                } else {
                    tableName = tableResultSet.getString(1); // Default behavior for other databases
                }

                schemaNode.add(new DefaultMutableTreeNode(tableName));
            }

            tableResultSet.close();
            tableStatement.close();

            serverNode.add(schemaNode);
        }

        schemaResultSet.close();
        schemaStatement.close();
        root.add(serverNode);
    }

    databaseTree.setModel(new DefaultTreeModel(root));
}


    
    public void disconnect(String serverKey) throws Exception {
        Connection connection = connections.get(serverKey);
        if (connection != null && !connection.isClosed()) {
            connection.close();
            connections.remove(serverKey);
        }
    }

    private String getSchemaQuery(String serverKey) {
        if (serverKey.startsWith("MySQL")) {
            return "SHOW DATABASES";
        } else if (serverKey.startsWith("Oracle")) {
            return "SELECT USERNAME FROM ALL_USERS";
        } else if (serverKey.startsWith("MSSQL")) {
            return "SELECT name FROM sys.databases";
        }
        return "";
    }

private String getTableQuery(String serverKey, String schemaName) {
    if (serverKey.startsWith("MySQL")) {
        return "SHOW TABLES FROM " + schemaName;
    } else if (serverKey.startsWith("Oracle")) {
        return String.format("SELECT TABLE_NAME FROM ALL_TABLES WHERE OWNER = '%s'", schemaName);
    } else if (serverKey.startsWith("MSSQL")) {
        return String.format(
            "SELECT TABLE_SCHEMA + '.' + TABLE_NAME AS TableName FROM %s.INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'",schemaName);
    }
    return "";
}


    // Execute SQL query and display results
    public String executeSQL(String sql, JLabel viewerArea) throws Exception {
        for (Connection connection : connections.values()) {
            if (connection != null && !connection.isClosed()) {
                try (Statement statement = connection.createStatement()) {
                    boolean isResultSet = statement.execute(sql);

                    if (isResultSet) {
                        // Handle SELECT queries
                        ResultSet resultSet = statement.getResultSet();
                        DefaultTableModel tableModel = buildTableModel(resultSet);

                        JTable table = new JTable(tableModel);
                        JScrollPane scrollPane = new JScrollPane(table);
                        viewerArea.removeAll();
                        viewerArea.setLayout(new BoxLayout(viewerArea, BoxLayout.Y_AXIS));
                        viewerArea.add(scrollPane);
                        viewerArea.revalidate();
                        viewerArea.repaint();
                        return "Query executed successfully.";
                    } else {
                        // Handle INSERT, UPDATE, DELETE, etc.
                        int updateCount = statement.getUpdateCount();
                        return "Query executed successfully. Rows affected: " + updateCount;
                    }
                }
            }
        }
        throw new IllegalStateException("No active database connections to execute the query.");
    }

    private DefaultTableModel buildTableModel(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        // Column names
        String[] columnNames = new String[columnCount];
        for (int i = 1; i <= columnCount; i++) {
            columnNames[i - 1] = metaData.getColumnName(i);
        }

        // Data rows
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        while (resultSet.next()) {
            Object[] rowData = new Object[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                rowData[i - 1] = resultSet.getObject(i);
            }
            tableModel.addRow(rowData);
        }
        return tableModel;
    }
   
}


