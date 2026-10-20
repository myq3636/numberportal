package com.liugh.rcs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.liugh.entity.RoutingInfo;

public class RcsRoutingSnapshotTest {

    @Test
    public void filtersNonEffectiveAndNonRcsSuppliers() {
        List<RoutingInfo> infos = new ArrayList<RoutingInfo>();
        infos.add(route(1, 3, 100, "CN", 200, 0, 100)); // effective + rcs
        infos.add(route(2, 3, 100, "CN", 201, 0, 100)); // non-rcs (predicate filters)
        infos.add(route(3, 3, 100, "CN", 202, 1, 100)); // status != 0

        RcsRoutingSnapshot snapshot = RcsRoutingSnapshot.build(infos, rssid -> rssid == 200);

        assertEquals(Integer.valueOf(200),
                snapshot.select(new RcsRoutingContext(100, "CN", null, null), new Random(1)));
    }

    @Test
    public void defaultsRatioTo100WhenNotConfigured() {
        List<RoutingInfo> infos = new ArrayList<RoutingInfo>();
        infos.add(route(1, 3, 100, "CN", 200, 0, 0));   // ratio 0 -> default 100
        infos.add(route(2, 3, 100, "CN", 201, 0, 100));

        RcsRoutingSnapshot snapshot = RcsRoutingSnapshot.build(infos, rssid -> true);

        for (int i = 0; i < 50; i++) {
            Integer r = snapshot.select(new RcsRoutingContext(100, "CN", null, null), new Random(i));
            assertTrue(r == 200 || r == 201);
        }
    }

    @Test
    public void precedenceSenderOverContentOverCommon() {
        List<RoutingInfo> infos = new ArrayList<RoutingInfo>();
        infos.add(route(1, 3, 100, "CN", 200, 0, 100));      // common -> 200
        infos.add(route(2, 2, 100, "kw-1", 201, 0, 100));    // content -> 201
        infos.add(route(3, 1, 100, "brandA", 202, 0, 100));  // sender -> 202

        RcsRoutingSnapshot snapshot = RcsRoutingSnapshot.build(infos, rssid -> true);

        assertEquals(Integer.valueOf(202),
                snapshot.select(new RcsRoutingContext(100, "CN", "brandA", "kw-1"), new Random(1)));
        assertEquals(Integer.valueOf(201),
                snapshot.select(new RcsRoutingContext(100, "CN", null, "kw-1"), new Random(1)));
        assertEquals(Integer.valueOf(200),
                snapshot.select(new RcsRoutingContext(100, "CN", null, null), new Random(1)));
    }

    @Test
    public void weightedSelectionRespectsRatio() {
        List<RoutingInfo> infos = new ArrayList<RoutingInfo>();
        infos.add(route(1, 3, 100, "CN", 200, 0, 1));
        infos.add(route(2, 3, 100, "CN", 201, 0, 99));

        RcsRoutingSnapshot snapshot = RcsRoutingSnapshot.build(infos, rssid -> true);

        int count200 = 0;
        int n = 10000;
        for (int i = 0; i < n; i++) {
            if (snapshot.select(new RcsRoutingContext(100, "CN", null, null), new Random(i)) == 200) {
                count200++;
            }
        }
        double ratio = count200 / (double) n;
        assertTrue("ratio=" + ratio, ratio > 0.005 && ratio < 0.02);
    }

    @Test
    public void returnsNullWhenNoRoute() {
        RcsRoutingSnapshot snapshot = RcsRoutingSnapshot.empty();
        assertNull(snapshot.select(new RcsRoutingContext(100, "CN", null, null), new Random(1)));
    }

    private static RoutingInfo route(int id, int type, int ossid, String key, int rssid, int status, int ratio) {
        RoutingInfo r = new RoutingInfo();
        r.setId(id);
        r.setType(type);
        r.setOssid(ossid);
        r.setRssid(rssid);
        r.setStatus(status);
        r.setRatio(ratio);
        if (type == 1) {
            r.setSender(key);
        } else if (type == 2) {
            r.setContentKeywordId(key);
        } else {
            r.setCountryCode(key);
        }
        return r;
    }
}
