package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointServiceUnitTest {

    @InjectMocks
    private PointService pointService;
    @Mock
    private UserPointTable userPointTable;
    @Mock
    private PointHistoryTable pointHistoryTable;

    @DisplayName("포인트 충전 시, 포인트가 증가하고 내역이 저장된다.")
    @Test
    void chargePoint_shouldIncreasePointAndSavePointHistory() {
        // given
        long userId = 1L;
        long amount = 1000L;
        long initialPoints = 5000L;
        long currentTime = System.currentTimeMillis();

        UserPoint mockUserPoint = new UserPoint(userId, initialPoints, currentTime);
        PointHistory mockPointHistory = new PointHistory(1L, userId, amount, TransactionType.CHARGE, currentTime);

        when(userPointTable.selectById(anyLong())).thenReturn(mockUserPoint);
        when(pointHistoryTable.insert(anyLong(), anyLong(), any(TransactionType.class), anyLong())).thenReturn(mockPointHistory);
        when(userPointTable.insertOrUpdate(anyLong(), anyLong())).thenReturn(new UserPoint(userId, initialPoints + amount, currentTime));

        // when
        UserPoint updatedPoint = pointService.chargePoint(userId, amount);

        // then
        assertThat(updatedPoint.point()).isEqualTo(initialPoints + amount);

        verify(userPointTable).selectById(anyLong());
        verify(pointHistoryTable).insert(anyLong(), anyLong(), any(TransactionType.class), anyLong());
        verify(userPointTable).insertOrUpdate(anyLong(), anyLong());
    }

    /*
    * 포인트 충전 정책 추가 -> 포인트 충전은 최소 1,000원 이상부터 가능합니다.
    */
    @DisplayName("포인트 충전 시, 입력된 값이 1000 미만인 경우 예외가 발생한다.")
    @Test
    void chargePoint_shouldThrowException_whenAmountIsLessThan1000() {
        // given
        long id = 1L;
        long invalidAmount = 999L;

        // when //then
        assertThatThrownBy(() -> pointService.chargePoint(id, invalidAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("포인트 충전은 1,000원 이상부터 가능합니다.");
    }

    @DisplayName("포인트 사용 시, 포인트가 감소하고 내역이 저장된다.")
    @Test
    void usedPoint_shouldReducePointAndSavePointHistory() {
        // given
        long userId = 1L;
        long amount = 1000L;
        long initialPoints = 5000L;
        long currentTime = System.currentTimeMillis();

        UserPoint mockUserPoint = new UserPoint(userId, initialPoints, currentTime);
        PointHistory mockPointHistory = new PointHistory(1L, userId, amount, TransactionType.USE, currentTime);

        when(userPointTable.selectById(anyLong())).thenReturn(mockUserPoint);
        when(pointHistoryTable.insert(anyLong(), anyLong(), any(TransactionType.class), anyLong())).thenReturn(mockPointHistory);
        when(userPointTable.insertOrUpdate(anyLong(), anyLong())).thenReturn(new UserPoint(userId, initialPoints - amount, currentTime));

        // when
        UserPoint resultUserPoint = pointService.usePoint(userId, amount);

        //then
        assertThat(resultUserPoint.id()).isEqualTo(userId);
        assertThat(resultUserPoint.point()).isEqualTo(initialPoints - amount);
        verify(userPointTable).selectById(anyLong());
        verify(pointHistoryTable).insert(anyLong(), anyLong(), any(TransactionType.class), anyLong());
        verify(userPointTable).insertOrUpdate(anyLong(), anyLong());
    }

    @DisplayName("포인트 사용 시, 입력된 값이 회원의 보유 포인트보다 큰 경우 예외가 발생한다.")
    @Test
    void usePoint_shouldThrowException_whenAmountIsGreaterThanUserPoint() {
        // given
        long userId = 1L;
        long amount = 2000L;
        long initialPoint = 1000L;

        UserPoint mockUserPoint = new UserPoint(userId, initialPoint, System.currentTimeMillis());
        when(userPointTable.selectById(anyLong())).thenReturn(mockUserPoint);

        // when //then
        assertThatThrownBy(() -> pointService.usePoint(userId, amount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔고 부족");
    }
}