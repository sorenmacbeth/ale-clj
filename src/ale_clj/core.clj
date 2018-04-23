(ns ale-clj.core
  (:require [ale-clj.util :refer [read-csv]]
            [ale-clj.tiles :refer [tile-indices]])
  (:import [org.bytedeco.javacpp IntPointer]))

;; I haven't been able to import ALEInterface and a number of
;; other classes in the usual way, so that's why all there's
;; junk like org.bytedeco.javacpp.ale$ALEInterface everywhere.

;; I'm not excited about including the tiling stuff here, but
;; keeping the tile information in the game map makes it easier
;; to avoid dealing directly with java arrays when developing agents.

;; Currently running all this with `format=SECAM` in ale.cfg.
;; I wanted to avoid converting the NTSC color palette into SECAM
;; as in the ALE Java example and unfortunately it isn't currently
;; possible to modify the color palette through the
;; ALEInterface object since theOSystem object wasn't included
;; in the JavaCPP presets interface. I'm still trying to work out
;; how to add it.


(defn start-game
  "Returns a map that contains the ALEInterface object through which
  learning agents will interact with Stella. It also holds the byte and
  int array will contain the screen information along with other game
  information that is frequently needed like screen dimensions, the
  set of actions that we can take and (for now) the necessary info
  for tiling the screen to reduce the number of features used by agents.
  We can also set a limited number of ALE configurations with this function."
  [rom & {:keys [seed repeat-prob frame-skip color-averaging?
                 tiles? tile-width tile-height]
          :or {seed 123 repeat-prob 0.25 frame-skip 5 color-averaging? true
               tiles? true tile-width 10 tile-height 10}}]
  (let [ale (doto (org.bytedeco.javacpp.ale$ALEInterface.)
              (.setInt "random_seed" ^int seed)
              (.loadROM (str "roms/" rom))
              (.setFloat "repeat_action_probability" ^double repeat-prob)
              (.setBool "color_averaging" ^boolean color-averaging?)
              (.setInt "frame_skip" ^int frame-skip))
        screen (.getScreen ale)
        actions-ptr (.getMinimalActionSet ale)
        actions (map #(.get ^IntPointer actions-ptr ^int %)
                     (range (.capacity ^IntPointer actions-ptr)))
        width (.width screen) height (.height screen)
        screen-bytes (byte-array (* 3 width height))
        screen-ints (int-array (* width height))
        background-data (read-csv (str "backgrounds/" rom ".csv"))
        screen-background (when-not (nil? background-data)
                            (int-array (first background-data)))
        tiles (when tiles? (tile-indices width height tile-width tile-height))]
    {:rom rom :ale ale :width width :height height :tiles tiles
     :tile-width tile-width :tile-height tile-height :num-tiles (alength tiles)
     :tile-size (* tile-height tile-width)
     :actions actions :screen-bytes screen-bytes :screen-ints screen-ints
     :screen-background screen-background}))

(defn act
  "Applies an action to the game and returns the reward. It is the user’s
  responsibility to check if the game has ended and to reset it when necessary
  (this method will keep pressing buttons on the game over screen)."
  [^org.bytedeco.javacpp.ale$ALEInterface game action]
  (.act ^org.bytedeco.javacpp.ale$ALEInterface (:ale game) action))

(defn game-over?
  "Indicates if the game has ended."
  [game]
  (.game_over ^org.bytedeco.javacpp.ale$ALEInterface (:ale game)))

(defn reset-game
  "Resets the game, but not the full system (it is not “equivalent” to
  un-plugging the console from electricity)."
  [game]
  (.reset_game ^org.bytedeco.javacpp.ale$ALEInterface (:ale game)))

