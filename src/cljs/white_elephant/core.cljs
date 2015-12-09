; (ns white-elephant.core
;     (:require [reagent.core :as reagent :refer [atom]]
;               [reagent.session :as session]
;               [secretary.core :as secretary :include-macros true]
;               [ajax.core :refer [GET POST]]
;               [accountant.core :as accountant]))

; ;; -------------------------
; ;; Views


; (defn home-page []
;   [:div
;    [:h2 "Welcome to white-elephant!"]
;    [:div (GET "/random-product")]
;    [:div [:a {:href "/about"} "go to about page"]]])

; (defn about-page []
;   [:div [:h2 "About white-elephant"]
;    [:div [:a {:href "/"} "go to the home page"]]])

; (defn current-page []
;   [:div [(session/get :current-page)]])

; ;; -------------------------
; ;; Routes

; (secretary/defroute "/" []
;   (session/put! :current-page #'home-page))

; (secretary/defroute "/about" []
;   (session/put! :current-page #'about-page))

; ;; -------------------------
; ;; Initialize app

; (defn mount-root []
;   (reagent/render [current-page] (.getElementById js/document "app")))

; (defn init! []
;   (accountant/configure-navigation!)
;   (accountant/dispatch-current!)
;   (mount-root))

(ns white-elephant.core
  (:require
    [ajax.core :refer [GET POST]]
    [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(println "Edits to this text should show up in my developer console.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {
                      :text "Hello to all the really strange wonderful weird new world!"
                      :min-price 0
                      :max-price 2000
                      :possible-products []
                      :product {
                                :img "https://www.blinq.com/search/thumb.php?s=172&aspect=true&f=https%3a%2f%2fin…sl.fastly.net%2f1588078%2ftwo_fifty%2f9750412txAJlbgL.jpg.jpg%3f1447436187"
                                :title "Calabria 91348 Bi-Focal Safety Glasses UV Protection in Smoke ; +1.00"
                                :url "https://blinq.resultspage.com/search?p=R&ts=infinitescroll&uid=554603337&w=…afety-glasses-uv-protection-in-smoke-1-00%2f590946%3fcondition%3dbrand-new"
                      :price "$17.01"
                                }
                      :next-product {
                                :img "https://www.blinq.com/search/thumb.php?s=172&aspect=true&f=https%3a%2f%2fin…sl.fastly.net%2f1588078%2ftwo_fifty%2f9750412txAJlbgL.jpg.jpg%3f1447436187"
                                :title "Calabria 91348 Bi-Focal Safety Glasses UV Protection in Smoke ; +1.00"
                                :url "https://blinq.resultspage.com/search?p=R&ts=infinitescroll&uid=554603337&w=…afety-glasses-uv-protection-in-smoke-1-00%2f590946%3fcondition%3dbrand-new"
                      :price "$17.01"
                                }
                      ; :best-product {
                      ;           :img "https://www.blinq.com/search/thumb.php?s=172&aspect=true&f=https%3a%2f%2fin…sl.fastly.net%2f1588078%2ftwo_fifty%2f9750412txAJlbgL.jpg.jpg%3f1447436187"
                      ;           :title "Calabria 91348 Bi-Focal Safety Glasses UV Protection in Smoke ; +1.00"
                      ;           :url "https://blinq.resultspage.com/search?p=R&ts=infinitescroll&uid=554603337&w=…afety-glasses-uv-protection-in-smoke-1-00%2f590946%3fcondition%3dbrand-new"
                      ; :price "$17.01"
                      ;           }
                      }))

(defn handler [response]
  (.log js/console (str response))
  (swap! app-state assoc :next-product response)
  (.log js/console (str "App state:" @app-state))
  )

(defn next-product []
  (swap! app-state assoc :product (get @app-state :next-product))
  (GET "/random-product" {
                          :response-format :json
                          :keywords? true
                          :handler handler}))

(defn save-product []
  (swap! app-state update-in [:possible-products] conj (get @app-state :product))
  (next-product))

(defn product [product-info]
   [:div.product
     [:img { :src (get product-info :img) }]
     [:div.price "Price " (get product-info :price)]
     [:a.link { :href (get product-info :url) } "Product Details / Buy"]
     [:h3.title (get product-info :title)]])

(defn possible-products-count []
  (let [c (count (@app-state :possible-products))]
    [:div.selected-count
     (str c " products selected")]))

(defn hello-world []
  [:div

   [:h2 "White Elephant Gift Selector"]

   (possible-products-count)

   [:div.current
    [:h3 "Worthy for consideration?"]
    (product (get @app-state :product))]

   [:div.actions
    [:div { :class "another" :onClick next-product } "Next!"]
    [:div { :class "thisone" :onClick save-product } "Keep it!"]]
   [:hr]
  [:div.contenders
  ; For debugging
  (map product (@app-state :possible-products))
   ]
  ]
  )

(defn render []
  (reagent/render-component [hello-world]
                            (. js/document (getElementById "app"))))

(defn init! []
  (next-product)
  (next-product)
  (render))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
