package com.qm.search;

import com.qm.common.Result;
import com.qm.requirement.RequirementService;
import com.qm.requirement.entity.Requirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "搜索")
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final MeilisearchService meiliService;
    private final RequirementService requirementService;

    @PostConstruct
    public void init() {
        meiliService.initIndex();
    }

    @Operation(summary = "全文搜索需求")
    @GetMapping("/requirements")
    public Result<Map<String, Object>> searchRequirements(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        java.util.Map<String, Object> filters = new java.util.HashMap<>();
        if (status != null) filters.put("status", status);
        if (priority != null) filters.put("priority", priority);
        return Result.ok(meiliService.search(q, filters));
    }

    @Operation(summary = "重建索引（管理用）")
    @PostMapping("/reindex")
    public Result<Integer> reindex() {
        // 拉所有需求重新索引
        List<Requirement> all = requirementService.listAll();
        int count = 0;
        for (Requirement req : all) {
            meiliService.indexRequirement(req);
            count++;
        }
        return Result.ok(count);
    }
}
