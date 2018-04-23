(ns ale-clj.tiles
  (:require [ale-clj.util :refer [deep-aget]]))

;; Not that it really matters, but I believe the tiling produced by this
;; function ends up getting ordered like:
;; 0 3 6             0 1 2
;; 1 4 7  instead of 3 4 5
;; 2 5 8             6 7 8
;; which would feel more natural to me.

(defn tile-indices
  "Partition the screen into equal sized tiles. Returns an array of int-arrays
  where each int-array represents a tile and the entries in those array indicate
  the indices of the pixels in the screen corresponding to that tile."
  [width height tile-width tile-height]
  {:pre [(and (zero? (mod width tile-width))
              (zero? (mod height tile-height)))]}
  (let [pixels (range (* width height))
        tiles (loop [xs (partition width pixels)
                     tiles []]
                (if (empty? (first xs))
                  (partition (* tile-height tile-width) (flatten tiles))
                  (recur (map #(drop tile-width %) xs)
                         (conj tiles
                               (map #(take tile-width %) xs)))))]
    (into-array (map int-array tiles))))

(defn tiled-screen-ints
  "Tiling usually happens when features are created. This function is for
  helping to produce an image of the tiled screen."
  [{:keys [screen-ints tiles num-tiles tile-size] :as game}]
  (let [num-tiles (int num-tiles)
        tile-size (int tile-size)]
    (dotimes [i num-tiles]
      (let [tile-colors
            (loop [j 0
                   colors (transient #{})]
              (if (= j tile-size)
                (persistent! colors)
                (let [tile-color (aget ^ints screen-ints (deep-aget ints tiles i j))]
                  (recur (inc j) (conj! colors tile-color)))))]
        (loop [j (long 0)
               [color & more] (cycle tile-colors)]
          (when-not (= j tile-size)
            (let [screen-idx (long (deep-aget ints tiles i j))
                  color (long color)]
              (aset ^ints screen-ints screen-idx color)
              (recur (inc j) more))))))))
