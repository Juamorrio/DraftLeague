package com.DraftLeague.models.Notification;
import com.DraftLeague.models.Notification.NotificationType;
import com.DraftLeague.models.Notification.NotificationLeague;
import java.util.Date;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import com.DraftLeague.models.Notification.Notification;

@Getter
@Setter
@Entity
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false, unique = true)
    private Integer id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Size(max = 500)
    @Column(name = "payload", length = 500)
    private String payload;

    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    @PastOrPresent
    @Column(name = "created_at", nullable = false, updatable = false)
    private Date createdAt;

    @ManyToOne(optional = false)
    @NotNull
    @Valid
    private NotificationLeague notificationLeague;
}
