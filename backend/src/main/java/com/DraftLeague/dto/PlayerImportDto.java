package com.DraftLeague.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerImportDTO {
    private String id;
    private String fullName;
    private String position;
    private String avatarUrl;
    private Integer teamId;
    private Integer marketValue;
}
