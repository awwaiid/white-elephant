(ns white-elephant.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]

            [net.cgrand.enlive-html :as enlive]

            ; [ring.util.response :refer [resource-response response]]
            ; [ring.middleware.json :as middleware]
            [ring.util.response :refer [response]]
            [ring.middleware.json :refer [wrap-json-response]]

            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-js include-css]]
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware.reload :refer [wrap-reload]]
            [clj-http.client :as http]
            [environ.core :refer [env]]))

(def mount-target
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]])

(def loading-page
  (html
   [:html
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1"}]
     (include-css "https://fonts.googleapis.com/css?family=Lato:400,100,300,700,900")
     (include-css (if (env :dev) "css/site.css" "css/site.min.css"))]
    [:body
     mount-target
     (include-js "js/app.js")
     (include-js "js/react-swipe.js")
     ]]))

(def cards-page
  (html
   [:html
    [:head
     [:meta {:charset "utf-8"}]]
    [:body
     mount-target
     (include-js "js/app_devcards.js")]]))

(defn fetch-url [url]
  (enlive/html-resource (java.net.URL. url)))

(defn random-product-url []
  (let [offset (rand-int 10000)
        low-price 0
        high-price 2000]
    (printf "Offset: %d\n" offset)
    (str "https://www.blinq.com/search/go?p=Q&lbc=blinq&w=*&af=price%3a%5b"
         low-price
         "%2c"
         high-price
         "%5d&isort=price&method=and&view=grid&af=price%3a%5b0%2c2000%5d&ts=infinitescroll&srt="
         offset)))

; TODO: prefech in the background to make it faster
(defn random-product []
  (let [page (fetch-url (random-product-url))
        title (enlive/text (first
                             (enlive/select
                               page
                               [[:li (enlive/nth-of-type 1)] :h3.tile-desc])))
        img (get-in (first (enlive/select
                      page
                      [[:li (enlive/nth-of-type 1)] :div.tile-img :img])) [:attrs :src])
        url (get-in (first (enlive/select
                      page
                      [[:li (enlive/nth-of-type 1)] :a.tile-link])) [:attrs :href])
        price (enlive/text (first
                             (enlive/select
                               page
                               [[:li (enlive/nth-of-type 1)] :span.live_saleprice])))]
        (response {:img img
                   :title title
                   :price price
                   :url url})))

(defroutes routes
  (GET "/" [] loading-page)
  (GET "/triage" [] loading-page)
  (GET "/about" [] loading-page)
  (GET "/intro" [] loading-page)
  (GET "/tournament" [] loading-page)
  (GET "/cards" [] cards-page)
  (GET "/random-product" [] (random-product))
  (GET "/test" [] (response {:a :b}))
  (resources "/")
  (not-found "Not Found"))

(def app
  (let [handler (wrap-json-response (wrap-defaults #'routes site-defaults))]
    (if (env :dev) (-> handler wrap-exceptions wrap-reload) handler)))

