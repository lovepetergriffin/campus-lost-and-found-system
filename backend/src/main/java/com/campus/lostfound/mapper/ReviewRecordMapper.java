package com.campus.lostfound.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.lostfound.domain.ReviewRecord;

/**
 * {@link ReviewRecord} 的数据访问接口。
 *
 * <p>目前审核记录的用法只有"插入一条"，通用 CRUD 能力已经够用，
 * 因此这里不定义额外方法。日后若要做审核工作量统计或按物品查历史轨迹，
 * 再在此追加对应的聚合查询。
 */
public interface ReviewRecordMapper extends BaseMapper<ReviewRecord> {
}
