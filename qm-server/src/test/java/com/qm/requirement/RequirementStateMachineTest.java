package com.qm.requirement;

import com.qm.common.exception.BizException;
import com.qm.requirement.entity.Requirement;
import com.qm.requirement.mapper.RequirementMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 12 态状态机单测：转移表（RequirementState.canTransitionTo）+ 流转服务（RequirementStateMachine）。
 * RequirementMapper 全部 mock，不触 DB。
 */
@ExtendWith(MockitoExtension.class)
class RequirementStateMachineTest {

    @Mock
    private RequirementMapper requirementMapper;

    @InjectMocks
    private RequirementStateMachine stateMachine;

    private Requirement reqInState(RequirementState state) {
        Requirement req = new Requirement();
        req.setStatus(state.name());
        return req;
    }

    @Nested
    @DisplayName("转移表 canTransitionTo")
    class TransitionTable {

        @Test
        @DisplayName("主链路每一步均合法: draft→clarifying→pending_review→reviewing→pending_sign→baselined→developing→accepting→delivered→archived")
        void mainPathAllLegal() {
            assertThat(RequirementState.draft.canTransitionTo(RequirementState.clarifying)).isTrue();
            assertThat(RequirementState.clarifying.canTransitionTo(RequirementState.pending_review)).isTrue();
            assertThat(RequirementState.pending_review.canTransitionTo(RequirementState.reviewing)).isTrue();
            assertThat(RequirementState.reviewing.canTransitionTo(RequirementState.pending_sign)).isTrue();
            assertThat(RequirementState.pending_sign.canTransitionTo(RequirementState.baselined)).isTrue();
            assertThat(RequirementState.baselined.canTransitionTo(RequirementState.developing)).isTrue();
            assertThat(RequirementState.developing.canTransitionTo(RequirementState.accepting)).isTrue();
            assertThat(RequirementState.accepting.canTransitionTo(RequirementState.delivered)).isTrue();
            assertThat(RequirementState.delivered.canTransitionTo(RequirementState.archived)).isTrue();
        }

        @Test
        @DisplayName("合法回退: reviewing→clarifying, pending_sign→reviewing, baselined→reviewing, accepting→developing")
        void rollbackTransitionsLegal() {
            assertThat(RequirementState.reviewing.canTransitionTo(RequirementState.clarifying)).isTrue();
            assertThat(RequirementState.pending_sign.canTransitionTo(RequirementState.reviewing)).isTrue();
            assertThat(RequirementState.baselined.canTransitionTo(RequirementState.reviewing)).isTrue();
            assertThat(RequirementState.accepting.canTransitionTo(RequirementState.developing)).isTrue();
        }

        @Test
        @DisplayName("on_hold 挂起规则: 仅 developing 可挂起; 挂起后可恢复 developing 或归档 archived")
        void onHoldRules() {
            assertThat(RequirementState.developing.canTransitionTo(RequirementState.on_hold)).isTrue();
            assertThat(RequirementState.on_hold.canTransitionTo(RequirementState.developing)).isTrue();
            assertThat(RequirementState.on_hold.canTransitionTo(RequirementState.archived)).isTrue();
            // 其他状态不可挂起
            assertThat(RequirementState.baselined.canTransitionTo(RequirementState.on_hold)).isFalse();
            assertThat(RequirementState.accepting.canTransitionTo(RequirementState.on_hold)).isFalse();
            assertThat(RequirementState.draft.canTransitionTo(RequirementState.on_hold)).isFalse();
            // 挂起后不可跳到非恢复/归档状态
            assertThat(RequirementState.on_hold.canTransitionTo(RequirementState.clarifying)).isFalse();
            assertThat(RequirementState.on_hold.canTransitionTo(RequirementState.accepting)).isFalse();
            assertThat(RequirementState.on_hold.canTransitionTo(RequirementState.delivered)).isFalse();
        }

