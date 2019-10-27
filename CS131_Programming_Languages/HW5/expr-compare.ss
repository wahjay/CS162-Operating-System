#lang racket


(define LAMBDA (string->symbol "\u03BB"))


(define has-lambda (lambda (b)
    (cond [(null? b) #f]
          [(list? (car b)) (or (has-lambda (car b)) (has-lambda (cdr b)))]
          [(or (eq? 'lambda (car b)) (eq? LAMBDA (car b))) #t]
          [else (has-lambda (cdr b))])))

(define find-unmatched (lambda (a b)
          (cond [(or (null? a) (null? b)) '()]
          [(equal? (car a) (car b)) (cons (car a) (find-unmatched (cdr a) (cdr b)))]
          [(and (not (equal? (car a) (car b))) (list? (car a)) (list? (car b)))  (cons (find-unmatched (car a) (car b)) (find-unmatched (cdr a) (cdr b)))]
          [(not (equal? (car a) (car b)))  (cons `(if % ,(car a) ,(car b)) (find-unmatched (cdr a) (cdr b)) ) ])))




(define find-mismatched (lambda (a b)
                     (cond [(null? a) '()]
                           [(eq? a b) a]
                           [(not (eq? a b)) `(if % ,a ,b)]
                           )))


(define (bind x y) (string->symbol (string-append (symbol->string x) "!" (symbol->string y))))

(define compare-head (lambda (a b)
             (cond [(or (eq? LAMBDA a) (eq? LAMBDA b)) LAMBDA]
                   [(and (eq? 'lambda a) (eq? 'lambda b)) 'lambda])))

(define compare-arg (lambda (a b)
             (cond [(null? a) '()]
                   [(not (eq? (car a) (car b))) (cons (bind (car a) (car b)) (compare-arg (cdr a) (cdr b))) ]
                   [else (cons (car a) (compare-arg (cdr a) (cdr b)))]
                   )))


(define compare-body (lambda (a b c)
                    (cond [(null? a) '()]
                          [(symbol? a) (find-mismatched (pre-exists a c) (suf-exists b c))]
                          [(list? (car a)) (cons (compare-body (car a) (car b) c) (compare-body (cdr a) (cdr b) c))]
                          [(or (eq? 'lambda (car a)) (eq? LAMBDA (car a))) (break-up a b)]
                          [(list? a) (cons (find-mismatched (pre-exists (car a) c) (suf-exists (car b) c)) (compare-body (cdr a) (cdr b) c))]
                          )))



(define pre-exists (lambda (a b)
            (cond [(null? b) a]
                  [(string-prefix? (symbol->string (car b)) (symbol->string a)) (car b)]
                  [(not (string-prefix? (symbol->string (car b)) (symbol->string a))) (pre-exists a (cdr b))]
                  )))

(define suf-exists (lambda (a b)
            (cond [(null? b) a]
                  [(string-suffix? (symbol->string (car b)) (symbol->string a)) (car b)]
                  [(not (string-suffix? (symbol->string (car b)) (symbol->string a))) (suf-exists a (cdr b))]
                  )))

(define check-nested (lambda (a)
                  (cond [(null? a) '()]
                        [(not (list? a)) '()]
                        [(list? (car a)) (cons (check-nested (car a)) (check-nested (cdr a)))]
                        [(or (eq? 'lambda (car a)) (eq? LAMBDA (car a))) (cons (car a) (check-nested (cdr a)))]
                        [else (check-nested (cdr a))] 
                       )))


(define check-pair (lambda (a)
                  (cond [(null? a) #f]
                        [(and (pair? (car a)) (not (list? (car a)))) #t ]
                        [else (check-pair (cdr a))]
                        )))

(define compare (lambda (a b)
    (cond [(and (list? (cadr a)) (not(list? (cadr b)))) (find-mismatched a b)]
          [(and (list? (cadr b)) (not(list? (cadr a)))) (find-mismatched a b)]
          [else (list (compare-head (car a) (car b)) (compare-arg (cadr a) (cadr b)) (compare-body (caddr a) (caddr b) (compare-arg (cadr a) (cadr b))))])))



(define break-up (lambda (a b)
             (cond [(null? a) '()]
                   [(list? (car a)) (cons (break-up (car a) (car b)) (break-up (cdr a) (cdr b)))] 
                   [(or (eq? 'lambda (car a)) (eq? LAMBDA (car a))) (compare a b)]
                   [(eq? (car a) (car b)) (cons (car a) (break-up (cdr a) (cdr b)))]
                   [(and (eq? #t (car a)) (eq? #f (car b))) (cons '% (break-up (cdr a) (cdr b)))]
                   [(and (eq? #f (car a)) (eq? #t (car b))) (cons '(not %) (break-up (cdr a) (cdr b)))]
                   [(not (eq? (car a) (car b))) (cons `(if % ,(car a) ,(car b)) (break-up (cdr a) (cdr b)))]
                   [(and (list? a) (list? b)) (find-unmatched a b)])))

(define break-down (lambda (a b)
                 (cond [(null? a) '()]
                       [(list? (car a)) (cons (break-down (car a) (car b)) (break-down (cdr a) (cdr b)))]
                       [(or (eq? 'lambda (car a)) (eq? LAMBDA (car b))) (break-up a b)]
                       )))


(define expr-compare (lambda (a b)
                (cond
                      [(equal? a b) a]
                      [(and (not(list? a)) (not(list? b)) (not(equal? a b))) `(if % ,a ,b)]
                      [(and (and (list? a) (list? b)) (not (eq? (length a) (length b)))) `(if % ,a ,b)]
                      [(and (and (list? a) (list? b)) (or (eq? 'quote (car a)) (eq? 'quote (car b)))) `(if % ,a ,b)]
                      [(or (and (list? a) (not(list? b))) (and (list? b) (not(list? a)))) `(if % ,a ,b)]
		      [(and (and (list? a) (list? b)) (and (check-pair a) (not (check-pair b))) (and (check-pair b) (not (check-pair a)))) `(if % ,a ,b)]
                      [(and (equal? #t a) (equal? #f b)) '%]
                      [(and (equal? #f a) (equal? #t b)) '(not %)]
		      [(> (length (flatten (check-nested a))) 1) (break-down a b)]
                      [(and (has-lambda a) (has-lambda b)) (break-up a b)]
                      [(and (and (list? a) (list? b)) (or (eq? 'if (car a)) (eq? 'if (car b))) (not (eq? (car a) (car b)))) `(if % ,a ,b)]
                      [(and (and (list? a) (list? b)) (or (eq? 'quote (car a)) (eq? 'quote (car b)))) `(if % ,a ,b)]
                      [(and (list? a) (list? b)) (find-unmatched a b)]
                      


                 )))



 
(define (test-expr-compare x y) 
  (and (equal? (eval x) (eval (list 'let '((% #t)) (expr-compare x y)))) 
       (equal? (eval y) (eval (list 'let '((% #f)) (expr-compare x y))))))



(define test-expr-x '(lambda (c d) a) '(lambda (c . d) a))
(define test-expr-y '(lambda (a b) a) '(lambda (a . b) a))









 
;(test-expr-compare test-expr-x test-expr-y)












#|
(expr-compare '((λ (a b) (f a b)) 1 2) '((λ (a b) (f b a)) 1 2))
(expr-compare '((λ (a b) (f a b)) 1 2) '((λ (a c) (f c a)) 1 2))
(expr-compare '(+ #f ((λ (a b) (f a b)) 1 2)) '(+ #t ((lambda (a c) (f a c)) 1 2)))
(expr-compare '((lambda (a) a) c) '((lambda (b) b) d))
(expr-compare '(lambda a a) '(lambda (a) a))
(expr-compare '((lambda (a) (f a)) 1) '((λ (a) (g a)) 2))
(expr-compare '((lambda (a) (f a)) 1) '((lambda (a) (g a)) 2))
(expr-compare ''((λ (a) a) c) ''((lambda (b) b) d))
(expr-compare '(if x y z) '(if x z z))
(expr-compare '(if x y z) '(g x y z))
(expr-compare '(quote (a b)) '(quote (a c)))
(expr-compare '(quoth (a b)) '(quoth (a c)))
(expr-compare ''(a b) ''(a c))
(expr-compare '(list) '(list a))
(expr-compare '(cons a b) '(list a b))
(expr-compare '(cons (cons a b) (cons b c)) '(cons (cons a c) (cons a c)))
(expr-compare '(cons a b) '(cons a c))
(expr-compare '(cons a b) '(cons a b))
(expr-compare 'a '(cons a b))
(expr-compare #f #t)
(expr-compare #t #f)
(expr-compare #f #f)
(expr-compare #t #t)
(expr-compare 12 20)
(expr-compare 12 12)
(expr-compare '(lambda (a b) a) '(lambda (a . b) a))
(expr-compare '((lambda (a) (eq? a ((λ (a b) ((λ (a b) (a b)) b a)) a (lambda (a) a)))) (lambda (b a) (b a)))
              '((λ (a) (eqv? a ((lambda (b a) ((lambda (a b) (a b)) b a)) a (λ (b) a)))) (lambda (a b) (a b))))
|#


