package com.wimoor.ozon.task.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_sync_cursor")
public class OzonSyncCursor {

    @TableId("id")
    private String id;

    @TableField("auth_id")
    private String authId;

    @TableField("shop_id")
    private String shopId;

    @TableField("cursor_type")
    private String cursorType;

    @TableField("cursor_value")
    private String cursorValue;

    @TableField("last_synced_at")
    private Date lastSyncedAt;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
