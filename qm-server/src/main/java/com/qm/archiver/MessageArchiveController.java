package com.qm.archiver;

import com.qm.archiver.entity.MessageArchive;
import com.qm.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "消息归档")
@RestController
@RequestMapping("/api/v1/archives")
@RequiredArgsConstructor
public class MessageArchiveController {

    private final MessageArchiveService archiveService;
    private final ArchiveExportService exportService;

    @Operation(summary = "需求的消息归档列表")
    @GetMapping("/requirements/{reqId}")
    public Result<List<MessageArchive>> listByReqId(
            @PathVariable String reqId,
            @RequestParam(defaultValue = "100") Integer limit) {
        return Result.ok(archiveService.listByReqId(reqId, limit));
    }

    @Operation(summary = "需求的消息归档数量")
    @GetMapping("/requirements/{reqId}/count")
    public Result<Long> countByReqId(@PathVariable String reqId) {
        return Result.ok(archiveService.countByReqId(reqId));
    }

    @Operation(summary = "导出需求群档案到 MinIO（解散前自动调用，也可手动补导）")
    @PostMapping("/requirements/{reqId}/export")
    public Result<Map<String, String>> export(@PathVariable String reqId) {
        return Result.ok(Map.of("archivePath", exportService.exportRequirementArchive(reqId)));
    }
}
