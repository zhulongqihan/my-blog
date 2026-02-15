package com.myblog.service;

import com.myblog.entity.OperationLog;
import com.myblog.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务
 * 设计目标：异步保存操作日志，避免阻塞主业务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {
    
    private final OperationLogRepository operationLogRepository;
    
    /**
     * 异步保存操作日志
     * 
     * @param operationLog 操作日志对象
     * 
     * 设计亮点：
     * - 使用@Async注解，异步执行，不阻塞主业务
     * - 即使日志保存失败，也不影响主业务
     * - 使用独立的线程池，避免影响其他异步任务
     */
    @Async("asyncExecutor")
    public void saveLog(OperationLog operationLog) {
        try {
            operationLogRepository.save(operationLog);
            log.debug("操作日志保存成功：{}", operationLog.getDescription());
        } catch (Exception e) {
            // 日志保存失败不影响主业务，只记录错误日志
            log.error("操作日志保存失败", e);
        }
    }
}
