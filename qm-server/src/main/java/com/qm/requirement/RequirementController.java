package com.qm.requirement;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qm.common.Result;
import com.qm.requirement.entity.Requirement;
import com.qm.requirement.entity.RequirementVersion;
import com.qm.requirement.entity.RequirementStakeholder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "需求管理")
@RestController
@RequestMapping("/api/v1/requirements")
@RequiredArgsConstructor
public class RequirementController {

    private final RequirementService requirementService;

    @Operation(summary = "需求列表")
    @GetMapping
    public Result<Page<Requirement>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String reqType,
            @RequestParam(required = false) String productLine,
            @RequestParam(required = false) String keyword) {
        return Result.ok(requirementService.page(page, size, status, reqType, productLine, keyword));
    }

    @Operation(summary = "需求详情")
    @GetMapping("/{id}")
    public Result<Requirement> get(@PathVariable String id) {
        return Result.ok(requirementService.getById(id));
    }

    @Operation(summary = "创建需求")
    @PostMapping
    public Result<Requirement> create(@Valid @RequestBody Requirement req,
                                      @RequestHeader("X-User-Id") String userId) {
        return Result.ok(requirementService.create(req, userId));
    }

    @Operation(summary = "保存新版本")
    @PostMapping("/{id}/versions")
    public Result<RequirementVersion> saveVersion(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") String userId) {
        return Result.ok(requirementService.saveVersion(
            id, body.get("content"), body.get("fieldsData"),
            body.get("changeSummary"), userId));
    }

    @Operation(summary = "当前版本")
    @GetMapping("/{id}/current-version")
    public Result<RequirementVersion> currentVersion(@PathVariable String id) {
        return Result.ok(requirementService.getCurrentVersion(id));
    }

    @Operation(summary = "版本列表")
    @GetMapping("/{id}/versions")
    public Result<List<RequirementVersion>> versions(@PathVariable String id) {
        return Result.ok(requirementService.getVersions(id));
    }

    @Operation(summary = "干系人列表")
    @GetMapping("/{id}/stakeholders")
    public Result<List<RequirementStakeholder>> stakeholders(@PathVariable String id) {
        return Result.ok(requirementService.getStakeholders(id));
    }

    @Operation(summary = "添加干系人")
    @PostMapping("/{id}/stakeholders")
    public Result<Void> addStakeholder(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") String userId) {
        requirementService.addStakeholder(id, body.get("userId"), body.get("role"), userId);
        return Result.ok();
    }

    @Operation(summary = "状态流转")
    @PostMapping("/{id}/transitions")
    public Result<Void> transition(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") String userId) {
        requirementService.transition(id, body.get("action"), userId, body.get("comment"));
        return Result.ok();
    }
}
