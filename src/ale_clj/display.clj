(ns ale-clj.display
  (:import [javax.swing JFrame JPanel]
           [java.awt Dimension]
           [java.awt.image BufferedImage]
           [java.awt Color]))

(defn get-screen-image
  "Returns a Bufferedimage object of the current game screen."
  [{:keys [screen-ints width height] :as game}]
  (let [image (BufferedImage. width height BufferedImage/TYPE_INT_RGB)
        height (int height) width (int width)]
    (dotimes [i height]
      (dotimes [j width]
        (let [k (+ (* i width ) j)]
          (.setRGB image j i (.getRGB (Color. (aget ^ints screen-ints k)))))))
    image))

(defn create-frame [width height]
  (doto (JFrame.)
    (.setSize (Dimension. width height))
    (.show)))

(defn display-screen [game]
  (let [frame ^JFrame (:frame game)
        image (get-screen-image game)
        panel (proxy [JPanel] []
                (paint [g]
                  (.drawImage ^java.awt.Graphics g image 0 0 this)
                  (.dispose ^java.awt.Graphics g)))
        graphics (.createGraphics ^BufferedImage image)]
    (.add frame panel)
    (.show frame)
    (.remove frame panel)))

(defn dispose-frame [frame]
  (.dispose ^JFrame frame))
