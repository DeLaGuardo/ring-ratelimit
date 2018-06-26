(ns ring.middleware.ratelimit-test
  (:import [java.util Date])
  (:require [clojure.string :as string]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds]
                             [openid :as openid])
            [ring.middleware.ratelimit :as ratelimit]
            (ring.middleware.ratelimit [util :as util]
                                       [backend :as backend]
                                       [local-atom :as local-atom]
                                       [redis :as redis]
                                       [limits :as limits])
            [ring.mock.request :as request]
            [midje.sweet :as midje]))

(defn rsp-limit [rsp] (get-in rsp [:headers "X-RateLimit-Limit"]))
(defn remaining [rsp] (get-in rsp [:headers "X-RateLimit-Remaining"]))

(doseq [backend [(local-atom/local-atom-backend) (redis/redis-backend)]]
  (let [app (-> (fn [req] {:status 418
                           :headers {"Content-Type" "air/plane"}
                           :body "Hello"})
                (ratelimit/wrap-ratelimit {:limits [(ratelimit/ip-limit 5)]
                                           :backend backend}))]

    (midje/facts (str "ratelimit <" (last (string/split (str (type backend)) #"\.")) ">")

                 (backend/reset-limits! backend (util/current-hour))

                 (midje/fact "shows the rate limit"
                             (let [rsp (-> (request/request :get "/") app)]
                               (:status rsp) => 418
                               (rsp-limit rsp) => "5"
                               (remaining rsp) => "4"))

                 (midje/fact "returns 429 when there are no requests left"
                             (dotimes [_ 5] (-> (request/request :get "/") app))
                             (let [rsp (-> (request/request :get "/") app)]
                               (:status rsp) => 429
                               (remaining rsp) => "0"))

                 (midje/fact "resets the limit every hour"
                             (with-redefs [util/current-hour (fn [] (- (.getHours (Date.)) 1))]
                               (dotimes [_ 5] (-> (request/request :get "/") app))
                               (-> (request/request :get "/") app :status) => 429)
                             (-> (request/request :get "/") app :status) => 418))))

(let [api-users {"api-key" {:username "api-key"
                            :password (creds/hash-bcrypt "api-pass")
                            :roles #{:api}}
                 "admin" {:username "admin"
                          :password (creds/hash-bcrypt "admin-pass")
                          :roles #{:admin}}}
      app (-> (fn [req] {:status 200
                         :headers {"Content-Type" "text/plain"}
                         :body (with-out-str (pr req))})
              (ratelimit/wrap-ratelimit {:limits [(ratelimit/role-limit :admin 20)
                                                  (-> 10
                                                      limits/limit
                                                      limits/wrap-limit-user
                                                      limits/wrap-limit-ip)
                                                  (ratelimit/ip-limit 5)]
                                         :backend (local-atom/local-atom-backend)})
              (friend/authenticate {:allow-anon? true
                                    :workflows [(workflows/http-basic
                                                 :credential-fn (partial creds/bcrypt-credential-fn api-users)
                                                 :realm "test-realm")]}))]
  (midje/facts "ratelimit with 3 limiters"
               (midje/fact "uses the admin limit for admins"
                           (let [rsp (-> (request/request :get "/")
                                         (request/header "Authorization" "Basic YWRtaW46YWRtaW4tcGFzcw==")
                                         app)]
                             (:status rsp) => 200
                             (remaining rsp) => "19"))
               (midje/fact "uses the user limit for authenticated requests"
                           (let [rsp (-> (request/request :get "/")
                                         (request/header "Authorization" "Basic YXBpLWtleTphcGktcGFzcw==")
                                         app)]
                             (:status rsp) => 200
                             (remaining rsp) => "9"))
               (midje/fact "uses the composed limit for user-ip"
                           (let [rsp (-> (request/request :get "/")
                                         (request/header "Authorization" "Basic YXBpLWtleTphcGktcGFzcw==")
                                         (assoc :remote-addr "host-one")
                                         app)]
                             (remaining rsp) => "9")
                           (let [rsp (-> (request/request :get "/")
                                         (request/header "Authorization" "Basic YXBpLWtleTphcGktcGFzcw==")
                                         (assoc :remote-addr "host-two")
                                         app)]
                             (remaining rsp) => "9"))
               (midje/fact "uses the ip limit for unauthenticated requests"
                           (let [rsp (-> (request/request :get "/") app)]
                             (:status rsp) => 200
                             (remaining rsp) => "3"))))
