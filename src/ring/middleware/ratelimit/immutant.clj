(ns ring.middleware.ratelimit.immutant
  (:require [ring.middleware.ratelimit.backend :as backend]
            [immutant.caching :as caching]))

(deftype ImmutantBackend [obj]
  backend/Backend
  (backend/get-limit [self limit k]
    (caching/swap-in! obj k (fn [prev]
                              (if prev
                                (inc prev)
                                1))))
  (backend/reset-limits! [self hour]
    (.clear obj)
    (caching/swap-in! :hour (constantly hour)))
  (backend/get-hour [self] (:hour obj))
  (backend/available? [self] true))

(defn immutant-backend
  ([] (immutant-backend (cache "ratelimit")))
  ([obj] (ImmutantBackend. obj)))
