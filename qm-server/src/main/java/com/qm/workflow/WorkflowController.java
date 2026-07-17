package com.qm.workflow;

import com.qm.common.Result;
import com.qm.workflow.entity.Baseline;
import com.qm.workflow.entity.ReviewFlow;
import com.qm.workflow.entity.ReviewVote;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "评审与基线")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WorkflowController {

    private final ReviewService reviewService;
    private final BaselineService baselineService;

    @Operation(summary = "发起评审")
    @PostMapping("/requirements/{id}/reviews")
    public Result<ReviewFlow> startReview(
            @PathVariable String id,
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-User-Id") String userId) {
        String reviewType = (String) body.getOrDefault("reviewType", "final");
        String mode = (String) body.getOrDefault("mode", "all");
        @SuppressWarnings("unchecked")
        List<String> voterIds = (List<String>) body.get("voterIds");
        return Result.ok(reviewService.startReview(id, reviewType, mode, voterIds, userId));
    }

    @Operation(summary = "评审投票")
    @PostMapping("/reviews/{flowId}/votes")
    public Result<ReviewVote> castVote(
            @PathVariable String flowId,
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") String userId) {
        return Result.ok(reviewService.castVote(
            flowId, body.get("voterId"), body.get("decision"),
            body.get("comment"), userId));
    }

    @Operation(summary = "评审列表")
    @GetMapping("/requirements/{id}/reviews")
    public Result<List<ReviewFlow>> listReviews(@PathVariable String id) {
        return Result.ok(reviewService.listFlows(id));
    }

    @Operation(summary = "当前评审")
    @GetMapping("/requirements/{id}/reviews/current")
    public Result<ReviewFlow> currentReview(@PathVariable String id) {
        return Result.ok(reviewService.getCurrentFlow(id));
    }

    @Operation(summary = "投票明细")
    @GetMapping("/reviews/{flowId}/votes")
    public Result<List<ReviewVote>> listVotes(@PathVariable String flowId) {
        return Result.ok(reviewService.listVotes(flowId));
    }

    @Operation(summary = "基线签认")
    @PostMapping("/requirements/{id}/baseline/sign")
    public Result<Baseline> signBaseline(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Real-IP", defaultValue = "127.0.0.1") String ip,
            @RequestHeader(value = "User-Agent", defaultValue = "unknown") String userAgent) {
        return Result.ok(baselineService.signBaseline(id, userId, ip, userAgent));
    }

    @Operation(summary = "当前基线")
    @GetMapping("/requirements/{id}/baseline")
    public Result<Baseline> currentBaseline(@PathVariable String id) {
        return Result.ok(baselineService.getCurrentBaseline(id));
    }

    @Operation(summary = "基线历史")
    @GetMapping("/requirements/{id}/baselines")
    public Result<List<Baseline>> listBaselines(@PathVariable String id) {
        return Result.ok(baselineService.listBaselines(id));
    }
}
