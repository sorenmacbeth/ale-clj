(ns ale-clj.display
  (:import [javax.swing JFrame JPanel]
           [java.awt Dimension]
           [java.awt.image BufferedImage AffineTransformOp]
           [java.awt.geom AffineTransform]
           [java.awt Color]))

(defn get-screen-image
  "Returns a Bufferedimage object of the current game screen."
  [{:keys [screen-ints width height] :as game}]
  (let [image (BufferedImage. width height BufferedImage/TYPE_INT_RGB)
        at (AffineTransform.)
        _ (.scale at 2.0 2.0)
        scale-op (AffineTransformOp. at AffineTransformOp/TYPE_BILINEAR)
        height (int height) width (int width)
        scaled-image (BufferedImage.
                      (* 2 width) (* 2 height) BufferedImage/TYPE_INT_RGB)]
    (dotimes [i height]
      (dotimes [j width]
        (let [k (+ (* i width ) j)]
          (.setRGB image j i (.getRGB (Color. (aget ^ints screen-ints k)))))))
    (.filter scale-op image scaled-image)))

(defn create-frame [width height]
  (doto (JFrame.)
    (.setSize (Dimension. (* 2.0 width) (* 2.0 height)))
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
