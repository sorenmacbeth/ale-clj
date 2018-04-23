(ns ale-clj.background
  (:require [ale-clj.core :as ale]
            [ale-clj.screen :as screen]
            [ale-clj.util :refer [deep-aget deep-aset write-csv]]))

;; Functions for simple histogram based background subtraction to
;; reduce the number of irrelevant features. Probably not very effective
;; for games with scrolling or otherwise changing backgrounds.

(defn subtract-background
  "Currently doing background subtraction during feature creation, but this
  function is handy if you just want to produce a background subtracted image."
  [game]
  (let [{:keys [screen-ints screen-background]} game]
    (dotimes [i (alength ^ints screen-ints)]
      (let [s (aget ^ints screen-ints i)
            b (aget ^ints screen-background i)]
        (if (= s b)
          (aset ^ints screen-ints i -1)
          (aset ^ints screen-ints i s))))))

(defn get-random-screens
  "Play `rom` with random action for `observations` number of frames.
  Collect the screen ints, representing colors, for each frame in `screen-data`"
  [rom observations]
  (let [{:keys [width height actions] :as game} (ale/start-game rom)
        width (long width) height (long height)
        n (* width height)
        screen-data (make-array Integer/TYPE observations n)]
    (dotimes [i observations]
      (screen/set-screen-ints game)
      (System/arraycopy (:screen-ints game) 0 (aget ^"[[I" screen-data i) 0 n)
      (ale/act game (rand-nth actions))
      (when (ale/game-over? game)
        (ale/reset-game game)))
     screen-data))

(defn transpose-int-matrix [m]
  (let [rows (alength ^"[[I" m)
        cols (alength ^ints (aget ^"[[I" m  0))
        m-t (make-array Integer/TYPE cols rows)]
    (doseq [row (range rows) col (range cols)]
      (deep-aset ints m-t col row (deep-aget ints m row col)))
    m-t))

(defn screen-background
  "Use random screen data to determine background using simple histogram approach.
  I.e., we're assuming the most frequent color for a pixel is the background. This
  isn't always good if the background changes a lot, like the scrolling in Pitfall."
  [screen-data]
  (let [;; easier to do this if the rows represent the same pixel for each observation
        screen-data-t (transpose-int-matrix screen-data)
        num-pixels (alength ^"[[I" screen-data-t)
        background-screen (int-array num-pixels)]
    (dotimes [i num-pixels]
      (let [freq (frequencies (aget ^"[[I" screen-data-t i))
            color (first (apply max-key val freq))]
        (aset ^ints background-screen i ^int color)))
    background-screen))

(defn write-background [rom-name screen-ints]
  (write-csv [(into [] screen-ints)] (str "backgrounds/" rom-name ".csv") ))
