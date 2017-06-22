(set-env!
 :source-paths #{"src/cljs"}
 :dependencies '[[org.clojure/clojure "1.9.0-alpha17"]
                 [org.clojure/clojurescript"1.9.562"]
                 [adzerk/boot-cljs "2.0.0"]
                 [adzerk/boot-reload "0.5.1"]
                 [adzerk/boot-cljs-repl "0.3.3"]
                 [com.cemerick/piggieback "0.2.1"]
                 [weasel "0.7.0"]
                 [org.clojure/tools.nrepl "0.2.12"]])

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-reload :refer [reload]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]])


(defn get-lan-ip
  []
  (cond
    (some #{(System/getProperty "os.name")} ["Mac OS X" "Windows 10"])
    (.getHostAddress (java.net.InetAddress/getLocalHost))

    :else
    (->> (java.net.NetworkInterface/getNetworkInterfaces)
         (enumeration-seq)
         (filter #(not (or (clojure.string/starts-with? (.getName %) "docker")
                           (clojure.string/starts-with? (.getName %) "br-"))))
         (map #(.getInterfaceAddresses %))
         (map
          (fn [ip]
            (seq (filter #(instance?
                           java.net.Inet4Address
                           (.getAddress %))
                         ip))))
         (remove nil?)
         (first)
         (filter #(instance?
                   java.net.Inet4Address
                   (.getAddress %)))
         (first)
         (.getAddress)
         (.getHostAddress))))


(deftask dev
  "Start development environment"
  []
  (let [lan-ip (get-lan-ip)] 
    (comp
     (watch)
     (reload :ip "0.0.0.0" :ws-host lan-ip)
     (cljs-repl :ip "0.0.0.0" :ws-host lan-ip)
     (cljs)
     (target :dir #{"target"}))))