        @Test
        @DisplayName("非法跳转: 跨态跳跃一律拒绝")
        void illegalSkipsRejected() {
            assertThat(RequirementState.draft.canTransitionTo(RequirementState.baselined)).isFalse();
            assertThat(RequirementState.draft.canTransitionTo(RequirementState.reviewing)).isFalse();
            assertThat(RequirementState.draft.canTransitionTo(RequirementState.archived)).isFalse();
            assertThat(RequirementState.clarifying.canTransitionTo(RequirementState.baselined)).isFalse();
            assertThat(RequirementState.pending_review.canTransitionTo(RequirementState.developing)).isFalse();
            assertThat(RequirementState.pending_sign.canTransitionTo(RequirementState.developing)).isFalse();
            assertThat(RequirementState.accepting.canTransitionTo(RequirementState.archived)).isFalse();
            assertThat(RequirementState.developing.canTransitionTo(RequirementState.delivered)).isFalse();
        }

        @ParameterizedTest(name = "archived 是终态, 不可再流转到 {0}")
        @EnumSource(RequirementState.class)
        void archivedIsTerminal(RequirementState target) {
            assertThat(RequirementState.archived.canTransitionTo(target)).isFalse();
        }

        @ParameterizedTest(name = "cancelled 是终态, 不可再流转到 {0}")
        @EnumSource(RequirementState.class)
        void cancelledIsTerminal(RequirementState target) {
            assertThat(RequirementState.cancelled.canTransitionTo(target)).isFalse();
        }

        @ParameterizedTest(name = "{0} 不可自流转")
        @EnumSource(RequirementState.class)
        void noSelfTransition(RequirementState state) {
            assertThat(state.canTransitionTo(state)).isFalse();
        }
    }

    @Nested
    @DisplayName("transition 流转服务")
    class TransitionService {

        @Test
        @DisplayName("主链路逐步流转: 每步均更新状态并落库")
        void mainPathStepByStep() {
            RequirementState[] path = {
                RequirementState.draft,
                RequirementState.clarifying,
                RequirementState.pending_review,
                RequirementState.reviewing,
                RequirementState.pending_sign,
                RequirementState.baselined,
                RequirementState.developing,
                RequirementState.accepting,
                RequirementState.delivered,
                RequirementState.archived
            };
            for (int i = 0; i < path.length - 1; i++) {
                reset(requirementMapper); // 每步独立校验, 避免 verify 计数累计
                when(requirementMapper.selectById("REQ-1")).thenReturn(reqInState(path[i]));
                stateMachine.transition("REQ-1", path[i + 1], "user-pm", "step " + i);

                ArgumentCaptor<Requirement> captor = ArgumentCaptor.forClass(Requirement.class);
                verify(requirementMapper).updateById(captor.capture());
                assertThat(captor.getValue().getStatus()).isEqualTo(path[i + 1].name());
            }
        }

        @Test
        @DisplayName("非法流转抛 BizException(QM-4000) 且不落库")
        void illegalTransitionThrows() {
            when(requirementMapper.selectById("REQ-1")).thenReturn(reqInState(RequirementState.draft));

            assertThatThrownBy(() -> stateMachine.transition("REQ-1", RequirementState.baselined, "user-pm", "skip"))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo("QM-4000");
            verify(requirementMapper, never()).updateById(any(Requirement.class));
        }

        @Test
        @DisplayName("终态 archived 再流转抛异常")
        void terminalStateThrows() {
            when(requirementMapper.selectById("REQ-1")).thenReturn(reqInState(RequirementState.archived));

            assertThatThrownBy(() -> stateMachine.transition("REQ-1", RequirementState.developing, "user-pm", "reopen"))
                .isInstanceOf(BizException.class);
            verify(requirementMapper, never()).updateById(any(Requirement.class));
        }

        @Test
        @DisplayName("需求不存在抛 BizException(QM-3000)")
        void notFoundThrows() {
            when(requirementMapper.selectById("REQ-X")).thenReturn(null);

            assertThatThrownBy(() -> stateMachine.transition("REQ-X", RequirementState.clarifying, "user-pm", null))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo("QM-3000");
        }
    }

    @Nested
    @DisplayName("assertCan 前置校验")
    class AssertCan {

        @Test
        @DisplayName("合法流转不抛异常")
        void legalPasses() {
            assertThatCode(() -> stateMachine.assertCan(reqInState(RequirementState.developing), RequirementState.accepting))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("非法流转抛 BizException(QM-4000)")
        void illegalThrows() {
            assertThatThrownBy(() -> stateMachine.assertCan(reqInState(RequirementState.draft), RequirementState.delivered))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo("QM-4000");
        }
    }
}
