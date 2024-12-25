/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dbserver;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.TreePath;

public class UIWireframe {
    public void setupUI() {
        JFrame frame = new JFrame("Multiple Database Server Connector");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLayout(new BorderLayout());

        DatabaseManager dbManager = new DatabaseManager();
        
        // Menu Bar
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");
        JMenu helpMenu = new JMenu("Help");
        JMenuItem exitMenuItem = new JMenuItem("Exit");
        fileMenu.add(exitMenuItem);
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(helpMenu);
        frame.setJMenuBar(menuBar);

        
        
        // Toolbar
        JPanel toolBarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        toolBarPanel.setBackground(Color.LIGHT_GRAY);
        JButton btnCon = new JButton("Connect");  
        JButton btnDiscon = new JButton("Disconnect");
        JButton btnExec = new JButton("Execute SQL");
        JButton btnRefresh = new JButton("Refresh");

     
        toolBarPanel.add(btnCon);
        toolBarPanel.add(btnDiscon);
        toolBarPanel.add(btnExec);
        toolBarPanel.add(btnRefresh);
        frame.add(toolBarPanel, BorderLayout.NORTH);

        // Explore Area 1: Connected Database Server List
        DefaultListModel<String> serverListModel = new DefaultListModel<>();
        JList<String> serverList = new JList<>(serverListModel);
        JScrollPane serverListScrollPane = new JScrollPane(serverList);

        // Explore Area 2: Selected Database Server Tree
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Databases");
        JTree databaseTree = new JTree(rootNode);
        JScrollPane databaseTreeScrollPane = new JScrollPane(databaseTree);

        // Split pane for Explore Areas
        JSplitPane exploreSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        exploreSplitPane.setTopComponent(serverListScrollPane);
        exploreSplitPane.setBottomComponent(databaseTreeScrollPane);
        exploreSplitPane.setDividerLocation(300);

        // Viewer Area
        JLabel viewerArea = new JLabel("Viewer or Editing Area", SwingConstants.CENTER);
      viewerArea.setOpaque(true);
        viewerArea.setBackground(Color.WHITE);


        JTextArea commandArea = new JTextArea(3, 0); // Set height to 3 lines
        JScrollPane commandScrollPane = new JScrollPane(commandArea);
        commandArea.setLineWrap(true);
        commandArea.setWrapStyleWord(true);
        commandArea.setFont(new Font("Arial", Font.PLAIN, 14));
        commandArea.setForeground(Color.GRAY);
        commandArea.setText("Command Area");

            commandArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (commandArea.getText().equals("Command Area")) {
                    commandArea.setText("");
                    commandArea.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (commandArea.getText().isEmpty()) {
                    commandArea.setForeground(Color.GRAY);
                    commandArea.setText("Command Area");
                }
            }
        });

        // Tree Selection Listener for Table Data
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
                                dbManager.populateTree(databaseTree); // Update tree with tables for the selected database
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(frame, "Error displaying database tables:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            }
                        }

                      
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

        // Split pane for Viewer and Command Areas
        JSplitPane viewerCommandSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        viewerCommandSplitPane.setTopComponent(viewerArea);
        viewerCommandSplitPane.setBottomComponent(commandScrollPane);
        viewerCommandSplitPane.setDividerLocation(500);

        // Horizontal Split: Explore Areas (Left) and Viewer/Command Areas (Right)
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setLeftComponent(exploreSplitPane);
        mainSplitPane.setRightComponent(viewerCommandSplitPane);
        mainSplitPane.setDividerLocation(250);

        frame.add(mainSplitPane, BorderLayout.CENTER);

        // Initialize ActionManager and DatabaseManager
        ActionManager actionManager = new ActionManager();
        actionManager.setupActions(frame, btnCon, btnDiscon, btnExec,btnRefresh, serverListModel, serverList, databaseTree, commandArea, viewerArea);

        exitMenuItem.addActionListener(e -> System.exit(0));

        // Footer Panel
    JPanel footerPanel = new JPanel();
    footerPanel.setLayout(new BorderLayout());
    footerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Padding
    footerPanel.setBackground(Color.LIGHT_GRAY);

    // Footer Content
    JLabel footerLabel = new JLabel("© SOF/20/B2/18_G.K.H.M.Deshapriya - All rights reserved.", SwingConstants.CENTER);
    footerLabel.setFont(new Font("Arial", Font.PLAIN, 12));
    footerLabel.setForeground(Color.DARK_GRAY);

    // Add label to footer panel
    footerPanel.add(footerLabel, BorderLayout.CENTER);

    // Add footer panel to frame
    frame.add(footerPanel, BorderLayout.SOUTH);
        
        frame.setVisible(true);
    }
}
