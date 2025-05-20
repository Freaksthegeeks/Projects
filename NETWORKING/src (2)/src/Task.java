import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

public class Task implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum Priority { LOW, MEDIUM, HIGH }
    public enum Status { PENDING, IN_PROGRESS, COMPLETED }

    private final String id;
    private String description;
    private Priority priority;
    private Status status;
    private final String assignedUser;
    private final String groupId;
    private LocalDate deadline;

    public Task(String description, Priority priority, String userId, String groupId, LocalDate deadline) {
        this.id = UUID.randomUUID().toString();
        this.description = description;
        this.priority = priority;
        this.status = Status.PENDING;
        this.assignedUser = userId;
        this.groupId = groupId;
        this.deadline = deadline;
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getAssignedUser() { return assignedUser; }
    public String getGroupId() { return groupId; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    @Override
    public String toString() {
        return String.format("%s | %s | %s | %s | Due: %s", 
            id.substring(0, 8), description, priority, status, 
            deadline != null ? deadline.toString() : "None");
    }
}