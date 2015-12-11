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

; (def Swipe (reagent/adapt-react-class js/ReactSwipe))
; (def Swipe (reagent/adapt-react-class (aget js/ReactSwipe ReactSwipe)))
; (def Button (reagent/adapt-react-class (aget js/ReactBootstrap "Button")))


(defn about-page []
  [:div [:h1 "About White Elephant Gift Selector"]
   [:div [:a {:href "/"} "go to the home page"]]
   [:div [:p
          "This is a tool to pick out the BEST White Elephant Gift!"]]
    ; [Swipe
    ;  [:div "pane1"]
    ;  [:div "pane2"]
    ;  [:div "pane3"]]
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
                          :error-handler next-product
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
                          :error-handler first-product
                          :handler first-product-handler}))

(defn seq-contains? [coll target] (some #(= target %) coll))

(defn save-product []
  (if (not (seq-contains? (map :title (@app-state :possible-products)) (get-in @app-state [:product :title])))
    (do (
      (swap! app-state update-in [:possible-products] conj (get @app-state :product))
      (swap! app-state update-in [:possible-products] shuffle))))
  (next-product))

(defn product [product-info]
  (dlog (str "Re-render product:" (get product-info :title)))
   [:div.product.clearfix
     [:img.photo { :src (get product-info :img) }]
     [:div.desc
       [:h3.title (get product-info :title)]
       [:div.price "Price " (get product-info :price)]
       [:a.buy-link { :href (get product-info :url) :target "_blank" } "Buy it on BLINQ"]
     ]
     ])

(defn possible-products-count []
  (let [c (count (@app-state :possible-products))]
    [:div.selected-count
     [:strong.count (str c)]
     (str " products selected")
     [:div.instruction
       (str "16 or more needed for the tournament to begin")
      ]]))

(defn triage-products []
  ; (if (< (count (@app-state :possible-products)) 1)
  ;   (secretary/dispatch! "/"))
  [:div

   [:h1 "White Elephant Gift Selector"]
   [:h2 "First: Build Your Product List"]
   ; [:div [:a {:href "/about"} "go to about page"] ]
   ; [:a {:href "/about"} "go to about page"]

   [possible-products-count]
   (if (>= (count (@app-state :possible-products)) 16)
      [:div [:a.onward {:href "/tournament"} (str (count (@app-state :possible-products)) " is enough... Tournament time!")]
   [:br]])

   [:div.current
      [:h3 "Worth Adding To Your List?"]
      [product (get @app-state :product)]
      [:div.actions.clearfix
        [:a.another { :onClick next-product } "🚫 Nope"]
        [:a.thisone { :onClick save-product } "👍Keep it! "]]]
    ;

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
        [:h1 "White Elephant Gift Selector"]
        [:h2 "WE HAVE A WINNER! YOU FOUND YOUR GIFT!"]
          [:a {:href "/about"} "go to about page"]
          " - "
          [:a {:href "/"} "back to phase 1"]
        [product product-1]]
      (let [product-2 (second products)]
        [:div
         [:h1 "White Elephant Gift Selector"]
         [:h2 "Next: Tournament of the Gifts"]
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
