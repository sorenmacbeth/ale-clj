# ale-clj
Some simple examples using Clojure to create agents for
[The Arcade Learning Environment](https://github.com/mgbellemare/Arcade-Learning-Environment) using
the [JavaCPP Presets ALE module. ](https://github.com/bytedeco/javacpp-presets/tree/master/ale).


## Random Agent

Here's an example of starting a game and running it through several episodes where
actions are randomly selected.

``` clojure
(require '[ale-clj.core :refer [start-game reset-game act game-over?]])

(defn game-episode [{:keys [actions] :as game}]
  (reset-game game)
  (loop [rewards []]
    (if (game-over? game)
      rewards
      (let [action (rand-nth actions)
            reward (act game action)]
        (recur (conj rewards reward))))))

(let [game (start-game "Kaboom.bin")
      rewards (doall (repeatedly
                        10
                        #(reduce + (game-episode game))))]
    rewards)
```

We can make this a little more interesting by displaying the game screens while it's running.
Forgive the clumsy approach to animating this.

``` clojure
(require '[ale-clj.screen :refer [set-screen-ints]]
         '[ale-clj.display :refer [create-frame display-screen dispose-frame]])

(defn game-episode [{:keys [actions frame] :as game}]
  (reset-game game)
  (loop [rewards []]
    (if (game-over? game)
      rewards
      (let [action (rand-nth actions)
            reward (act game action)]
        ;; need to executre this if I want to do anything like display the screen since
        ;; otherwise the only thing ALE is sending back to the agent is the reward.
        (set-screen-ints game)
        (display-screen game)
        (recur (conj rewards reward))))))

(let [game (start-game "Kaboom.bin")
      {:keys [width height]} game
      frame (create-frame width height)
      rewards (doall (repeatedly
                      10
                      #(reduce + (game-episode
                                  (assoc game :frame frame)))))]
    (dispose-frame frame)
    rewards)

```
In order to reduce the number of features, it's handy to subtract the (hopefully) static background
from the game screens. It doesn't matter for the random agent, but I thought it would be cute to show 
what the that looks like.
This can be done by adding the following require:
``` clojure
(require '[ale-clj.background :refer [subtract-background]])
```
and by insertering `(subtract-background game)` after `(set-screen-ints)` in the `game-episode` function.
This will only work if we actually determine what the background looks like, so if there
isn't a file for the game in the `backgrounds` directory, you'll have to create one.
Check the `examples` directory to see how that's done.

Another way to reduce the feature set is to tile the screen and we can see what that looks like
by requiring this:

``` clojure
(require '[ale-clj.tiles :refer [tiled-screen-ints]])
```
and inserting `(tiled-screen-ints game)` after `(subtract-background game)`.

More comprehesive examples with actual learning agents are in `src/ale-clj/examples`.

## License

Copyright © 2018 Yieldbot, Inc.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
