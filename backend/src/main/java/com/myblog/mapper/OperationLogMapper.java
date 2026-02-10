package com.myblog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.myblog.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志Mapper
 * 继承MyBatis-Plus的BaseMapper，自动拥有CRUD方法
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
    // MyBatis-Plus已提供基础CRUD方法，无需额外定义
    // 如需复杂查询，可在此添加自定义方法
}
