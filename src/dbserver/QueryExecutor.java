/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dbserver;

/**
 *
 * @author Harsha Madhumal
 */
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;

public class QueryExecutor {
    private Connection connection;

    public QueryExecutor(Connection connection) {
        this.connection = connection;
    }

    /**
     * Executes a given SQL query and returns the result as a DefaultTableModel.
     * @param query SQL query to execute.
     * @return DefaultTableModel containing the query results.
     * @throws SQLException If there is an issue executing the query.
     */
    public DefaultTableModel executeQuery(String query) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(query);

        // Get metadata for column names
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        // Create table model
        DefaultTableModel tableModel = new DefaultTableModel();

        // Add column names to table model
        for (int i = 1; i <= columnCount; i++) {
            tableModel.addColumn(metaData.getColumnName(i));
        }

        // Add rows to table model
        while (resultSet.next()) {
            Object[] row = new Object[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                row[i - 1] = resultSet.getObject(i);
            }
            tableModel.addRow(row);
        }
        
        return tableModel;
    }

   
    public void bindExecuteAction(JButton executeButton, JTextArea queryArea, JTable viewerTable) {
        executeButton.addActionListener(e -> {
            String query = queryArea.getText().trim();
            if (query.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please enter a SQL query.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                DefaultTableModel model = executeQuery(query);
                viewerTable.setModel(model);
                JOptionPane.showMessageDialog(null, "Query executed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Error executing query: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
