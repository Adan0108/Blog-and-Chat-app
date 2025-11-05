package com.blog.entity.social;

import com.blog.entity.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "follow_requests",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_fr_unique_pending",
                columnNames = {"requester_id", " target_id", " req_type", "status"}
        ),
        indexes = {
        @Index(name = "ix_fr_requester", columnList = "requester_id"),
        @Index(name = "ix_fr_target", columnList = "target_id"),
        @Index(name = "ix_fr_type", columnList = "req_type"),
        @Index(name = "ix_fr_status", columnList = "status"),
        })
public class FollowRequest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "target_id" , nullable = false)
    private User target;

    @Enumerated(EnumType.STRING) @Column(name = "req_type", nullable = false)
    private RequestType reqType;

    @Enumerated(EnumType.STRING) @Column(name = "status",nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "responded_at")
    private Instant respondedAt;

    // getters // setters

    public Long getId() { return id; }
    public User getRequester() { return requester; }
    public void setRequester( User requester ) { this.requester = requester; }
    public User getTarget() { return target; }
    public void setTarget(User target) { this.target = target; }
    public RequestType getReqType() { return reqType; }
    public void setReqType(RequestType reqType) { this.reqType = reqType; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Instant respondedAt) { this.respondedAt = respondedAt; }


}
