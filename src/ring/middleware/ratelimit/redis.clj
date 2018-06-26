(ns ring.middleware.ratelimit.redis
  (:require [taoensso.carmine :as car]
            (ring.middleware.ratelimit [backend :as backend]
                                       [util :as util])))

(deftype RedisBackend [pool spec hashname hourname]
  backend/Backend
  (backend/get-limit [self limit k]
    (car/with-conn pool spec (car/hincrby hashname k 1)))
  (backend/reset-limits! [self hour]
    (car/with-conn pool spec
      (car/del hashname)
      (car/set hourname hour)))
  (backend/get-hour [self]
    (Integer/parseInt (car/with-conn pool spec (car/get hourname))))
  (backend/available? [self]
    (= "PONG" (try (car/with-conn pool spec (car/ping))
                   (catch Throwable _)))))

(defn redis-backend
  ([] (redis-backend (car/make-conn-spec)))
  ([spec] (redis-backend (car/make-conn-pool) spec))
  ([pool spec] (redis-backend pool spec "ratelimits"))
  ([pool spec hashname]
   (let [backend (RedisBackend. pool spec hashname (str hashname ":hour"))]
     (car/with-conn pool spec
       (car/set (.hourname backend) (util/current-hour)))
     backend)))