(defn get-available-modes
  "Returns the vector of modes available for the current game. This should be
  called only after the ROM is loaded."
  [game]
  (let [modes (.getAvailableModes ^org.bytedeco.javacpp.ale$ALEInterface (:ale game))]
    (mapv #(.get modes ^int %) (range (.capacity modes)))))

(defn set-mode
  "Sets the mode of the game. The mode must be an available mode (otherwise it
  throws an exception). This should be called only after the ROM is loaded."
  [game m]
  (.setMode ^org.bytedeco.javacpp.ale$ALEInterface (:ale game) m))

(defn get-available-difficulties
  "Returns the vector of difficulties available for the current game. This
  should be called only after the ROM is loaded."
  [game]
  (let [difficulties (.getAvailableDifficulties
                      ^org.bytedeco.javacpp.ale$ALEInterface (:ale game))]
    (mapv #(.get difficulties ^int %) (range (.capacity difficulties)))))

(defn set-difficulty
  "Sets the difficulty of the game. The difficulty must be an available mode
  (otherwise it throws an exception). This should be called only after the ROM
  is loaded."
  [game d]
  (.setDifficulty ^org.bytedeco.javacpp.ale$ALEInterface (:ale game) d))

(defn get-legal-action-set
  "Returns the vector of legal actions (all the 18 actions). This should be
  called only after the ROM is loaded."
  [game]
  (let [legal-actions (.getLegalActionSet
                       ^org.bytedeco.javacpp.ale$ALEInterface (:ale game))]
    (mapv #(.get legal-actions ^int %) (range (.capacity legal-actions)))))

(defn get-minimal-action-set
  "Returns the vector of the minimal set of actions needed to play the game (all
  actions that have some effect on the game). This should be called only after
  the ROM is loaded."
  [game]
  (let [minimal-actions (.getMinimalActionSet
                         ^org.bytedeco.javacpp.ale$ALEInterface (:ale game))]
    (mapv #(.get minimal-actions ^int %) (range (.capacity minimal-actions)))))

(defn get-frame-number
  "Returns the current frame number since the loading of the ROM."
  [game]
  (.getFrameNumber ^org.bytedeco.javacpp.ale$ALEInterface (:ale game)))

(defn max-num-frames
  ""
  [game]
  (.max_num_frames ^org.bytedeco.javacpp.ale$ALEInterface (:ale game)))

(defn lives
  "Returns the agent’s remaining number of lives. If the game does not have the
  concept of lives (e.g. Freeway), this function returns 0."
  [game]
  (.lives ^org.bytedeco.javacpp.ale$ALEInterface (:ale game)))

(defn get-episode-frame-number
  "Returns the current frame number since the start of the cur- rent episode."
  [game]
  (.getEpisodeFrameNumber ^org.bytedeco.javacpp.ale$ALEInterface (:ale game)))

(defn get-screen
  "Returns a matrix (ALEScreen object) containing the current game screen. "
  [game]
  (.getScreen ^org.bytedeco.javacpp.ale$ALEInterface (:ale game)))

(defn get-screen-rgb
  "This method fills the given vector with a RGB version of the current screen,
  provided in row, column, then colour channel order (typically yielding
  210 × 160 × 3 = 100, 800 entries). The colour channels themselves are,
  in order: R, G, B. For example, output_rgb_buffer[(160 * 3) * 1 + 52 * 3 + 1]
  corresponds to the 2nd row, 53rd column pixel’s green value. The vector is
  resized as needed. Still, for efficiency it is recommended to initialize the
  vector beforehand, to make sure an allocation is not performed at each time step."
  [game]
  (.getScreenRGB ^org.bytedeco.javacpp.ale$ALEInterface (:ale game)
                 ^bytes (:screen-bytes game)))

(defn get-screen-grayscale
  "This method fills the given vector with a grayscale version of the current
  screen, provided in row- major order (typically yielding 210 × 160 = 33, 600
  entries). The vector is resized as needed. For efficiency it is recommended to
  initialize the vector beforehand, to make sure an allocation is not performed
  at each time step. Note that the grayscale value corresponds to the pixel’s
  luminance; for more details, consult the web."
  [game]
  (.getScreenGrayscale ^org.bytedeco.javacpp.ale$ALEInterface (:ale game)
                       ^bytes (:screen-bytes game)))

(defn get-ram
  "Returns current RAM content (byte-level) in an ALERAM object. "
  [game]
  (.getRAM ^org.bytedeco.javacpp.ale$ALEInterface (:ale game)))

(defn save-state
  "Saves the current state of the system if one wants to be able to recover a
  state in the future; e.g. in search algorithms."
  [game]
  (.saveState ^org.bytedeco.javacpp.ale$ALEInterface (:ale game)))

(defn load-state
  "Loads a previous saved state of the system once we have a state saved."
  [game]
  (.loadState ^org.bytedeco.javacpp.ale$ALEInterface (:ale game)))

(defn clone-state
  "Makes a copy of the environment state. This copy does not include
  pseudo-randomness, making it suitable for planning purposes. Returns
  an ALEState object."
  [game]
  (.cloneState ^org.bytedeco.javacpp.ale$ALEInterface (:ale game)))

(defn clone-system-state
  "This makes a copy of the system and environment state, suitable for
  serialization. This includes pseudo-randomness and so is not suitable for
  planning purposes. Returns an ALEState object."
  [game]
  (.cloneSystemState ^org.bytedeco.javacpp.ale$ALEInterface (:ale game)))

(defn restore-state
  "Reverse operation of cloneState(). This does not restore pseudo-randomness,
  so that repeated calls to restoreState() in the stochastic controls setting
  will not lead to the same outcomes. By contrast, see restoreSystemState."
  [game ^org.bytedeco.javacpp.ale$ALEState ale-state]
  (.restoreState ^org.bytedeco.javacpp.ale$ALEInterface (:ale game) ale-state))

(defn restore-system-state
  "Reverse operation of cloneSystemState."
  [game ^org.bytedeco.javacpp.ale$ALEState ale-state]
  (.restoreSystemState ^org.bytedeco.javacpp.ale$ALEInterface (:ale game) ale-state))

(defn save-screen-png [game ^String file-path]
  (.saveScreenPNG ^org.bytedeco.javacpp.ale$ALEInterface (:ale game) file-path))

(defn create-screen-exporter [game ^String path]
  (.createScreenExporter ^org.bytedeco.javacpp.ale$ALEInterface (:ale game) path))
