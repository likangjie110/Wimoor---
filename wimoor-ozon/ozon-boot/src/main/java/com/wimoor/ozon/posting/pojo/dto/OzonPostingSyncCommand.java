package com.wimoor.ozon.posting.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OzonPostingSyncCommand {

    private String authId;

    private Integer sinceDays;

    private Boolean useCursor;
}
