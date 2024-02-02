package cn.shabbywu.imagej.utils;

import java.util.HashMap;

public class BestThresholdStore {
    public static class BestThreshold {
        public int minThreshold;
        public int maxThreshold;

        public BestThreshold(int minThreshold, int maxThreshold) {
            this.minThreshold = minThreshold;
            this.maxThreshold = maxThreshold;
        }
    }

    static HashMap<String, BestThreshold> store = new HashMap<>();

    public static BestThreshold getBestThreshold(String channel) {
        if (store.containsKey(channel)) {
            return store.get(channel);
        }
        // TODO: Auto 最佳阈值
        store.put(channel, new BestThreshold(70, 255));
        return store.get(channel);
    }

    public static void setBestThreshold(String channel, BestThreshold bestThreshold) {
        store.put(channel, bestThreshold);
    }
}
