import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class ModernTodoListGUI extends JFrame {
    private TodoList todoList;
    private DefaultListModel<String> listModel;
    private JList<String> taskList;
    private JTextField taskInput;
    private JComboBox<Task.Priority> priorityCombo;
    private NetworkManager networkManager;
    private String currentUserId;
    private String currentGroupId;
    private ScheduledExecutorService syncExecutor;
    private JLabel connectionStatusLabel;
    private boolean todoListModified = false;

    public ModernTodoListGUI() {
        super("Distributed Task Scheduler");
        initializeComponents();
        setupUI();
        setupNetwork();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initializeComponents() {
        todoList = new TodoList();
        listModel = new DefaultListModel<>();
        networkManager = new NetworkManager();
        syncExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    private void setupUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(70, 130, 180));
        headerPanel.setPreferredSize(new Dimension(0, 70));
        
        JLabel title = new JLabel("DISTRIBUTED TASK SCHEDULER");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        
        connectionStatusLabel = new JLabel("Not connected");
        connectionStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        connectionStatusLabel.setForeground(Color.LIGHT_GRAY);
        
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.add(Box.createVerticalGlue());
        headerPanel.add(title);
        headerPanel.add(connectionStatusLabel);
        headerPanel.add(Box.createVerticalGlue());
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Left Panel
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setPreferredSize(new Dimension(300, 0));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Task Management"));

        taskInput = new JTextField();
        taskInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        priorityCombo = new JComboBox<>(Task.Priority.values());
        priorityCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        JButton addButton = new JButton("ADD TASK");
        addButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        addButton.setBackground(new Color(101, 43, 226));
        addButton.setForeground(Color.WHITE);
        addButton.addActionListener(e -> addNewTask());

        leftPanel.add(new JLabel("Task Description:"));
        leftPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        leftPanel.add(taskInput);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        leftPanel.add(new JLabel("Priority:"));
        leftPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        leftPanel.add(priorityCombo);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        leftPanel.add(addButton);
        mainPanel.add(leftPanel, BorderLayout.WEST);

        // Task List
        taskList = new JList<>(listModel);
        taskList.setCellRenderer(new TaskListRenderer());
        taskList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(taskList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Task List"));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Control Panel
        JPanel controlPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        
        // Status Buttons
        JPanel statusPanel = new JPanel(new GridLayout(1, 3, 5, 5));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Change Status"));
        statusPanel.add(createStatusButton("PENDING", new Color(255, 193, 7), Task.Status.PENDING));
        statusPanel.add(createStatusButton("IN PROGRESS", new Color(3, 169, 244), Task.Status.IN_PROGRESS));
        statusPanel.add(createStatusButton("COMPLETED", new Color(56, 142, 60), Task.Status.COMPLETED));
        
        // Action Buttons
        JPanel actionPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        actionPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
        actionPanel.add(createActionButton("CHANGE PRIORITY", new Color(103, 58, 183), e -> changeSelectedTaskPriority()));
        actionPanel.add(createActionButton("REMOVE TASK", new Color(229, 57, 53), e -> removeSelectedTask()));
        actionPanel.add(createActionButton("CLEAR COMPLETED", new Color(216, 67, 21), e -> clearCompletedTasks()));
        actionPanel.add(createActionButton("REFRESH", new Color(33, 150, 243), e -> forceRefresh()));
        
        controlPanel.add(statusPanel);
        controlPanel.add(actionPanel);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void setupNetwork() {
        showUserRegistrationDialog();
        if (networkManager.isConnected()) {
            startNetworkSync();
            // Initial sync with delay to ensure connection is ready
            new Timer(1000, e -> {
                try {
                    if (networkManager.isRunning()) {
                        networkManager.sendObject(new RefreshRequest());
                    }
                } catch (IOException ex) {
                    showError("Initial sync failed", ex);
                    updateConnectionStatus("DISCONNECTED");
                    networkManager.close();
                }
            }).start();
        }
        setupKeyboardShortcuts();
    }

    private void showUserRegistrationDialog() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        JTextField usernameField = new JTextField("User" + (int)(Math.random() * 1000));
        JTextField groupIdField = new JTextField("Group1");
        JCheckBox isServerCheckbox = new JCheckBox("Host as Server?");
        JLabel ipLabel = new JLabel("Your IP: " + NetworkManager.getLocalIP());

        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Group ID:"));
        panel.add(groupIdField);
        panel.add(new JLabel(""));
        panel.add(isServerCheckbox);
        panel.add(new JLabel("Network Info:"));
        panel.add(ipLabel);

        int result = JOptionPane.showConfirmDialog(
            this, panel, "User Registration", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            currentUserId = usernameField.getText().trim();
            currentGroupId = groupIdField.getText().trim();

            try {
                if (networkManager.isRunning()) {
                    networkManager.close();
                    Thread.sleep(100);
                }

                if (isServerCheckbox.isSelected()) {
                    networkManager.startServer(todoList);
                    updateConnectionStatus("SERVER RUNNING | IP: " + NetworkManager.getLocalIP());
                    showInfo("Server started successfully!\nClients can now connect to your IP.");
                } else {
                    String serverIp = JOptionPane.showInputDialog(
                        this, "Enter Server IP:", NetworkManager.getLocalIP());
                    if (serverIp != null && !serverIp.isEmpty()) {
                        networkManager.connectToServer(serverIp);
networkManager.sendObject(new JoinGroupRequest(currentUserId, currentGroupId)); // JOIN group
                        updateConnectionStatus("CONNECTED TO: " + serverIp);
                        showInfo("Successfully connected to server!");
                    } else {
                        updateConnectionStatus("LOCAL MODE - NOT CONNECTED");
                    }
                }
            } catch (Exception e) {
                showError("Connection failed", e);
                updateConnectionStatus("CONNECTION FAILED");
                networkManager.close();
            }
        } else {
            System.exit(0);
        }
    }

    private void startNetworkSync() {
        syncExecutor.scheduleAtFixedRate(() -> {
            try {
                // Send updates if we're the server
                if (networkManager.isServer() && todoListModified) {
                    networkManager.sendObject(todoList);
                    todoListModified = false;
                }
                
                // Process incoming updates
                Object update = networkManager.getUpdate();
                if (update instanceof TodoList) {
                    SwingUtilities.invokeLater(() -> {
                        todoList = (TodoList) update;
                        updateTaskList();
                    });
                } else if (update instanceof RefreshRequest && networkManager.isServer()) {
                    networkManager.sendObject(todoList);
                }
            } catch (Exception e) {
                if (networkManager.isRunning()) {
                    SwingUtilities.invokeLater(() -> 
                        connectionStatusLabel.setText("SYNC ERROR - " + e.getMessage()));
                }
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    private void forceRefresh() {
        if (!networkManager.isRunning()) {
            showError("Network Error", new Exception("Not connected to network"));
            reconnect();
            return;
        }
        try {
            networkManager.sendObject(new RefreshRequest());
        } catch (IOException e) {
            showError("Refresh failed", e);
            updateConnectionStatus("DISCONNECTED");
            reconnect();
        }
    }

    private void reconnect() {
        int option = JOptionPane.showConfirmDialog(
            this, 
            "Connection lost. Would you like to reconnect?",
            "Reconnect",
            JOptionPane.YES_NO_OPTION
        );
        
        if (option == JOptionPane.YES_OPTION) {
            setupNetwork();
        }
    }

    private void updateConnectionStatus(String status) {
        SwingUtilities.invokeLater(() -> connectionStatusLabel.setText(status));
    }

    private JButton createStatusButton(String text, Color color, Task.Status status) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.addActionListener(e -> {
            int index = taskList.getSelectedIndex();
            if (index >= 0) {
                todoList.updateTaskStatus(index, status, currentUserId);
                updateTaskList();
                sendUpdate();
                todoListModified = true;
            }
        });
        return button;
    }

    private JButton createActionButton(String text, Color color, ActionListener action) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.addActionListener(action);
        return button;
    }

    private void setupKeyboardShortcuts() {
        taskInput.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addNewTask();
                }
            }
        });

        taskList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    removeSelectedTask();
                }
            }
        });
    }

    private void addNewTask() {
        String description = taskInput.getText().trim();
        if (!description.isEmpty()) {
            Task.Priority priority = (Task.Priority) priorityCombo.getSelectedItem();
            
            LocalDate deadline = null;
            String deadlineStr = JOptionPane.showInputDialog(
                this, "Enter deadline (YYYY-MM-DD, optional):", 
                "Task Deadline", JOptionPane.PLAIN_MESSAGE);
            
            if (deadlineStr != null && !deadlineStr.isEmpty()) {
                try {
                    deadline = LocalDate.parse(deadlineStr);
                } catch (DateTimeParseException e) {
                    showError("Invalid date format! Use YYYY-MM-DD.", e);
                    return;
                }
            }

            todoList.addTask(description, priority, currentUserId, currentGroupId, deadline);
            taskInput.setText("");
            updateTaskList();
            sendUpdate();
            todoListModified = true;
        }
    }

    private void updateTaskList() {
        listModel.clear();
        List<Task> tasks = todoList.getGroupTasks(currentGroupId);
        for (Task task : tasks) {
            listModel.addElement(task.toString());
        }
    }

    private void sendUpdate() {
        if (!networkManager.isRunning()) {
            showError("Network Error", new Exception("Connection lost"));
            reconnect();
            return;
        }
        try {
            networkManager.sendObject(todoList);
        } catch (IOException e) {
            showError("Failed to sync with network", e);
            updateConnectionStatus("DISCONNECTED");
            reconnect();
        }
    }

    private void removeSelectedTask() {
        int index = taskList.getSelectedIndex();
        if (index >= 0) {
            Task task = todoList.getAllTasks().get(index);
            if (task.getStatus() == Task.Status.COMPLETED || 
                JOptionPane.showConfirmDialog(
                    this, "Delete this task?\n\"" + task.getDescription() + "\"", 
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION
                ) == JOptionPane.YES_OPTION) {
                todoList.removeTask(index, currentUserId);
                updateTaskList();
                sendUpdate();
                todoListModified = true;
            }
        }
    }

    private void clearCompletedTasks() {
        int removed = todoList.removeAllCompletedTasks(currentUserId);
        if (removed > 0) {
            updateTaskList();
            sendUpdate();
            todoListModified = true;
        }
    }

    private void changeSelectedTaskPriority() {
        int index = taskList.getSelectedIndex();
        if (index >= 0) {
            Task task = todoList.getAllTasks().get(index);
            Task.Priority newPriority = (Task.Priority) JOptionPane.showInputDialog(
                this,
                "Current: " + task.getPriority() + "\nSelect new priority:",
                "Change Priority",
                JOptionPane.PLAIN_MESSAGE,
                null,
                Task.Priority.values(),
                task.getPriority()
            );
            
            if (newPriority != null && newPriority != task.getPriority()) {
                todoList.updateTaskPriority(index, newPriority, currentUserId);
                updateTaskList();
                sendUpdate();
                todoListModified = true;
            }
        }
    }

    private void showError(String message, Exception e) {
        JOptionPane.showMessageDialog(
            this, message + ": " + e.getMessage(), 
            "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(
            this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void dispose() {
        syncExecutor.shutdown();
        try {
            if (!syncExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                syncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            syncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        networkManager.close();
        super.dispose();
    }

    private class TaskListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                                                    int index, boolean isSelected, 
                                                    boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            Task task = todoList.getAllTasks().get(index);
            String statusText;
            switch (task.getStatus()) {
                case PENDING:
                    statusText = "[ ] " + task.getDescription();
                    break;
                case IN_PROGRESS:
                    statusText = "[…] " + task.getDescription() + " (Processing)";
                    break;
                case COMPLETED:
                    statusText = "[✓] " + task.getDescription();
                    break;
                default:
                    throw new IllegalStateException("Unexpected status: " + task.getStatus());
            }
            
            setText(statusText);
            switch (task.getStatus()) {
                case PENDING:
                    setForeground(Color.BLACK);
                    break;
                case IN_PROGRESS:
                    setForeground(Color.BLUE);
                    break;
                case COMPLETED:
                    setForeground(new Color(0, 100, 0));
                    break;
            }
            
            if (isSelected) {
                setBackground(task.getStatus() == Task.Status.COMPLETED ? 
                    new Color(200, 230, 201) : new Color(187, 222, 251));
            }
            
            return this;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new ModernTodoListGUI();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}