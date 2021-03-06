(ns suckdit.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.dom :as dom]
            [goog.events :as events]
            [cljs.core.async :refer [<! put! chan timeout]])
  (:import [goog.net Jsonp]
           [goog Uri]))

(def SUBREDDIT "pics")

(defn listen [el type]
  (let [out (chan)]
    (events/listen el type
      (fn [e] (put! out e)))
    out))

(defn jsonp [uri & {:keys [out]}]
  (let [out (or out (chan))
        req (Jsonp. (Uri. uri) "jsonp")]
    (.send req nil (fn [res] (put! out res)))
    out))

(defn reddit-r [r & {:keys [limit after]}]
  (let [limit (or limit 1)
        after (if after (str "&after=" after) "")]
    (str "http://www.reddit.com/r/" r "/.json?limit=" limit after)))

(defn ch-map [f in]
  (let [out (chan)]
    (go (while true
         (let [v (<! in)]
             (>! out (f v)))))
    out))

(defn ch-flat-map [f in]
  (let [out (chan)]
    (go (while true
         (let [v (<! in)]
           (doseq [i (f v)]
             (>! out i)))))
    out))

(defn split-posts [resp]
  (let [r (js->clj resp)
        o (-> r (get "data") (get "children"))]
    (map #(-> % (get "data") (select-keys ["domain" "thumbnail" "author" "name" "title" "url"])) o)))

(defn post->image [post]
  (let [url (post "url")
        image? #(.match % #"(jpeg|jpg|gif|png)$")
        plain-imgur? #(.match % #"https?://imgur.com/(\w+)$")
        imgurize #(.replace % #"https?://imgur.com/(\w+)$" "http://i.imgur.com/$1.jpg")]
    (cond
     (image? url) url
     (plain-imgur? url) (imgurize url)
     :else (post "thumbnail"))))

(def reddit (chan))

(def posts (ch-map #(assoc % :image (post->image %)) (ch-flat-map split-posts reddit)))

(defn request-more [after]
  (jsonp (reddit-r SUBREDDIT :limit 24 :after after) :out reddit))

(defn post->html [post]
  (let [style (str "background-image: url(" (post :image) ")")]
    (dom/createDom "div" "post"
                   (dom/createDom "a" #js{:href (post "url") :style style :target "_blank"}
                                  (post "title")))))

(def scroll (let [out (chan) interval 3000]
    (go (while true
      (<! (timeout interval))
      (let [height (dom/getDocumentHeight)
            scroll (.-y (dom/getDocumentScroll))]
        (>! out [height scroll]))))
     out))

(def last-post nil)
(def posts-loaded 0)

(let [ribbon (dom/getElement "ribbon")
      clicks (listen (dom/getElement "moar") "click")
      wheight (.-height  (dom/getViewportSize))]
  (go (while true
        (let [post (<! posts)]
          (set! last-post (post "name"))
          (set! posts-loaded (inc posts-loaded))
          (dom/append ribbon (post->html post)))))
  (go (while true
        (<! clicks)
        (request-more last-post)))
  (go (while true
        (let [[h s] (<! scroll) k 2]
          (when (> s (- h (* wheight k)))
            (request-more last-post))))))

(request-more nil)
