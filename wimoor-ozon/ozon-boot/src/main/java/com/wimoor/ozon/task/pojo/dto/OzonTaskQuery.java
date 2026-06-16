package com.wimoor.ozon.task.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonTaskQuery {

    private String authId;

    private String jobType;

    private String status;
}
