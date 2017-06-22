(set-env!
 :source-paths #{"src/cljs"}
 :dependencies '[[org.clojure/clojure "1.9.0-alpha17"]
                 [org.clojure/clojurescript"1.9.562"]
                 [adzerk/boot-cljs "2.0.0"]
                 [adzerk/boot-reload "0.5.1"]
                 [adzerk/boot-cljs-repl "0.3.3"]
                 [com.cemerick/piggieback "0.2.1"]
                 [weasel "0.7.0"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [reagent "0.6.1" :exclusions [cljsjs/react cljsjs/react-dom cljsjs/react-dom-server]]
                 [re-frame "0.9.3"]]
 )

(require '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-reload :refer [reload]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]])



(deftask dev
  "Start development environqment"
  [i ip IP str "lan ip"]
  (let [ws-host (if (nil? ip)
                  (.getHostName (java.net.InetAddress/getLocalHost))
                  ip)]
    (comp
     (watch)
     (reload :ip "0.0.0.0" :ws-host ws-host)
     (cljs-repl :ip "0.0.0.0" :ws-host ws-host)
     (cljs)
     (target :dir #{"target"}))))
