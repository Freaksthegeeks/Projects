
import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

public class TodoList implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final List<Task> tasks = new CopyOnWriteArrayList<>();
    private final Map<String, List<Task>> userTasks = new ConcurrentHashMap<>();
    private final Map<String, List<String>> groups = new ConcurrentHashMap<>();

    public synchronized String createUser() {
        String userId = UUID.randomUUID().toString();
        userTasks.putIfAbsent(userId, new CopyOnWriteArrayList<>());
        return userId;
    }

    public synchronized String createGroup(String hostUserId) {
        if (hostUserId == null || hostUserId.trim().isEmpty()) {
            throw new IllegalArgumentException("Host user ID cannot be null or empty");
        }
        
        String groupId = UUID.randomUUID().toString();
        groups.put(groupId, new CopyOnWriteArrayList<>(Collections.singletonList(hostUserId)));
        return groupId;
    }

    public synchronized boolean joinGroup(String groupId, String userId) {
        if (groupId == null || userId == null) {
            return false;
        }
        
        List<String> groupMembers = groups.get(groupId);
        if (groupMembers != null && !groupMembers.contains(userId)) {
            groupMembers.add(userId);
            return true;
        }
        return false;
    }

    public synchronized void addTask(String description, Task.Priority priority, 
                                   String userId, String groupId, LocalDate deadline) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Task description cannot be null or empty");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        Task task = new Task(description, priority, userId, groupId, deadline);
        tasks.add(task);
        userTasks.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(task);
    }

    public synchronized boolean removeTask(int index, String userId) {
        if (index < 0 || index >= tasks.size() || userId == null) {
            return false;
        }
        
        Task task = tasks.get(index);
        if (userId.equals(task.getAssignedUser())) {
            tasks.remove(index);
            List<Task> userTaskList = userTasks.get(userId);
            if (userTaskList != null) {
                userTaskList.remove(task);
            }
            return true;
        }
        return false;
    }

    public synchronized int removeAllCompletedTasks(String userId) {
        if (userId == null) {
            return 0;
        }
        
        List<Task> userTaskList = userTasks.get(userId);
        if (userTaskList == null) {
            return 0;
        }
        
        List<Task> toRemove = new ArrayList<>();
        for (Task task : userTaskList) {
            if (task.getStatus() == Task.Status.COMPLETED) {
                toRemove.add(task);
            }
        }
        
        tasks.removeAll(toRemove);
        userTaskList.removeAll(toRemove);
        return toRemove.size();
    }

    public synchronized void updateTaskStatus(int index, Task.Status newStatus, String userId) {
        if (index >= 0 && index < tasks.size() && userId != null) {
            Task task = tasks.get(index);
            if (userId.equals(task.getAssignedUser())) {
                task.setStatus(newStatus);
            }
        }
    }

    public synchronized void updateTaskPriority(int index, Task.Priority newPriority, String userId) {
        if (index >= 0 && index < tasks.size() && userId != null) {
            Task task = tasks.get(index);
            if (userId.equals(task.getAssignedUser())) {
                task.setPriority(newPriority);
            }
        }
    }

    public synchronized void updateTaskDescription(int index, String newDescription, String userId) {
        if (index >= 0 && index < tasks.size() && userId != null && newDescription != null) {
            Task task = tasks.get(index);
            if (userId.equals(task.getAssignedUser())) {
                task.setDescription(newDescription);
            }
        }
    }

    public synchronized void updateTaskDeadline(int index, LocalDate newDeadline, String userId) {
        if (index >= 0 && index < tasks.size() && userId != null) {
            Task task = tasks.get(index);
            if (userId.equals(task.getAssignedUser())) {
                task.setDeadline(newDeadline);
            }
        }
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    public List<Task> getUserTasks(String userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(userTasks.getOrDefault(userId, new CopyOnWriteArrayList<>()));
    }

    public synchronized List<Task> getGroupTasks(String groupId) {
        if (groupId == null) {
            return Collections.emptyList();
        }
        
        List<Task> groupTasks = new ArrayList<>();
        for (Task task : tasks) {
            if (groupId.equals(task.getGroupId())) {
                groupTasks.add(task);
            }
        }
        return groupTasks;
    }

    public synchronized List<String> getGroupMembers(String groupId) {
        if (groupId == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(groups.getOrDefault(groupId, new CopyOnWriteArrayList<>()));
    }

    public synchronized boolean isUserInGroup(String userId, String groupId) {
        if (userId == null || groupId == null) {
            return false;
        }
        List<String> members = groups.get(groupId);
        return members != null && members.contains(userId);
    }

    public synchronized int getTaskCountForUser(String userId) {
        if (userId == null) {
            return 0;
        }
        List<Task> userTasksList = userTasks.get(userId);
        return userTasksList != null ? userTasksList.size() : 0;
    }

    public synchronized int getCompletedTaskCountForUser(String userId) {
        if (userId == null) {
            return 0;
        }
        List<Task> userTasksList = userTasks.get(userId);
        if (userTasksList == null) {
            return 0;
        }
        
        int count = 0;
        for (Task task : userTasksList) {
            if (task.getStatus() == Task.Status.COMPLETED) {
                count++;
            }
        }
        return count;
    }
}