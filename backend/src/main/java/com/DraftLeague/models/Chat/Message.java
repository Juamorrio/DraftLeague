package com.DraftLeague.models.Chat;

import java.util.Date;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false, unique = true)
    private Integer id;

    @NotNull
    @Size(max = 500)
    @Column(name = "body", nullable = false, length = 500)
    private String body;

    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    @PastOrPresent
    @Column(name = "sent_at", nullable = false, updatable = false)
    private Date sentAt;
}
