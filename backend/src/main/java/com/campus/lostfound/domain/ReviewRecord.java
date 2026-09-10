package com.campus.lostfound.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_record")
public class ReviewRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long itemId;
    private Long reviewerId;
    private String result;
    private String comment;
    private LocalDateTime reviewTime;
}
