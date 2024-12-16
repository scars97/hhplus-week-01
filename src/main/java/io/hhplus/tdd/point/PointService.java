package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointService {

    private static final long MIN_CHARGE_AMOUNT = 1000L;

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    @Autowired
    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    public UserPoint getUserPoint(long id) {
        return userPointTable.selectById(id);
    }

    public List<PointHistory> getPointHistories(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }

    public UserPoint chargePoint(long id, long amount) {
        if (amount < MIN_CHARGE_AMOUNT) {
            throw new IllegalArgumentException("포인트 충전은 1,000원 이상부터 가능합니다.");
        }

        UserPoint userPoint = userPointTable.selectById(id);
        PointHistory pointHistory = pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
        long resultPoint = userPoint.addPoint(pointHistory.amount());
        return userPointTable.insertOrUpdate(id, resultPoint);
    }

    public UserPoint usePoint(long id, long amount) {
        UserPoint userPoint = userPointTable.selectById(id);
        if (userPoint.point() < amount) {
            throw new IllegalArgumentException("잔고 부족");
        }

        PointHistory pointHistory = pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis());
        long resultPoint = userPoint.reducePoint(pointHistory.amount());
        return userPointTable.insertOrUpdate(id, resultPoint);
    }
}