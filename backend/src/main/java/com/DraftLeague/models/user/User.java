package com.DraftLeague.models.user;

import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
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

	@NotNull
	@Past
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
	private Date createdAt = new Date();

}

