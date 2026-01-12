package com.sakury.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class UserInfoTokenDto implements Serializable {
    private String userId;
    private String nickName;
    private String avatar;
    private Long expireTime;
    private String token;

    private Integer fanCount;
    private Integer currentCoinCount;
    private Integer focusCount;
}
