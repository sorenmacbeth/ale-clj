(ns ale-clj.examples.online-sarsa-agent
  (:require [ale-clj.core :as ale]
            [ale-clj.features :as features]
            [ale-clj.util :refer [deep-aget deep-aset]]
            [ale-clj.display :refer [create-frame display-screen dispose-frame]]))

;; True Online Sarsa Lambda
;; - uses java arrays for features, weights, traces etc.
;; - based in part on https://github.com/mcmachado/TrueOnlineSarsa

;; van Seijen, H., Mahmood, A. R., Pilarski, P. M., Machado, M. C.,
;; Sutton, R. S. (2016). True online temporal- difference learning.
;; Journal of Machine Learning Research, 17(145):1–40.

(defn dot [^doubles xs ^doubles ys]
  (areduce xs i ret 0.0 (+ ret (* (aget xs i) (aget ys i)))))

(defn zero-vec! [^doubles v]
  (dotimes [i (alength v)]
    (aset v i 0.0)))

(defn select-action [^doubles q-values epsilon]
  (let [r (double (rand))
        eps (double epsilon)]
    (if (> r eps)
      (first (apply max-key second (shuffle (map-indexed vector q-values))))
      (rand-int (alength q-values)))))

(defn update-q-values [q-values weights features]
  (dotimes [i (alength ^doubles q-values)]
    (aset ^doubles q-values i
          ^double (dot ^doubles (aget ^"[[D" weights i) features))))

(defn update-trace
  [trace features action alpha lambda gamma trace-threshold]
  (let [;; it's crazy how much junk like this matters
        alpha (double alpha) lambda (double lambda) gamma (double gamma)
        z (aget ^"[[D" trace action)
        z-dot-x (double (dot z features))
        a (- 1.0 (* alpha gamma lambda z-dot-x))
        num-actions (alength ^"[[D" trace)
        num-features (alength ^doubles features)]
    (dotimes [i num-actions]
      (dotimes [j num-features]
        (let [t (+ (* gamma lambda (deep-aget doubles trace i j))
                   (if (= i action)
                     (* a (aget ^doubles features j))
                     0.0))]
          (deep-aset doubles trace i j
                     (if (> t trace-threshold) t 0.0)))))))

(defn update-weights
  [weights trace features action alpha delta delta-q]
  (let [delta (double delta) delta-q (double delta-q) alpha (double alpha)]
    (let [num-actions (alength ^"[[D" weights)
          num-features (alength ^doubles features)]
      (dotimes [i num-actions]
        (dotimes [j num-features]
          (if (= i action)
            (deep-aset doubles weights i j
                       (+ (deep-aget doubles weights i j)
                          (* alpha (+ delta delta-q) (deep-aget doubles trace i j))
                          (* -1.0 alpha delta-q (aget ^doubles features j))))
            (deep-aset doubles weights i j
                       (+ (deep-aget doubles weights i j)
                          (* alpha (+ delta delta-q)
                             (deep-aget doubles trace i j))))))))))

(defn game-episode
  [game weights features next-features qs next-qs params]
  (let [{:keys [epsilon alpha gamma lambda trace-threshold max-steps]} params
        num-actions (count (:actions game))
        num-features (alength ^doubles features)
        trace (make-array Double/TYPE num-actions num-features)]
    (ale/reset-game game)
    (features/tiled-secam-features game features)
    (loop [game-over? false
            rewards []
            q-old (double 0.0)
            action (select-action qs epsilon)
            step 0]
       (if (or game-over? (= step max-steps))
         rewards
         (let [_ (display-screen game)
               reward (double (ale/act game (nth (:actions game) action)))
               over? (ale/game-over? game)
               reward (double (if (pos? reward) 1.0 -0.01))
               _ (zero-vec! next-features)
               _ (update-q-values qs weights features)
               _ (features/tiled-secam-features game next-features)
               _ (update-q-values next-qs weights next-features)
               next-action (select-action next-qs epsilon)
               q (double (aget ^doubles qs action))
               next-q (if over?
                        0.0
                        (double (aget ^doubles next-qs next-action)))
               next-q (double next-q)
               gamma (double gamma)
               delta (+ reward (* gamma next-q) (* -1.0 q))
               delta-q (- q q-old)
               alpha (double (/ alpha num-features))]
           (update-trace
            trace features action alpha lambda gamma trace-threshold)
           (update-weights
            weights trace features action alpha delta delta-q)
           (System/arraycopy ^doubles next-features 0
                             ^doubles features 0
                             num-features)
           (recur over?
                  (conj rewards reward)
                  next-q
                  next-action
                  (inc step)))))))

(defn learn [game weights params]
  (let [{:keys [num-episodes num-colors num-tiles
                num-colors num-tiles num-actions]} params
        num-features (* num-colors num-tiles)
        qs (double-array num-actions)
        next-qs (double-array num-actions)
        features (double-array num-features)
        next-features (double-array num-features)]
    (loop [episode 0
           total-rewards []]
      (if (= episode num-episodes)
        total-rewards
        (let [_ (println "Episode:" episode)
              rewards (time
                       (game-episode
                        game weights features next-features qs next-qs params))]
          (recur (inc episode)
                 (conj total-rewards (reduce + rewards))))))))

(defn online-sarsa-example []
  (let [rom "Kaboom.bin"
        game (ale/start-game rom :display-screen? false)
        {:keys [actions tiles width height]} game
        frame (create-frame width height)
        num-tiles (count tiles)
        num-actions (count actions) num-colors 8
        params {:rom rom :epsilon 0.1 :lambda 0.9 :gamma 0.999 :alpha 0.01
                :num-episodes 10 :trace-threshold 0.01 :num-colors num-colors
                :num-tiles num-tiles :num-actions num-actions}
        num-features (* num-colors num-tiles)
        weights (make-array Double/TYPE num-actions num-features)
        total-rewards (learn (merge game {:frame frame}) weights params)]
    (dispose-frame frame)
    total-rewards))
