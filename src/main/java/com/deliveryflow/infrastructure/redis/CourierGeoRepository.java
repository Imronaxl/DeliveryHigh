package com.deliveryflow.infrastructure.redis;

import java.time.Instant;
import java.util.List;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class CourierGeoRepository {

  private static final String COURIERS_GEO_KEY = "couriers:geo";
  private final GeoOperations<String, String> geoOperations;
  private final RedisTemplate<String, String> redisTemplate;

  public CourierGeoRepository(
      GeoOperations<String, String> geoOperations, RedisTemplate<String, String> redisTemplate) {
    this.geoOperations = geoOperations;
    this.redisTemplate = redisTemplate;
  }

  public boolean updateCourierLocation(String courierId, Point point, Instant timestamp) {
    var script =
        new DefaultRedisScript<Long>(
            "local previous = redis.call('HGET', KEYS[2], ARGV[1]); "
                + "if previous and tonumber(previous) >= tonumber(ARGV[4]) then return 0 end; "
                + "redis.call('GEOADD', KEYS[1], ARGV[2], ARGV[3], ARGV[1]); "
                + "redis.call('HSET', KEYS[2], ARGV[1], ARGV[4]); return 1",
            Long.class);
    Long updated =
        redisTemplate.execute(
            script,
            List.of(COURIERS_GEO_KEY, "couriers:timestamps"),
            courierId,
            Double.toString(point.getX()),
            Double.toString(point.getY()),
            Long.toString(timestamp.toEpochMilli()));
    return Long.valueOf(1).equals(updated);
  }

  public GeoResults<RedisGeoCommands.GeoLocation<String>> findNearbyCouriers(
      double longitude, double latitude, Distance distance) {
    Point point = new Point(longitude, latitude);
    return geoOperations.radius(COURIERS_GEO_KEY, new Circle(point, distance));
  }

  public Long removeCourier(String courierId) {
    redisTemplate.opsForHash().delete("couriers:timestamps", courierId);
    return redisTemplate.opsForZSet().remove(COURIERS_GEO_KEY, courierId);
  }
}
