(defproject ring-ratelimit "0.2.3-SNAPSHOT"
  :description "Rate limit middleware for Ring"
  :url "https://github.com/myfreeweb/ring-ratelimit"
  :license {:name "WTFPL"
            :url "http://www.wtfpl.net/about/"}
  :dependencies [[org.clojure/clojure "1.9.0"]]
  :profiles {:dev {:dependencies [[midje "1.9.1"]
                                  [lein-release "1.1.3"]
                                  [ring/ring-core "1.6.3"]
                                  [ring-mock "0.1.5"]
                                  [http-kit "2.3.0"]
                                  [compojure "1.6.1"]
                                  [com.cemerick/friend "0.2.3"]
                                  [com.taoensso/carmine "2.18.1"]
                                  [commons-codec/commons-codec "1.11"]]}}
  :plugins [[lein-midje "3.2.1"]
            [lein-release "1.1.3"]]
  :aliases {"test" ["midje" "ring.middleware.ratelimit-test"]}
  :bootclasspath true
  :lein-release {:deploy-via :lein-deploy}
  :repositories [["snapshots" {:url "https://clojars.org/repo" :creds :gpg}]
                 ["releases"  {:url "https://clojars.org/repo" :creds :gpg}]])
