package com.wimoor.ozon.auth.pojo.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("t_ozon_auth")
public class OzonAuth {

    @TableId("id")
    private String id;

    @TableField("shop_id")
    private String shopId;

    @TableField("name")
    private String name;

    @TableField("client_id")
    private String clientId;

    @TableField("api_key_ciphertext")
    private String apiKeyCiphertext;

    @TableField("api_key_fingerprint")
    private String apiKeyFingerprint;

    @TableField("status")
    private String status;

    @TableField("disabled")
    private Boolean disabled;

    @TableField("last_sync_status")
    private String lastSyncStatus;

    @TableField("last_sync_message")
    private String lastSyncMessage;

    @TableField("last_sync_time")
    private Date lastSyncTime;

    @TableField("created_by")
    private String createdBy;

    @TableField("updated_by")
    private String updatedBy;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableField(exist = false)
    private String apiKeyPlaintext;

    public String getApiKey() {
        return apiKeyPlaintext;
    }

    public void setApiKey(String apiKey) {
        this.apiKeyPlaintext = apiKey;
    }
}
