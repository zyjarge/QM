package com.qm.archiver;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qm.archiver.entity.MessageArchive;
import com.qm.archiver.mapper.MessageArchiveMapper;
import com.qm.common.exception.BizException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 群档案导出：需求的归档消息 → JSON → MinIO（qm-archives bucket）
 * 设计依据 03-im-integration：需求关闭解散群前，完整记录导出归档、永不丢失。
 * 注意：直接依赖 MessageArchiveMapper（而非 MessageArchiveService），避免与 GroupService 形成循环依赖。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveExportService {

    private final MinioClient minioClient;
    private final MessageArchiveMapper archiveMapper;

    @Value("${minio.bucket-archives}")
    private String bucketArchives;

    /**
     * 导出需求的全部归档消息到 MinIO，返回对象路径（qm-archives/{reqId}/messages.json）
     */
    public String exportRequirementArchive(String reqId) {
        List<MessageArchive> messages = archiveMapper.selectList(
            new LambdaQueryWrapper<MessageArchive>()
                .eq(MessageArchive::getRequirementId, reqId)
                .orderByAsc(MessageArchive::getMsgTime));

        JSONObject doc = new JSONObject();
        doc.put("requirementId", reqId);
        doc.put("exportedAt", LocalDateTime.now().toString());
        doc.put("messageCount", messages.size());
        doc.put("messages", messages);

        String objectName = reqId + "/messages.json";
        byte[] bytes = doc.toJSONString().getBytes(StandardCharsets.UTF_8);
        try {
            minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketArchives)
                .object(objectName)
                .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                .contentType("application/json")
                .build());
        } catch (Exception e) {
            log.error("Archive export failed: req={}", reqId, e);
            throw BizException.illegalState("档案导出 MinIO 失败: " + e.getMessage());
        }

        String path = bucketArchives + "/" + objectName;
        log.info("Archive exported: req={} path={} messages={}", reqId, path, messages.size());
        return path;
    }
}
