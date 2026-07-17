package com.qm.archiver;

import com.qm.archiver.entity.MessageArchive;
import com.qm.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "消息归档")
@RestController
@RequestMapping("/api/v1/archives")
@RequiredArgsConstructor
public class MessageArchiveController {

    private final MessageArchiveService archiveService;

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
}
