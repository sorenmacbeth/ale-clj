(ns ale-clj.screen
  (:require [ale-clj.core :as ale]))

(defn set-screen-ints
  "Grabs the current screen from ALE in the form of a byte array
  and converts the RGB bytes into an integer representation
  that lives in the `screen-ints` array, which is what we build
  the features from.
  Since there didn't appear to be a significant performance difference
  between byte arrays, ByteBuffers, and BytePointers, byte arrays are
  used for the screen data as they were easiest to deal with."
  [{:keys [ale width height screen-bytes screen-ints] :as game}]
  (ale/get-screen-rgb game)
  ;; avoiding boxing with junk like (long width) makes a big
  ;; difference when using unchecked math
  (let [width (long width) height (long height)]
    (dotimes [i height]
      (dotimes [j width]
        (let [k (+ (* 3 (* i width)) (* j 3))
              r (Byte/toUnsignedInt (aget ^bytes screen-bytes k))
              g (Byte/toUnsignedInt (aget ^bytes screen-bytes (+ k 1)))
              b (Byte/toUnsignedInt (aget ^bytes screen-bytes (+ k 2)))
              color (+ (bit-shift-left (+ (bit-shift-left r 8) g) 8) b)]
          (aset ^ints screen-ints (+ (* i width) j) color))))))
