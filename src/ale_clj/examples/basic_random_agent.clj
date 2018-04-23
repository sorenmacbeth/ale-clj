(ns ale-clj.examples.basic-random-agent
  (:import [org.bytedeco.javacpp IntPointer]
           [java.awt.image BufferedImage]
           [javax.swing JFrame JPanel]
           [java.awt Dimension]
           [java.awt Color]))

;; Basic example that's self contained doesn't try to hide the Java interop.
;; PNG files of the game screen are saved in the frames directory.
;; These can be converted in to a movie using ffmpeg and a script
;; is provided to do so assuming ffmpeg is installed.

;; There is also an example showing how to grab the screen data
;; from ALE and display it while the game is playing.

(defn game-episode
  [^org.bytedeco.javacpp.ale$ALEInterface ale actions png?]
  (.reset_game ale) ; start a new game for each episode
  (loop [rewards []
         step 0]
    (if (.game_over ale)
      rewards
      (let [action (rand-nth actions)
            reward (.act ale action)]
        (when png?
          (.saveScreenPNG ale (str "frames/frame-" (format "%06d" step) ".png")))
        (recur (conj rewards reward) (inc step))))))

(defn basic-example [rom]
  (let [ale (doto (org.bytedeco.javacpp.ale$ALEInterface.)
              (.loadROM (str "roms/" rom)))
        actions-ptr (.getMinimalActionSet ale)
        ;; The IntPointer is the only other class you really need to
        ;; deal with and just to get the list of actions that can
        ;; be taken
        actions (map #(.get ^IntPointer actions-ptr ^int %)
                     (range (.capacity ^IntPointer actions-ptr)))
        num-episodes 10
        rewards (reduce +
                        (for [i (range num-episodes)]
                          (game-episode ale actions (= (inc i) num-episodes))))]
    rewards))

;;; An example of grabing and displaying screen data

(set! *unchecked-math* true)

(defn set-screen-image
  [{:keys [ale width height screen-bytes screen-image] :as game}]
  (.getScreenRGB ale ^bytes screen-bytes)
  (let [;; it's surprising how much this matters
        width (long width) height (long height)]
    (dotimes [i height]
      (dotimes [j width]
        (let [k (+ (* 3 (* i width)) (* j 3))
              r (Byte/toUnsignedInt (aget ^bytes screen-bytes k))
              g (Byte/toUnsignedInt (aget ^bytes screen-bytes (+ k 1)))
              b (Byte/toUnsignedInt (aget ^bytes screen-bytes (+ k 2)))
              color (+ (bit-shift-left (+ (bit-shift-left r 8) g) 8) b)]
          (.setRGB ^BufferedImage screen-image j i (.getRGB (Color. color))))))))

(defn display-image [game]
  (let [frame ^JFrame (:frame game)
        image (:screen-image game)
        panel (proxy [JPanel] []
                (paint [g]
                  (.drawImage ^java.awt.Graphics g image 0 0 this)
                  (.dispose ^java.awt.Graphics g)))
        graphics (.createGraphics ^BufferedImage image)]
    (.add frame panel)
    (.show frame)
    (.remove frame panel)))

(defn game-episode-1 [game]
  (let [{:keys [width height ale screen-bytes screen-image actions]} game]
    (.reset_game ale)
    (loop [rewards []]
      (if (.game_over ale)
        rewards
        (let [action (rand-nth actions)
              reward (.act ale action)]
          (set-screen-image game)
          (display-image game)
          (recur (conj rewards reward)))))))

(defn basic-example-w-image [rom]
  (let [ale (doto (org.bytedeco.javacpp.ale$ALEInterface.)
              (.loadROM (str "roms/" rom)))
        ;; need this to get the screen dimensions
        screen (.getScreen ale)
        actions-ptr (.getMinimalActionSet ale)
        ;; We can get the screen data back as a Java byte array
        ;; a ByteBuffer, or a JavaCPP BytePointer. The choice
        ;; doesn't seem to affect performance much, so I use
        ;; byte arrays because it's easiest.
        width (.width screen) height (.height screen)
        game {:ale ale :screen-bytes (byte-array (* 3 width height))
              :screen-image (BufferedImage. width height BufferedImage/TYPE_INT_RGB)
              :width width :height height
              :actions (map #(.get ^IntPointer actions-ptr ^int %)
                            (range (.capacity ^IntPointer actions-ptr)))
              :frame (doto (JFrame.)
                       (.setSize (Dimension. width height))
                       (.show))}
        num-episodes 5
        rewards (doall (repeatedly
                        num-episodes
                        #(reduce + (game-episode-1 game))))]
    (.dispose (:frame game))
    rewards))
