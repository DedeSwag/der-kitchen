package com.der.kitchen.notification.service;

import com.der.kitchen.notification.vo.AdminRealtimeEventVO;

import java.util.List;

record RealtimeReplayBatch(List<AdminRealtimeEventVO> events, boolean resetRequired, long cursor) {

    static RealtimeReplayBatch replay(List<AdminRealtimeEventVO> events, long cursor) {
        return new RealtimeReplayBatch(events, false, cursor);
    }

    static RealtimeReplayBatch reset(long cursor) {
        return new RealtimeReplayBatch(List.of(), true, cursor);
    }
}
