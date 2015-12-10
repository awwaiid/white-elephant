(ns white-elephant.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [ajax.core :refer [GET POST]]
              [accountant.core :as accountant]))

(defn dlog [thing]
  (.log js/console thing)
  thing)


; ;; -------------------------
; ;; Views

(defn about-page []
  [:div [:h2 "About White Elephant Gift Selector"]
   [:div [:a {:href "/"} "go to the home page"]]
   [:div [:p
          "This is a tool to pick out the BEST White Elephant Gift!"]]
   ])

(defonce app-state (atom {
                          :text "Hello to all the really strange wonderful weird new world!"
                          :min-price 0
                          :max-price 2000
                          :possible-products []
                          :product {
                                    :img ""
                                    :title ""
                                    :url ""
                                    :price ""
                                    }
                          :next-product {
                                         :img ""
                                         :title ""
                                         :url ""
                                         :price ""
                                         }
                          }))

(defn next-product-handler [response]
  ; (.log js/console (str response))
  (swap! app-state assoc :next-product response)
  ; Pre-fetch the image, specifically
  (aset (js/Image.) "src" (get response :img)))

(defn next-product []
  (swap! app-state assoc :product (get @app-state :next-product))
  (GET "/random-product" {
                          :response-format :json
                          :keywords? true
                          :handler next-product-handler}))

(defn first-product-handler [response]
  ; (.log js/console (str response))
  (swap! app-state assoc :next-product response)
  ; Pre-fetch the image, specifically
  (aset (js/Image.) "src" (get response :img))
  (next-product))

(defn first-product []
  (GET "/random-product" {
                          :response-format :json
                          :keywords? true
                          :handler first-product-handler}))

(defn save-product []
  (swap! app-state update-in [:possible-products] conj (get @app-state :product))
  (swap! app-state update-in [:possible-products] shuffle)
  (next-product))

(defn product [product-info]
  (dlog (str "Re-render product:" (get product-info :title)))
   [:div.product.clearfix
     [:img.photo { :src (get product-info :img) }]
     [:div.price "Price " (get product-info :price)]
     [:a.buy-link { :href (get product-info :url) :target "_blank" } "Product Details / Buy"]
     [:h3.title (get product-info :title)]])

(defn possible-products-count []
  (let [c (count (@app-state :possible-products))]
    [:div.selected-count
     [:strong.count (str c)]
     (str " products selected (16+ needed)")]))

(defn triage-products []
  [:div

   [:h2 "White Elephant Gift Selector"]
   [:h3 "Phase 1: Triage"]

   [possible-products-count]

   [:div.current
   [:div
    [:a {:href "/about"} "go to about page"]
    " - "
    ]
    [:h3 "Worthy for consideration?"]
     [:div.actions.clearfix
      [:div.another { :onClick next-product } "Next!"]
      [:div.thisone { :onClick save-product } "Keep it!"]]
   ; [:hr]
    [product (get @app-state :product)]]
   (if (>= (count (@app-state :possible-products)) 16)
      [:a.onward {:href "/tournament"} (str (count (@app-state :possible-products)) " is enough... Tournament time!")])

  [:div.contenders
  ; For debugging
  (map product (@app-state :possible-products))
   ]
  ]
  )

(defn tournament-keep [product]
    (dlog (str "Keeping product " product))
    (dlog (str "Currently there are" (count (@app-state :possible-products))))
    (dlog (str (map :title (get @app-state :possible-products))))
    (swap! app-state update-in [:possible-products] rest)
    (dlog (str "Currently there are" (count (@app-state :possible-products))))
    (dlog (str (map :title (get @app-state :possible-products))))
    (swap! app-state update-in [:possible-products] rest)
    (dlog (str "Currently there are" (count (@app-state :possible-products))))
    (dlog (str (map :title (get @app-state :possible-products))))
    (swap! app-state update-in [:possible-products] concat [product])
    (dlog (str "Currently there are" (count (@app-state :possible-products))))
    (dlog (str (map :title (get @app-state :possible-products))))
    (dlog "Done keeping"))

(defn tournament []
  (dlog "Re-render tournament")
  ; (let [product-1 (first (@app-state :possible-products))
  ;       product-2 (second (@app-state :possible-products))]
  (let [products (@app-state :possible-products)
        product-count (count products)
        product-1 (first products)]
    (dlog (str "Currently there are " (count (@app-state :possible-products))))
    (dlog (str "product-1: " (product-1 :title)))
    ; (dlog (str "product-2: " (product-2 :title)))
    (if (= product-count 1)
      [:div
        [:h2 "White Elephant Gift Selector"]
        [:h3 "FINAL WINNER"]
          [:a {:href "/about"} "go to about page"]
          " - "
          [:a {:href "/"} "back to phase 1"]
        [product product-1]]
      (let [product-2 (second products)]
        [:div
         [:h2 "White Elephant Gift Selector"]
         [:h3 "Phase 2: Tournament"]
          [:a {:href "/about"} "go to about page"]
          " - "
          [:a {:href "/"} "back to phase 1"]

         [:h4.remaining-count
          [:span "There are "]
          [:span.count (- (count products) 2)]
          [:span " match-ups after this!"]]

         [:div.clearfix
           [:div.compare
             [:div.clearfix [:div.another { :onClick #(tournament-keep product-1) } "This one"]]
             [product product-1]]

           [:div.compare
             [:div.clearfix [:div.thisone { :onClick #(tournament-keep product-2) } "This one"]]
             [product product-2]]]

         ]))))


; (defn on-js-reload []
;   ;; optionally touch your app-state to force rerendering depending on
;   ;; your application
;   ;; (swap! app-state update-in [:__figwheel_counter] inc)
; )


(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'triage-products))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

(secretary/defroute "/tournament" []
  (session/put! :current-page #'tournament))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!)
  (accountant/dispatch-current!)
  (first-product)
  (mount-root))
