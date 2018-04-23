(ns ale-clj.examples.random-agent
  (:require [ale-clj.core :refer [start-game reset-game act game-over?]]
            [ale-clj.screen :refer [set-screen-ints]]
            [ale-clj.display :refer [create-frame display-screen dispose-frame]]
            [ale-clj.background :refer [subtract-background]]
            [ale-clj.tiles :refer [tiled-screen-ints]]))


(defn game-episode [{:keys [actions frame] :as game}]
  (reset-game game)
  (loop [rewards []]
    (if (game-over? game)
      rewards
      (let [action (rand-nth actions)
            reward (act game action)]
        ;; need to call this if I want to do anything like display the screen
        (set-screen-ints game)
        (subtract-background game)
        (tiled-screen-ints game)
        (display-screen game)
        (recur (conj rewards reward))))))

(defn random-example []
  (let [rom "Kaboom.bin"
        max-episodes 10
        game (start-game rom)
        {:keys [width height]} game
        frame (create-frame width height)
        rewards (doall (repeatedly
                        max-episodes
                        #(reduce + (game-episode
                                    (assoc game :frame frame)))))]
    (dispose-frame frame)
    rewards))
