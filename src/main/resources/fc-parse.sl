fc-parse `.t` .parsed
	:- fc-parse-op-sugar .t .t1
	, !, fc-parse .t1 .parsed
#
fc-parse .t .parsed
	:- bound .t, fc-parse-sugar .t .t1
	, !, fc-parse .t1 .parsed
#
fc-parse (.var => .do) (FUN .var .do1)
	:- !, fc-parse .do .do1
#
fc-parse (.type of .value) (PRAGMA (TYPE-CAST DOWN .type1) .value1)
	:- fc-parse-type .type .type1
	, !, fc-parse .value .value1
#
fc-parse (data .class over some .typeVars as .type >> .do) (
	PRAGMA (DEF-TYPE .type1 .class1 .typeVars1) .do1
) :- fc-parse-type .type .type1
	, fc-parse-type-list .typeVars .typeVars1
	, fc-parse-type .class .class1
	, !, fc-parse .do .do1
#
fc-parse (data .class over .typeVar as .type >> .do) .do1
	:- !, fc-parse (data .class over some (.typeVar,) as .type >> .do) .do1
#
fc-parse (data .class as .type >> .do) .do1
	:- !, fc-parse (data .class over some () as .type >> .do) .do1
#
fc-parse (define .var := .value >> .do) (
	PRAGMA DEF-OUTSIDE (DEF-VARS (.var (PRAGMA TYPE-RESOLVE .value1),) .do1)
) :- !
	, once (fc-parse .value .value1
		; fc-error "at variable" .var
	)
	, fc-parse .do .do1
