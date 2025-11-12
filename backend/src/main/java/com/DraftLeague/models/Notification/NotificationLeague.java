package com.DraftLeague.models.Notification;

import com.DraftLeague.models.Chat.Chat;
import com.DraftLeague.models.League.League;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class NotificationLeague {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false, unique = true)
    private Integer id;
    
    @ManyToOne(optional = true)
    @NotNull
    @Valid
    private League league;
}
