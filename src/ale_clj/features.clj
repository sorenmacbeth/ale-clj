(ns ale-clj.features
  (:require [ale-clj.screen :refer [set-screen-ints]]
            [ale-clj.util :refer [deep-aget deep-aset]]))

(defn tiled-secam-features
  "Grabs the screen data from ALE and converts to simple tiled features. Assumes
  the 8 color SECAM palette."
  [{:keys [screen-ints screen-background tiles num-tiles tile-size] :as game}
   ^doubles screen-features]
  (set-screen-ints game)
  (let [num-tiles (int num-tiles)
        tile-size (int tile-size)]
    (dotimes [i num-tiles]
      (dotimes [j tile-size]
        (let [tile-color (aget ^ints screen-ints (deep-aget ints tiles i j))
              background-color (aget ^ints screen-background
                                     (deep-aget ints tiles i j))
              idx (bit-shift-right (bit-and tile-color 0xf) 1)]
          (when
              (and
               (not= tile-color background-color)
               (zero? (aget screen-features (+ (* i 8) idx))))
            (aset screen-features (+ (* i 8) idx) 1.0)))))))

(defn sparse-tiled-secam-features
  "Grabs the screen data from ALE and converts to simple tiled features. Assumes
  the 8 color SECAM palette. This version returns a sparse representation of the
  features in a Clojure map."
  [{:keys [screen-ints screen-background tiles num-tiles tile-size] :as game}]
  (set-screen-ints game)
  (let [num-tiles (int num-tiles)
        tile-size (int tile-size)]
    (loop [i (int 0)
           features (transient {})]
      (if (= i num-tiles)
        (persistent! features)
        (let [tile-features
              (loop [j (int 0)
                     tile-features (transient {})]
                (if (= j tile-size)
                  (persistent! tile-features)
                  (let [tile-color (aget ^ints screen-ints (deep-aget ints tiles i j))
                        background-color (aget ^ints screen-background
                                               (deep-aget ints tiles i j))
                        idx (bit-shift-right (bit-and tile-color 0xf) 1)]
                    (recur (inc j)
                           (if (or (= tile-color background-color)
                                   (not (nil? (get features (+ (* i 8) idx)))))
                             tile-features
                             (assoc! tile-features (+ (* i 8) idx) 1.0))))))]
          (recur (inc i)
                 (conj! features tile-features)))))))
