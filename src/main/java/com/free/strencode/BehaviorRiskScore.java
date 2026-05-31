package com.free.strencode;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BehaviorRiskScore {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static class Behavior {
        public List<Long> moves = new ArrayList<>();
        public List<Long> clicks = new ArrayList<>();
        public List<Long> keys = new ArrayList<>();
        public List<Long> scrolls = new ArrayList<>();
        public Long duration;
    }

    public static class RiskResult {
        public int score;
        public String level;
        public List<String> reasons = new ArrayList<>();

        public RiskResult(int score, String level, List<String> reasons) {
            this.score = score;
            this.level = level;
            this.reasons = reasons;
        }

        @Override
        public String toString() {
            return "RiskResult{" +
                    "score=" + score +
                    ", level='" + level + '\'' +
                    ", reasons=" + reasons +
                    '}';
        }
    }

    public static RiskResult score(String encodedBehavior,String keyStr) {
        List<String> reasons = new ArrayList<>();

        try {
            encodedBehavior = Crypto.webDecrypt(encodedBehavior,keyStr);
            String json = URLDecoder.decode(encodedBehavior, StandardCharsets.UTF_8.name());
            Behavior b = objectMapper.readValue(json, Behavior.class);
            int score = 100;
            int moveCount = size(b.moves);
            int clickCount = size(b.clicks);
            int keyCount = size(b.keys);
            int scrollCount = size(b.scrolls);
            long duration = b.duration == null ? 0 : b.duration;
            // 1. duration 异常
            if (duration <= 0) {
                score -= 30;
                reasons.add("duration异常");
            } else if (duration < 1000) {
                score -= 15;
                reasons.add("行为采集时间过短");
            }
            // 2. 完全没有行为
            int total = moveCount + clickCount + keyCount + scrollCount;
            if (total == 0) {
                score -= 60;
                reasons.add("无任何用户行为");
            }
            // 3. 有点击/输入，但没有鼠标移动，比较可疑
            if ((clickCount > 0 || keyCount > 0) && moveCount == 0) {
                score -= 20;
                reasons.add("存在点击或键盘行为，但无鼠标移动");
            }
            // 4. 鼠标移动过少
            if (moveCount > 0 && moveCount < 3 && duration > 3000) {
                score -= 10;
                reasons.add("鼠标移动次数过少");
            }
            // 5. 鼠标移动过于频繁，可能是脚本刷数据
            if (moveCount >= 180) {
                score -= 10;
                reasons.add("鼠标移动次数接近上限，可能存在异常采样");
            }
            // 6. 点击过多
            if (clickCount > 30) {
                score -= 20;
                reasons.add("短时间点击次数过多");
            } else if (clickCount > 15) {
                score -= 10;
                reasons.add("短时间点击较频繁");
            }
            // 7. 键盘输入过多
            if (keyCount > 80) {
                score -= 15;
                reasons.add("短时间键盘事件过多");
            }
            // 8. 检测时间间隔是否过于规律
            if (isTooRegular(b.moves)) {
                score -= 20;
                reasons.add("鼠标事件间隔过于规律");
            }
            if (isTooRegular(b.clicks)) {
                score -= 15;
                reasons.add("点击事件间隔过于规律");
            }
            if (isTooRegular(b.keys)) {
                score -= 10;
                reasons.add("键盘事件间隔过于规律");
            }
            // 9. 检测重复时间戳
            if (hasTooManySameTimestamp(b.moves)) {
                score -= 15;
                reasons.add("鼠标事件存在大量重复时间戳");
            }
            if (hasTooManySameTimestamp(b.clicks)) {
                score -= 15;
                reasons.add("点击事件存在大量重复时间戳");
            }
            // 10. 基础加分：有自然行为
            if (moveCount >= 5) {
                score += 5;
            }
            if (clickCount > 0 && moveCount > 0) {
                score += 5;
            }
            if (scrollCount > 0 && moveCount > 0) {
                score += 5;
            }
            score = Math.max(0, Math.min(100, score));
            String level;
            if (score >= 70) {
                level = "LOW";
            } else if (score >= 40) {
                level = "MEDIUM";
            } else {
                level = "HIGH";
            }
            return new RiskResult(score, level, reasons);

        } catch (Exception e) {
            reasons.add("行为数据解析失败");
            return new RiskResult(0, "HIGH", reasons);
        }
    }

    private static int size(List<Long> list) {
        return list == null ? 0 : list.size();
    }

    /**
     * 判断事件间隔是否过于规律。
     * 例如：每隔 50ms、100ms 触发一次，比较像脚本。
     */
    private static boolean isTooRegular(List<Long> list) {
        if (list == null || list.size() < 8) {
            return false;
        }
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < list.size(); i++) {
            long diff = list.get(i) - list.get(i - 1);
            if (diff > 0) {
                intervals.add(diff);
            }
        }
        if (intervals.size() < 6) {
            return false;
        }
        double avg = intervals.stream().mapToLong(Long::longValue).average().orElse(0);
        double variance = 0;
        for (Long interval : intervals) {
            variance += Math.pow(interval - avg, 2);
        }
        variance = variance / intervals.size();
        double std = Math.sqrt(variance);
        // 平均间隔很稳定，标准差很小，认为可疑
        return avg > 0 && std < 5;
    }

    /**
     * 大量重复时间戳，可能是脚本批量 push Date.now()
     */
    private static boolean hasTooManySameTimestamp(List<Long> list) {
        if (list == null || list.size() < 10) {
            return false;
        }
        Map<Long, Integer> countMap = new HashMap<>();
        for (Long t : list) {
            countMap.put(t, countMap.getOrDefault(t, 0) + 1);
        }
        int maxSame = countMap.values().stream().max(Integer::compareTo).orElse(0);
        return maxSame >= 5;
    }

}