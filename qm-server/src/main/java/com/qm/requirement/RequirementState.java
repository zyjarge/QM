package com.qm.requirement;

public enum RequirementState {
    draft,
    clarifying,
    pending_review,
    reviewing,
    pending_sign,
    baselined,
    developing,
    accepting,
    delivered,
    archived,
    on_hold,
    cancelled;

    public boolean canTransitionTo(RequirementState target) {
        return switch (this) {
            case draft -> target == clarifying;
            case clarifying -> target == pending_review;
            case pending_review -> target == reviewing;
            case reviewing -> target == pending_sign || target == clarifying;
            case pending_sign -> target == baselined || target == reviewing;
            case baselined -> target == developing || target == reviewing || target == archived;
            case developing -> target == accepting || target == on_hold;
            case accepting -> target == delivered || target == developing;
            case delivered -> target == archived;
            case on_hold -> target == developing || target == archived;
            default -> false;
        };
    }
}
