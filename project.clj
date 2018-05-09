(defproject ale-clj "0.1.0-SNAPSHOT"
  :description "Using The Arcade Learning Environment in Clojure."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.bytedeco.javacpp-presets/ale-platform "0.6.0-1.4.1"]
                 [org.clojure/data.csv "0.1.4"]]
  :source-paths ["src"]
  :profiles {:uberjar {:global-vars {*unchecked-math* true}}
             :dev {:global-vars {*warn-on-reflection* true
                                 *unchecked-math* :warn-on-boxed
                                 *print-length* 128}}}
  :jvm-opts ["-server" "-Xmx6g"])
