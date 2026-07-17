package com.qm.groupengine;

import com.qm.common.Result;
import com.qm.groupengine.entity.RequirementGroup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "群管理")
@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @Operation(summary = "为需求建群")
    @PostMapping("/requirements/{reqId}")
    public Result<RequirementGroup> createGroup(
            @PathVariable String reqId,
            @RequestHeader("X-User-Id") String userId) {
        return Result.ok(groupService.createGroup(reqId, userId));
    }

    @Operation(summary = "解散需求群")
    @DeleteMapping("/requirements/{reqId}")
    public Result<Void> dissolveGroup(
            @PathVariable String reqId,
            @RequestHeader("X-User-Id") String userId) {
        groupService.dissolveGroup(reqId, userId);
        return Result.ok();
    }

    @Operation(summary = "查询需求群")
    @GetMapping("/requirements/{reqId}")
    public Result<RequirementGroup> getGroup(@PathVariable String reqId) {
        return Result.ok(groupService.getByReqId(reqId));
    }

    @Operation(summary = "活跃群列表")
    @GetMapping
    public Result<List<RequirementGroup>> listActive() {
        return Result.ok(groupService.listActiveGroups());
    }
}
