// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class KeyCommandTest extends AbstractCommandTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void del() throws Exception {
        redis.set(key, value);
        assertEquals(1, (long) redis.del(key));
        redis.set(key + "1", value);
        redis.set(key + "2", value);
        assertEquals(2, (long) redis.del(key + "1", key + "2"));
    }

    @Test
    public void dump() throws Exception {
        assertNull(redis.dump("invalid"));
        redis.set(key, value);
        assertTrue(redis.dump(key).length > 0);
    }

    @Test
    public void exists() throws Exception {
        assertFalse(redis.exists(key));
        redis.set(key, value);
        assertTrue(redis.exists(key));
    }

    @Test
    public void expire() throws Exception {
        assertFalse(redis.expire(key, 10));
        redis.set(key, value);
        assertTrue(redis.expire(key, 10));
        assertEquals(10, (long) redis.ttl(key));
    }

    @Test
    public void expireat() throws Exception {
        Date expiration = new Date(System.currentTimeMillis() + 10000);
        assertFalse(redis.expireat(key, expiration));
        redis.set(key, value);
        assertTrue(redis.expireat(key, expiration));
        assertTrue(redis.ttl(key) >= 9);
    }

    @Test
    public void keys() throws Exception {
        assertEquals(list(), redis.keys("*"));
        Map<String, String> map = new HashMap<String, String>();
        map.put("one", "1");
        map.put("two", "2");
        map.put("three", "3");
        redis.mset(map);
        List<String> keys = redis.keys("???");
        assertEquals(2, keys.size());
        assertTrue(keys.contains("one"));
        assertTrue(keys.contains("two"));
    }

    @Test
    public void keysStreaming() throws Exception {
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<String>();

        assertEquals(list(), redis.keys("*"));
        Map<String, String> map = new HashMap<String, String>();
        map.put("one", "1");
        map.put("two", "2");
        map.put("three", "3");
        redis.mset(map);
        Long count = redis.keys(adapter, "???");
        assertEquals(2, count.intValue());

        List<String> keys = adapter.getList();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("one"));
        assertTrue(keys.contains("two"));
    }

    @Test
    public void move() throws Exception {
        redis.set(key, value);
        redis.move(key, 1);
        assertNull(redis.get(key));
        redis.select(1);
        assertEquals(value, redis.get(key));
    }

    @Test
    public void objectEncoding() throws Exception {
        redis.set(key, value);
        assertEquals("embstr", redis.objectEncoding(key));
        redis.set(key, String.valueOf(1));
        assertEquals("int", redis.objectEncoding(key));
    }

    @Test
    public void objectIdletime() throws Exception {
        redis.set(key, value);
        assertEquals(0, (long) redis.objectIdletime(key));
    }

    @Test
    public void objectRefcount() throws Exception {
        redis.set(key, value);
        assertTrue(redis.objectRefcount(key) > 0);
    }

    @Test
    public void persist() throws Exception {
        assertFalse(redis.persist(key));
        redis.set(key, value);
        assertFalse(redis.persist(key));
        redis.expire(key, 10);
        assertTrue(redis.persist(key));
    }

    @Test
    public void pexpire() throws Exception {
        assertFalse(redis.pexpire(key, 10));
        redis.set(key, value);
        assertTrue(redis.pexpire(key, 10));
        assertTrue(redis.pttl(key) <= 10 && redis.pttl(key) > 0);
    }

    @Test
    public void pexpireat() throws Exception {
        Date expiration = new Date(System.currentTimeMillis() + 100);
        assertFalse(redis.pexpireat(key, expiration));
        redis.set(key, value);
        assertTrue(redis.pexpireat(key, expiration));
        assertTrue(redis.pttl(key) <= 100 && redis.pttl(key) > 0);
    }

    @Test
    public void pttl() throws Exception {
        assertEquals(-2, (long) redis.pttl(key));
        redis.set(key, value);
        assertEquals(-1, (long) redis.pttl(key));
        redis.pexpire(key, 10);
        assertTrue(redis.pttl(key) <= 10 && redis.pttl(key) > 0);
    }

    @Test
    public void randomkey() throws Exception {
        assertNull(redis.randomkey());
        redis.set(key, value);
        assertEquals(key, redis.randomkey());
    }

    @Test
    public void rename() throws Exception {
        redis.set(key, value);
        assertEquals("OK", redis.rename(key, key + "X"));
        assertNull(redis.get(key));
        assertEquals(value, redis.get(key + "X"));
        redis.set(key, value + "X");
        assertEquals("OK", redis.rename(key + "X", key));
        assertEquals(value, redis.get(key));
    }

    @Test(expected = RedisException.class)
    public void renameNonexistentKey() throws Exception {
        redis.rename(key, key + "X");
    }

    @Test(expected = RedisException.class)
    public void renameIdenticalKeys() throws Exception {
        redis.set(key, value);
        redis.rename(key, key);
    }

    @Test
    public void renamenx() throws Exception {
        redis.set(key, value);
        assertTrue(redis.renamenx(key, key + "X"));
        assertEquals(value, redis.get(key + "X"));
        redis.set(key, value);
        assertFalse(redis.renamenx(key + "X", key));
    }

    @Test(expected = RedisException.class)
    public void renamenxNonexistentKey() throws Exception {
        redis.renamenx(key, key + "X");
    }

    @Test(expected = RedisException.class)
    public void renamenxIdenticalKeys() throws Exception {
        redis.set(key, value);
        redis.renamenx(key, key);
    }

    @Test
    public void restore() throws Exception {
        redis.set(key, value);
        byte[] bytes = redis.dump(key);
        redis.del(key);

        assertEquals("OK", redis.restore(key, 0, bytes));
        assertEquals(value, redis.get(key));
        assertEquals(-1, redis.pttl(key).longValue());

        redis.del(key);
        assertEquals("OK", redis.restore(key, 1000, bytes));
        assertEquals(value, redis.get(key));
        long ttl = redis.pttl(key);
        assertTrue(ttl <= 1000 && ttl >= 0);
    }

    @Test
    public void ttl() throws Exception {
        assertEquals(-2, (long) redis.ttl(key));
        redis.set(key, value);
        assertEquals(-1, (long) redis.ttl(key));
        redis.expire(key, 10);
        assertEquals(10, (long) redis.ttl(key));
    }

    @Test
    public void type() throws Exception {
        assertEquals("none", redis.type(key));

        redis.set(key, value);
        assertEquals("string", redis.type(key));

        redis.hset(key + "H", value, "1");
        assertEquals("hash", redis.type(key + "H"));

        redis.lpush(key + "L", "1");
        assertEquals("list", redis.type(key + "L"));

        redis.sadd(key + "S", "1");
        assertEquals("set", redis.type(key + "S"));

        redis.zadd(key + "Z", 1, "1");
        assertEquals("zset", redis.type(key + "Z"));
    }

    @Test
    public void scan() throws Exception {
        RedisAsyncConnection<String, String> async = client.connectAsync();
        async.set(key, value).get();
        RedisFuture<KeyScanCursor<String>> future = async.scan();

        KeyScanCursor<String> cursor = future.get();
        assertEquals("0", cursor.getCursor());
        assertTrue(cursor.isFinished());
        assertEquals(list(key), cursor.getKeys());

    }

    @Test
    public void scanStreaming() throws Exception {
        redis.set(key, value);
        ListStreamingAdapter<String> adapter = new ListStreamingAdapter<String>();

        StreamScanCursor cursor = redis.scan(adapter, ScanArgs.Builder.count(100).match("*"));

        assertEquals(1, cursor.getCount());
        assertEquals("0", cursor.getCursor());
        assertTrue(cursor.isFinished());
        assertEquals(list(key), adapter.getList());

    }

    @Test
    public void scanMultiple() throws Exception {

        Set<String> expect = new HashSet<String>();
        Set<String> check = new HashSet<String>();
        setup100KeyValues(expect);

        KeyScanCursor<String> cursor = redis.scan(ScanArgs.Builder.count(12));

        assertNotNull(cursor.getCursor());
        assertNotEquals("0", cursor.getCursor());
        assertFalse(cursor.isFinished());

        assertEquals(13, cursor.getKeys().size());
        check.addAll(cursor.getKeys());

        while (!cursor.isFinished()) {
            cursor = redis.scan(cursor);
            check.addAll(cursor.getKeys());
        }

        assertEquals(expect, check);
    }

    @Test
    public void scanMatch() throws Exception {

        Set<String> expect = new HashSet<String>();
        setup100KeyValues(expect);

        KeyScanCursor<String> cursor = redis.scan(ScanArgs.Builder.count(100).match("key1*"));

        assertEquals("0", cursor.getCursor());
        assertTrue(cursor.isFinished());

        assertEquals(11, cursor.getKeys().size());
    }

    protected void setup100KeyValues(Set<String> expect) {
        for (int i = 0; i < 100; i++) {
            redis.set(key + i, value + i);
            expect.add(key + i);
        }
    }
}
