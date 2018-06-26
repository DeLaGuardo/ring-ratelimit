(ns ring.middleware.ratelimit.local-atom
  (:require (ring.middleware.ratelimit [util :as util]
                                       [backend :as backend])))

(defn- update-state [state limit k]
  (assoc state k
         (if-let [v (state k)] (inc v) 1)))

(deftype LocalAtomBackend [rate-map hour-atom]
  backend/Backend
  (bacckend/get-limit [self limit k]
    ((swap! rate-map update-state limit k) k))
  (backend/reset-limits! [self hour]
    (reset! rate-map {})
    (reset! hour-atom hour))
  (backend/get-hour [self] @hour-atom)
  (backend/available? [self] true))

(def ^:private default-rate-map (atom {}))
(def ^:private default-hour (atom (util/current-hour)))

(defn local-atom-backend
  ([] (local-atom-backend default-rate-map))
  ([rate-map] (local-atom-backend rate-map default-hour))
  ([rate-map hour-atom] (LocalAtomBackend. rate-map hour-atom)))
