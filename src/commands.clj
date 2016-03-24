(ns commands
  (:require [clojure.string :as string]
            [evaluator :as evaluator]
            [expander :as expander]))

;;move to common
;;http://stackoverflow.com/questions/9568050/in-clojure-how-to-write-a-function-that-applies-several-string-replacements
(defn replace-several [content & replacements]
  (let [replacement-list (partition 2 replacements)]
    (reduce #(apply string/replace %1 %2) content replacement-list)))

(def pseudo-bnf-non-terminals [
                "<term>" "(<word>(?:<white space><word>)*)"
                "<word>" "(?:[a-zA-Z0-9&-/]+)"
                "<white space>" "\\s*"
                "<separator>" "="
                "<description>" "(.+)"])

;;## white space at the end?
(def pseudo-bnf-terminals
  (list
   ;;help
   ["\\!help" expander/help]
   ;;explain
   ["\\!explain<white space><term>" expander/lookup]
   ["\\?<white space><term>" expander/lookup]
   ;;define
    ["\\!define<white space><term><white space><separator><white space><description>" expander/teach]
    ["\\?<white space><term><white space><separator><white space><description>" expander/teach] ;; ## potential confusion for ?define term . abc
;   ["\\?+<white space><term><white space><separator><white space><description>" identity]
   ;;drop
;   ["\\!drop<white space><term><white space><separator><white space><description>" identity]
;   ["\\?-<white space><term><white space><separator><white space><description>" identity]
;   ["\\!alias<white space><term><white space><separator><white space><term>" identity]
   ;;validate
   ;;["\\!+1<white space><term> [# identity]" identity]
   ;;["\\!-1<white space><term> [# identity]" identity]
   ;;subscribtion
;   ["\\!subscribe" identity]
;   ["\\!unsubscribe" identity]
;   ["\\!seen<white space><term>" identity]
;   ["\\!forget<white space><term>" identity]
   ;;Clojure REPL
   ["\\'(.*)" #(vector false (evaluator/evaluate %))]))

(def match-map (map #(vector (re-pattern (str "^" (apply replace-several (first %) pseudo-bnf-non-terminals) "$")) (second %) ) pseudo-bnf-terminals))

(defn parse-and-execute [s]
  (if (= "?WF*$" s)
    [false "Working from Starbucks."]
    (some #(when-let [m (re-matches (first %) s)]
             ((second %) m))
          match-map)))
