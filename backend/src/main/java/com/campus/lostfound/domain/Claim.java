package com.campus.lostfound.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("claim")
public class Claim {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long itemId;
    private Long applicantId;
    private String description;
    private String proof;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
