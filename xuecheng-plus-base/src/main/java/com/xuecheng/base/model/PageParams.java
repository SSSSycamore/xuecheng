package com.xuecheng.base.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageParams {
    @ApiModelProperty(value = "查询页码")
    private Long pageNo = 1L;
    @ApiModelProperty(value = "每页记录数")
    private Long pageSize = 10L;
}
