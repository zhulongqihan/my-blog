package com.myblog.repository;

import com.myblog.entity.AppliedViewCountBatch;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppliedViewCountBatchRepository
        extends JpaRepository<AppliedViewCountBatch, String> {}
