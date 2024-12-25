/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dbserver;

import java.awt.BorderLayout;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.*;
//import javax.swing.tree.JTree;

public class ActionManager {
   

   private final DatabaseManager dbManager = new DatabaseManager();

    public void setupActions(JFrame frame, JButton btnCon, JButton btnDiscon, JButton btnExec,JButton btnRefresh, 
                             DefaultListModel<String> serverListModel, JList<String> serverList,
                             JTree databaseTree, JTextArea commandArea, JLabel viewerArea) {


    btnCon.addActionListener(e -> {
        String[] options = {"MySQL", "Oracle", "MSSQL"};
        String databaseType = (String) JOptionPane.showInputDialog(frame, "Select Database Type:",
                "Database Type", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (databaseType == null) return;

        JTextField hostField = new JTextField();
        JTextField portField = new JTextField();
        JTextField userField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        Object[] message = {
            "Host:", hostField,
            "Port:", portField,
            "Username:", userField,
            "Password:", passwordField
        };

        int option = JOptionPane.showConfirmDialog(frame, message, "Connect to Database", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String host = hostField.getText();
            String port = portField.getText();
            String user = userField.getText();
            String password = new String(passwordField.getPassword());

            String serverKey = databaseType + "@" + host;

            try {
                dbManager.connect(serverKey, databaseType, host, port, user, password);
                dbManager.populateTree(databaseTree);
                serverListModel.addElement(serverKey);
                JOptionPane.showMessageDialog(frame, "Connected successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error connecting to database:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    });

 btnDiscon.addActionListener(e -> {
    String selectedServer = serverList.getSelectedValue();
    if (selectedServer == null) {
        JOptionPane.showMessageDialog(frame, "Please select a server to disconnect.", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    try {
        // Disconnect the selected server
        dbManager.disconnect(selectedServer);

        // Remove the server from the server list
        serverListModel.removeElement(selectedServer);

        // Update the tree by removing only the disconnected database
        DefaultTreeModel treeModel = (DefaultTreeModel) databaseTree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();

        // Find the node corresponding to the disconnected database and remove it
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode serverNode = (DefaultMutableTreeNode) root.getChildAt(i);
            if (serverNode.getUserObject().equals(selectedServer)) {
                root.remove(i);
                break;
            }
        }

        // Refresh the tree
        treeModel.reload();


viewerArea.removeAll();
        viewerArea.setText("Viewer or Editing Area");
        viewerArea.setHorizontalAlignment(SwingConstants.CENTER);
        viewerArea.revalidate();
        viewerArea.repaint();
        JOptionPane.showMessageDialog(frame, "Disconnected successfully!", "Disconnected", JOptionPane.INFORMATION_MESSAGE);
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(frame, "Error while disconnecting:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
});

    btnExec.addActionListener(e -> {
        String sql = commandArea.getText().trim();
        if (sql.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "SQL command area is empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String feedback = dbManager.executeSQL(sql, viewerArea);
            JOptionPane.showMessageDialog(frame, feedback, "Execution Result", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Error executing SQL:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
   });
    

btnRefresh.addActionListener(e -> {
    try {
        // Determine the selected server key dynamically
        String selectedServerKey = serverList.getSelectedValue(); // Assuming serverList is the JList<String> showing server keys

        if (selectedServerKey == null || selectedServerKey.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Please select a server to refresh.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Refresh the viewer area
        refreshViewerArea(databaseTree, viewerArea, selectedServerKey);
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(null, "Error refreshing viewer area:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
});





    
  databaseTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                TreePath selectedPath = databaseTree.getSelectionPath();

                if (selectedPath != null) {
                    Object lastPathComponent = selectedPath.getLastPathComponent();
                    if (lastPathComponent instanceof DefaultMutableTreeNode) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) lastPathComponent;
                        Object nodeInfo = node.getUserObject();

                        // If it's a database node, show its tables
                        if (nodeInfo instanceof String && !node.isLeaf()) {
                            String databaseName = (String) nodeInfo;
                            try {
                                dbManager.populateTree(databaseTree); // Update tree with tables of the selected database
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(frame, "Error displaying database tables:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        }

                        // If it's a table node, show the data of the selected table
                        if (node.isLeaf()) {
                            String tableName = (String) node.getUserObject();
                            String databaseName = selectedPath.getParentPath().getLastPathComponent().toString(); // Get parent DB name
                            String serverKey = (String) serverList.getSelectedValue(); // Get selected server

                            try {
                                dbManager.showTableData(serverKey, databaseName, tableName, viewerArea);
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(frame, "Error fetching table data:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                }
            }
        });

     
     
   }
   public void refreshViewerArea(JTree databaseTree, JLabel viewerArea, String selectedServerKey) {
    try {
        // Get the selected node from the tree
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) databaseTree.getLastSelectedPathComponent();
        if (selectedNode != null && selectedNode.isLeaf()) {
            // Get the parent node (database name)
            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selectedNode.getParent();
            if (parentNode != null) {
                String selectedTable = selectedNode.getUserObject().toString(); // Table name
                String selectedDatabase = parentNode.getUserObject().toString(); // Database name

                // Call the existing method to display table data in the viewer area
                dbManager.showTableData(selectedServerKey, selectedDatabase, selectedTable, viewerArea);
            }
        }
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(null, "Error refreshing table:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}

}


