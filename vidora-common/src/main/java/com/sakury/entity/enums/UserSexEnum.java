package com.sakury.entity.enums;

import lombok.Getter;

@Getter
public enum UserSexEnum {
    WOMAN(0, "女"),
    MAN(1, "男"),
    UNKNOWN(2, "未知");
    private Integer type;
    private String desc;

    UserSexEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static UserSexEnum getByType(Integer type) {
        for (UserSexEnum value : UserSexEnum.values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return null;
    }
}
