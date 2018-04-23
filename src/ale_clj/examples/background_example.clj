(ns ale-clj.background.background-example
  (:require [ale-clj.background :refer
             [get-random-screens screen-background write-background]]))


(defn game-background [rom]
  (let [total-observations 18000
        screens (get-random-screens rom total-observations)
        background (screen-background screens)]
    (write-background rom background)))

#_(game-background "Kamboom.bin")
#_(game-background "seaquest.bin")
