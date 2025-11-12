package com.DraftLeague.models.user;

import com.DraftLeague.models.Chat.Message;
import com.DraftLeague.models.Notification.Notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false, unique = true)
    private Integer id;

	@NotNull
	@Column(unique = true)
	@Size(min = 3, max = 40)
	private String username;

	@NotNull
	@Column(unique = true)
	@Email
	@Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
	private String email;

	@NotNull
	@Size(min = 8, max = 100)
	private String password;

	@NotNull
	@Size(max = 40)
	private String displayName;

	@ManyToOne
	@JoinColumn(name = "notification_id", nullable = true)
	private Notification notification;

	@ManyToOne
	@JoinColumn(name = "message_id", nullable = true)
	private Message message; 

}
