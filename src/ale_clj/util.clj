(ns ale-clj.util
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]))


;; Naively using aset and aget for multi-dimensional java arrays
;; is super slow even with type hints.
;; These macros were taken from http://clj-me.cgrand.net/2009/10/15/multidim-arrays/

(defmacro deep-aget
  ([hint array idx]
   `(aget ~(vary-meta array assoc :tag hint) ~idx))
  ([hint array idx & idxs]
   `(let [a# (aget ~(vary-meta array assoc :tag 'objects) ~idx)]
      (deep-aget ~hint a# ~@idxs))))

(defmacro deep-aset [hint array & idxsv]
  (let [hints '{doubles double ints int} ; not a comprehensive map
        [v idx & sxdi] (reverse idxsv)
        idxs (reverse sxdi)
        v (if-let [h (hints hint)] (list h v) v)
        nested-array (if (seq idxs)
                       `(deep-aget ~'objects ~array ~@idxs)
                       array)
        a-sym (with-meta (gensym "a") {:tag hint})]
    `(let [~a-sym ~nested-array]
       (aset ~a-sym ~idx ~v))))


(defn write-csv [xs file-name]
  (with-open [writer (io/writer file-name)]
    (csv/write-csv writer xs)))

(defn read-csv [file-name]
  (when (.exists (io/as-file file-name))
    (with-open [reader (io/reader file-name)]
      (doall
       (for [line (csv/read-csv reader)]
         (map read-string line))))))
