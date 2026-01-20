package com.DraftLeague.models.Player.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerImportDto {
	private String id;
	private String fullName;
	private String position; 
	private String avatarUrl;
    private Integer teamId;
	private String marketValue;
	
}
