package com.sakury.entity.query;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BaseParam {
    private SimplePage simplePage;
    private Integer pageNo;
    private Integer pageSize;
    private String orderBy;
}
