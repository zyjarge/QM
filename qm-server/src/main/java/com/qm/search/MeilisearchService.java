package com.qm.search;

import com.qm.common.MeilisearchConfig;
import com.qm.requirement.entity.Requirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Meilisearch 搜索服务
 * - 索引需求文档
 * - 全文搜索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeilisearchService {

    private final MeilisearchConfig config;
    private final RestTemplate restTemplate;

    /**
     * 初始化索引（在启动时调用）
     */
    public void initIndex() {
        try {
            // 创建索引（已存在则忽略）
            String url = config.getHost() + "/indexes";
            HttpHeaders headers = buildHeaders();
            Map<String, Object> body = Map.of(
                "uid", MeilisearchConfig.INDEX_REQUIREMENTS,
                "primaryKey", "id");
            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
            try {
                restTemplate.exchange(url, HttpMethod.POST, req, String.class);
                log.info("Meili index created: {}", MeilisearchConfig.INDEX_REQUIREMENTS);
            } catch (Exception e) {
                // 索引可能已存在，忽略
                log.debug("Index may already exist: {}", e.getMessage());
            }

            // 设置可搜索字段
            String settingsUrl = config.getHost() + "/indexes/" + MeilisearchConfig.INDEX_REQUIREMENTS + "/settings";
            Map<String, Object> settings = Map.of(
                "searchableAttributes", List.of("title", "reqNo", "reqType", "priority", "productLine", "createdBy"),
                "filterableAttributes", List.of("status", "priority", "reqType", "productLine")
            );
            restTemplate.exchange(settingsUrl, HttpMethod.PATCH, new HttpEntity<>(settings, headers), String.class);
            log.info("Meili index settings updated");
        } catch (Exception e) {
            log.warn("Failed to init Meili index: {}", e.getMessage());
        }
    }

    /**
     * 索引单个需求
     */
    public void indexRequirement(Requirement req) {
        try {
            String url = config.getHost() + "/indexes/" + MeilisearchConfig.INDEX_REQUIREMENTS + "/documents";
            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("id", req.getId());
            doc.put("reqNo", req.getReqNo());
            doc.put("title", req.getTitle());
            doc.put("reqType", req.getReqType());
            doc.put("priority", req.getPriority());
            doc.put("productLine", req.getProductLine());
            doc.put("status", req.getStatus());
            doc.put("createdBy", req.getCreatedBy());
            doc.put("createdAt", req.getCreatedAt() != null ? req.getCreatedAt().toString() : null);

            HttpEntity<List<Map<String, Object>>> req2 =
                new HttpEntity<>(List.of(doc), buildHeaders());
            restTemplate.exchange(url, HttpMethod.POST, req2, String.class);
            log.debug("Requirement indexed: {}", req.getId());
        } catch (Exception e) {
            log.warn("Failed to index requirement {}: {}", req.getId(), e.getMessage());
        }
    }

    /**
     * 删除索引
     */
    public void deleteRequirement(String reqId) {
        try {
            String url = config.getHost() + "/indexes/" + MeilisearchConfig.INDEX_REQUIREMENTS
                + "/documents/" + reqId;
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(buildHeaders()), String.class);
        } catch (Exception e) {
            log.warn("Failed to delete indexed requirement {}: {}", reqId, e.getMessage());
        }
    }

    /**
     * 搜索
     */
    public Map<String, Object> search(String query, Map<String, Object> filters) {
        String url = config.getHost() + "/indexes/" + MeilisearchConfig.INDEX_REQUIREMENTS + "/search";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("q", query == null ? "" : query);
        if (filters != null && !filters.isEmpty()) {
            body.put("filter", formatFilter(filters));
        }

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, buildHeaders());
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, req, Map.class);
            return resp.getBody();
        } catch (Exception e) {
            log.warn("Meili search failed: {}", e.getMessage());
            return Map.of("hits", List.of(), "estimatedTotalHits", 0);
        }
    }

    private String formatFilter(Map<String, Object> filters) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : filters.entrySet()) {
            if (sb.length() > 0) sb.append(" AND ");
            sb.append(e.getKey()).append(" = '").append(e.getValue()).append("'");
        }
        return sb.toString();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!config.getApiKey().isEmpty()) {
            headers.set("Authorization", "Bearer " + config.getApiKey());
        }
        return headers;
    }
}
