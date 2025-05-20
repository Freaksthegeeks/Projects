import java.io.Serializable;

public class JoinGroupRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    public final String userId;
    public final String groupId;

    public JoinGroupRequest(String userId, String groupId) {
        this.userId = userId;
        this.groupId = groupId;
    }
}
