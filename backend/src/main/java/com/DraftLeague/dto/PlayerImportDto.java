package com.DraftLeague.dto;

import lombok.Getter;
import lombok.Setter;
import com.DraftLeague.models.Player.Position;

@Getter
@Setter
public class PlayerImportDto {
	private String id;
	private String fullName;
	private String position; 
	private String avatarUrl;
    private Integer teamId;
	private Integer marketValue;
	
}
