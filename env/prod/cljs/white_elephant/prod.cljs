(ns white-elephant.prod
  (:require [white-elephant.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
