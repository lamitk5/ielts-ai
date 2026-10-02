package com.ieltsaitutor.mock;

import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class MockTestStateMachine {

    public MockTestSession transition(MockTestSession session, MockTestCommand command) {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(command, "command must not be null");

        if (session.status().isTerminal()) {
            throw new MockConflictException("Không thể thao tác trên bài thi thử đã kết thúc (" + session.status() + ")");
        }

        return switch (command) {
            case START -> {
                if (session.status() != MockTestSessionStatus.NOT_STARTED) {
                    throw new MockConflictException("Bài thi thử đã được bắt đầu");
                }
                yield session.withStatus(MockTestSessionStatus.IN_PROGRESS);
            }
            case PAUSE -> {
                if (session.status() != MockTestSessionStatus.IN_PROGRESS) {
                    throw new MockConflictException("Chỉ có thể tạm dừng bài thi đang làm");
                }
                yield session.withStatus(MockTestSessionStatus.PAUSED);
            }
            case RESUME -> {
                if (session.status() != MockTestSessionStatus.PAUSED) {
                    throw new MockConflictException("Bài thi không ở trạng thái tạm dừng");
                }
                yield session.withStatus(MockTestSessionStatus.IN_PROGRESS);
            }
            case NEXT_SECTION -> {
                if (session.status() != MockTestSessionStatus.IN_PROGRESS) {
                    throw new MockConflictException("Bài thi phải ở trạng thái đang làm để chuyển phần");
                }
                int nextIndex = session.currentSectionIndex() + 1;
                if (nextIndex >= session.sections().size() && !session.sections().isEmpty()) {
                    throw new MockConflictException("Đã ở phần thi cuối cùng");
                }
                yield session.withCurrentSection(nextIndex);
            }
            case PREV_SECTION -> {
                if (session.status() != MockTestSessionStatus.IN_PROGRESS) {
                    throw new MockConflictException("Bài thi phải ở trạng thái đang làm để chuyển phần");
                }
                int prevIndex = Math.max(0, session.currentSectionIndex() - 1);
                yield session.withCurrentSection(prevIndex);
            }
            case SUBMIT -> {
                if (session.status() != MockTestSessionStatus.IN_PROGRESS && session.status() != MockTestSessionStatus.PAUSED) {
                    throw new MockConflictException("Không thể nộp bài thi ở trạng thái " + session.status());
                }
                yield session.withStatus(MockTestSessionStatus.SUBMITTED);
            }
            case EXPIRE -> session.withStatus(MockTestSessionStatus.EXPIRED);
        };
    }
}
