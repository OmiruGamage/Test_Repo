package au.edu.rmit.sept.webapp.admin;

import java.time.LocalDateTime;

/** One entry in the administrative action history. */
public class AdminActionView {

    private final Long id;
    private final String adminName;
    private final AdminActionType actionType;
    private final Long targetId;
    private final String targetLabel;
    private final String reason;
    private final LocalDateTime createdAt;

    public AdminActionView(
            Long id,
            String adminName,
            AdminActionType actionType,
            Long targetId,
            String targetLabel,
            String reason,
            LocalDateTime createdAt) {

        this.id = id;
        this.adminName = adminName;
        this.actionType = actionType;
        this.targetId = targetId;
        this.targetLabel = targetLabel;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getAdminName() {
        return adminName;
    }

    public AdminActionType getActionType() {
        return actionType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getTargetLabel() {
        return targetLabel;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