#
fc-parse (lets (.var := .value # .list) >> .do) (
	DEF-VARS (.var (PRAGMA TYPE-RESOLVE .value1), .list1) .do1
) :- !
	, once (fc-parse .value .value1
		; fc-error "at variable" .var
	)
	, fc-parse (lets .list >> .do) (DEF-VARS .list1 .do1)
#
fc-parse (lets () >> .do) (DEF-VARS () .do1)
	:- !, fc-parse .do .do1
#
fc-parse (.callee {.parameter}) (INVOKE .parameter1 .callee1)
	:- !, fc-parse .callee .callee1
	, fc-parse .parameter .parameter1
#
fc-parse (if .if then .then .otherwise) (IF .if1 .then1 .else1)
	:- !
	, fc-parse .if .if1
	, fc-parse .then .then1
	, (.otherwise = else .else, !, fc-parse .else .else1
		; .otherwise = else-if .elseif, !, fc-parse (if .elseif) .else1
		; fc-error "Unknown else clause" .otherwise
	)
#
fc-parse (if-bind (.v0 = .v1) then .then else .else) .parsed
	:- !
	, fc-parse .v0 .vp0
	, fc-parse .v1 .vp1
	, fc-parse .then .thenp
	, fc-parse .else .elsep
	, fc-bind .vp0 .vp1 .thenp .elsep .parsed
#
fc-parse (skip-type-check .do) (PRAGMA TYPE-SKIP-CHECK .do1)
	:- !, fc-parse .do .do1
#
fc-parse (using source .lib >> .do) .dox
	:- !, fc-load-library .lib .do .do1
	, fc-parse .do1 .dox
#
fc-parse (using external .lib >> .do) (USING EAGER EXTERNAL .lib .do1)
	:- !, fc-parse .do .do1
#
fc-parse (using .lib >> .do) (USING EAGER BUILTIN .lib .do1)
	:- !, fc-parse .do .do1
#
fc-parse (.p0 .p1) (PRAGMA TYPE-CAST-TO-CLASS (PAIR .parsed0 .parsed1))
	:- !
	, fc-parse .p0 (PRAGMA TYPE-CAST-TO-CLASS .parsed0)
	, fc-parse .p1 .parsed1
#
fc-parse (.p0, .p1) (PAIR .parsed0 .parsed1)
	:- !
	, fc-parse .p0 .parsed0
	, fc-parse .p1 .parsed1
#
fc-parse .tree (TREE .oper .left1 .right1)
	:- tree .tree .left .oper .right
	, fc-operator .oper
	, !
	, fc-parse .left .left1
	, fc-parse .right .right1
#
fc-parse () (ATOM ()) :- ! #
fc-parse atom:.a (ATOM .a) :- ! #
fc-parse .b (BOOLEAN .b) :- fc-is-boolean .b, ! #
fc-parse .i (NUMBER .i) :- is.int .i, ! #
fc-parse .v (NEW-VAR .nv) :- to.string .v "_", temp .nv, ! #
fc-parse .v (NEW-VAR .nv) :- fc-parse-bind-variable .v .nv, ! #
fc-parse .a (PRAGMA TYPE-CAST-TO-CLASS (ATOM .a)) :- fc-is-atom .a, ! #
fc-parse string:.s (STRING .s) :- ! #
fc-parse .v (VAR .v) :- is.atom .v, ! #
fc-parse .d _ :- fc-error "Unknown expression" .d #

fc-parse-list () () :- ! #
fc-parse-list (.e, .es) (.p, .ps) :- !, fc-parse .e .p, fc-parse-list .es .ps #

fc-parse-op-sugar .t .do
	:- tree .t .left .op .right
	, once (
		fc-operator .op
		, fc-parse-op-sugar1 .op .left .right .do
	)
#

fc-parse-op-sugar1 .op () () (.var0 => .var1 => .t1)
	:- !, temp .var0, temp .var1, tree .t1 .var0 .op .var1
#
fc-parse-op-sugar1 .op () .right (.var => .t1)
	:- !, temp .var, tree .t1 .var .op .right
#
fc-parse-op-sugar1 .op .left () (.var => .t1)
	:- !, temp .var, tree .t1 .left .op .var
#

fc-parse-sugar error (throw {}) :- ! #
fc-parse-sugar (if (`.p` = `.q`) .thenElse) (if-bind (.p = .q) .thenElse) :- ! #
fc-parse-sugar (if (.p = `.q`) .thenElse) (if-bind (.p = .q) .thenElse) :- ! #
fc-parse-sugar (if (`.p` = .q) .thenElse) (if-bind (.p = .q) .thenElse) :- ! #
fc-parse-sugar (let `.binds` := .value >> .do) (if-bind (.binds = .value) then .do else error) :- ! #
fc-parse-sugar (let .var := .value >> .do) (lets (.var := .value #) >> .do) :- ! #
fc-parse-sugar (case || .bind => .then || .otherwise) .p1
	:- !, temp .var
	, .p1 = (.var =>
		case
		|| (.var = .bind) .then
		|| ((case || .otherwise) {.var})
	)
#
fc-parse-sugar (case || .if .then || .otherwise) .p1
	:- !
	, .p1 = if .if then .then else (case || .otherwise)
#
fc-parse-sugar (case || .p) .p :- ! #
fc-parse-sugar (.l && .r) ((and {.l} {.r})) :- ! #
fc-parse-sugar (.l || .r) (or {.l} {.r}) :- ! #
fc-parse-sugar ({.t}) (.var => .var {.t}) :- !, temp .var #
fc-parse-sugar (.l; .r) (+lcons {.l} {.r}) :- ! #
fc-parse-sugar (.l . .r) (.var => .l {.r {.var}}) :- !, temp .var #
fc-parse-sugar (.l | .r) (.r {.l}) :- ! #
fc-parse-sugar (do >> .do) (
	define fun-to-monad := (:t => (number -> :t) -> do-of :t) of (skip-type-check id) >>
	define monad-to-fun := (:t => do-of :t -> (number -> :t)) of (skip-type-check id) >>
	fun-to-monad {scope =>
		define unliftm := ({scope} . monad-to-fun) >>
		expand getm := +getm {scope} >>
		expand setm := +putm {scope} >>
		.do
	}
) :- ! #
fc-parse-sugar (definem .type .mv # .monad) (
	define .mv := (mutable^.type) of (skip-type-check atom:.atom) >> .monad
)
	:- !, temp .atom
#
fc-parse-sugar (setm .mv := .value # .monad) (+setm {scope} {.mv} {.value} # .monad) :- ! #
fc-parse-sugar (.monad #) (unliftm {.monad}) :- ! #
fc-parse-sugar (.monad # .monads0) (unliftm {seqm {.monad} {.monads1}})
	:- fc-parse-sugar .monads0 (unliftm {.monads1}), !
#
fc-parse-sugar (expand .var := .value >> .do) .do1
	:- !, replace .var .value .do .do1
#
fc-parse-sugar (.var as .type => .do) (.var1 => (define .var :=  .type of .var1 >> .do))
	:- !, temp .var1
#
fc-parse-sugar (`.bind` => .do) (.var => (if-bind (.var = .bind) then .do else error))
	:- !, temp .var
#
fc-parse-sugar (anything => .do) (.var => .do) :- !, temp .var #
fc-parse-sugar (not .b) (not {.b}) :- ! #
fc-parse-sugar (.a ++ .b) (append {.a} {.b}) :- ! #
fc-parse-sugar (.s until .e) (range {.s} {.e} {1}) :- ! #
fc-parse-sugar (.f/) (flip {.f}) :- ! #
fc-parse-sugar "" () :- ! #
fc-parse-sugar .s (string of (
	skip-type-check (string:.s | (atom:INTRN!CharsIntrinsics.charsString | getintrn | callintrn-v1))
))
	:- is.string .s, string.length .s .l, .l > 2, !
#
fc-parse-sugar .s (.ascii; .cs)
	:- is.string .s
	, !, substring .s 0 1 .c, substring .s 1 0 .cs
	, to.int .c .ascii
#

fc-parse-type .t .t :- not bound .t, ! #
fc-parse-type any .t :- not bound .t, ! #
fc-parse-type _ .t :- is.cyclic .t, !, fc-error "Cyclic type" .t #
fc-parse-type (.paramType -> .returnType) (FUN-OF .paramType1 .returnType1)
	:- !
	, fc-parse-type .paramType .paramType1
	, fc-parse-type .returnType .returnType1
#
fc-parse-type ([.type]) (LIST-OF .type1) :- !, fc-parse-type .type .type1 #
fc-parse-type .a (ATOM-OF .a) :- fc-is-atom .a, ! #
fc-parse-type atom:.a (ATOM-OF .a) :- ! #
fc-parse-type (data-of .d) (DATA-OF .d) :- ! #
fc-parse-type (do-of .do) (DO-OF .do1) :- !, fc-parse-type .do .do1 #
fc-parse-type (.functor^.type0) (FUNCTOR-OF .functor .type1) :- !, fc-parse-type .type0 .type1 #
fc-parse-type .do (PAIR-OF .type0 .type1)
	:- (.do = (.t0, .t1); .do = (.t0 .t1)), !
	, fc-parse-type .t0 .type0
	, fc-parse-type .t1 .type1
#
fc-parse-type (.typeVar => .type) (GENERIC-OF .typeVar1 .type1)
	:- !
	, fc-parse-type .type .type1
	, fc-parse-type .typeVar .typeVar1
#
-- Keeps contained in class definition for tuple matching.
fc-parse-type (.type {.paramType}) (CLASS (PARAMETERIZED .paramType1 .class))
	:- !
	, fc-parse-type .type (CLASS .class)
	, fc-parse-type .paramType .paramType1
#
fc-parse-type :.typeVar (TYPE-VAR .typeVar) :- ! #
fc-parse-type boolean BOOLEAN :- ! #
fc-parse-type number NUMBER :- ! #
fc-parse-type string (LIST-OF NUMBER) :- ! #
fc-parse-type .t (CLASS .t) :- is.atom .t #

fc-parse-type-list () () :- ! #
fc-parse-type-list (.type, .types) (.type1, .types1)
	:- fc-parse-type .type .type1, fc-parse-type-list .types .types1
#

fc-parse-bind-variable .v .vd
	:- is.atom .v, to.string .v .s0, substring .s0 0 1 "$"
	, !, substring .s0 1 0 .s1, to.atom .s1 .vd
#

fc-operator .oper
	:- member (' + ', ' - ', ' * ', ' / ', ' %% ',
		' = ', ' != ',
		' > ', ' < ', ' >= ', ' <= ',
		',', ';',
		' . ',
	) .oper
#

fc-is-atom .a :- is.atom .a, to.string .a .s, substring .s 0 1 .ch
	, "A" <= .ch, .ch <= "Z"
#

fc-is-boolean true #
fc-is-boolean false #

fc-bind .v0 .v1 .tep :- .v0 = NEW-VAR _, !, fc-bind0 .v1 .v0 .tep #
fc-bind .v0 .v1 .tep :- .v1 = NEW-VAR _, !, fc-bind0 .v0 .v1 .tep #
fc-bind .v0 .v1 .tep
	:- once (fc-bind-cons .v0 _ _
		; .v0 = PAIR _ _
		; .v0 = PRAGMA _
	)
	, !, fc-bind0 .v1 .v0 .tep
#
fc-bind .v0 .v1 .tep :- fc-bind0 .v0 .v1 .tep #

fc-bind0 .v0 .v1 .then .else .parsed
	:- fc-bind-cons .v0 .h0 .t0
	, fc-bind-cons .v1 .h1 .t1
	, !, .then1 = PRAGMA ( -- Handle type fails like .v0 = (1; true;)
		TYPE-VERIFY (TREE ' = ' .v0 .v1) BOOLEAN
	) .then
	, fc-bind-pair .h0 .t0 .h1 .t1 .then1 .else .parsed
#
fc-bind0 (PAIR .p0 .q0) (PAIR .p1 .q1) .then .else .parsed
	:- !, fc-bind-pair .p0 .q0 .p1 .q1 .then .else .parsed
#
fc-bind0 .v0 (NEW-VAR .nv) .then _ (DEF-VARS (.nv .v0,) .then)
	:- !
#
fc-bind0 .v0 .v1 .then .else (
	DEF-VARS (.elseVar (WRAP .else), .v0var .v0,) (
		IF (INVOKE (VAR .v0var) (VAR is-list)) (
			DEF-VARS (
				.headVar (INVOKE (VAR .v0var) (VAR +lhead)),
				.tailVar (INVOKE (VAR .v0var) (VAR +ltail)),
			)
			.then1
		) .else1
	)
) :- fc-bind-cons .v1 .h1 .t1
	, !
	, temp .elseVar, temp .v0var, temp .headVar, temp .tailVar
	, .else1 = UNWRAP (VAR .elseVar)
	, fc-bind-pair (VAR .headVar) (VAR .tailVar) .h1 .t1 .then .else1 .then1
#
fc-bind0 .v0 (PAIR .p1 .q1) .then .else (
	DEF-VARS (.elseVar (WRAP .else), .v0var (PRAGMA (TYPE-CAST UP _) .v0),) (
		IF (INVOKE (VAR .v0var) (VAR is-pair)) (
			DEF-VARS (
				.leftVar (INVOKE (VAR .v0var) (VAR +pleft)),
				.rightVar (INVOKE (VAR .v0var) (VAR +pright)),
			) .then1
		) .else1
	)
) :- !
	, temp .elseVar, temp .v0var, temp .leftVar, temp .rightVar
	, .else1 = UNWRAP (VAR .elseVar)
	, fc-bind-pair (VAR .leftVar) (VAR .rightVar) .p1 .q1 .then .else1 .then1
#
fc-bind0 .v0 (PRAGMA _ .v1) .then .else .parsed
	:- !
	, fc-bind .v0 .v1 .then .else .parsed
#
fc-bind0 .v0 .v1 .then .else (
	IF (TREE ' = ' .v0 .v1) .then .else
) #

fc-bind-cons (INVOKE .t INVOKE .h VAR +lcons) .h .t #
fc-bind-cons (INVOKE .t INVOKE .h VAR +pcons) .h .t #

fc-bind-pair .h0 .t0 .h1 .t1 .then .else (DEF-VARS (.elseVar (WRAP .else),) .parsed)
	:- temp .elseVar
	, .else1 = UNWRAP (VAR .elseVar)
	, fc-bind .h0 .h1 .then1 .else1 .parsed
	, fc-bind .t0 .t1 .then .else1 .then1
#
