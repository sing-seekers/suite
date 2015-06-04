-------------------------------------------------------------------------------
-- Type inference predicates
--
-- Environment consists of:
-- .ue - dictionary of inside variables / their corresponding types
-- .ve - dictionary of outside variables / their corresponding types, specialized
-- .te - list of types / their corresponding belonging classes
-- .tr - type deduction rule to be assembled
--
-- Inside variables include parent function definitions and parameter variables
-- that do not need type specialization.
-- Outside variables are local variables that require type specialization.
--
-- Notes about generic types:
-- - Generic type class, usually used in abstract data structures.
--   Written like B-TREE {:t}.
--   Represented internally as (CLASS (PARAMETERIZED (VAR t) B-TREE)).
--   Resolved by binding the type structures.
-- - Generic type caused by not enough variable information during type inference.
--   They are specialized to bounded type node and put into the outside variable
--   list, and generalized back when being used.
--

fc-infer-type .do .type
	:- try (fc-infer-type-rule .do ()/()/() .tr/() .type) .ex (fc-error .ex)
	, fc-resolve-type-rules .tr
#

fc-infer-type-rule .p .env .tr/.tr .type
	:- fc-find-simple-type .p .env .type
#
fc-infer-type-rule (CONS L .v0 .v1) .env .tr0/.trx (LIST-OF .t)
	:- fc-infer-type-rule .v0 .env .tr0/.tr1 .t
	, fc-infer-type-rule .v1 .env .tr1/.trx (LIST-OF .t)
#
fc-infer-type-rule (CONS P .v0 .v1) .env .tr0/.trx (PAIR-OF .t0 .t1)
	:- fc-infer-type-rule .v0 .env .tr0/.tr1 .t0
	, fc-infer-type-rule .v1 .env .tr1/.trx .t1
#
fc-infer-type-rule (DEF-VARS .vvs .do) .ue/.ve/.te .tr0/.trx .type
	:- fc-define-var-types () .vvs .vvts .ue/.ue1
	, .env1 = .ue1/.ve/.te
	, fc-infer-var-types .vvts .env1 .tr0/.tr1
	, fc-infer-type-rule .do .env1 .tr1/.trx .type
#
fc-infer-type-rule ERROR _ .tr/.tr _
#
fc-infer-type-rule (FUN .var .do) .ue/.ve/.te .tr (FUN-OF .varType .type)
	:- fc-dict-add .var/.varType .ue/.ue1
	, fc-infer-type-rule .do .ue1/.ve/.te .tr .type
#
fc-infer-type-rule (IF .if .then .else) .env .tr0/.trx .type
	:- fc-infer-type-rule .if .env .tr0/.tr1 BOOLEAN
	, fc-infer-type-rule .then .env .tr1/.tr2 .type
	, fc-infer-type-rule .else .env .tr2/.trx .type
#
fc-infer-type-rule (INVOKE .param .callee) .env .tr0/.trx .returnType
	:- fc-infer-type-rule .callee .env .tr0/.tr1 (FUN-OF .paramType .returnType)
	, fc-infer-type-rule .param .env .tr1/.trx .paramType
#
fc-infer-type-rule (
	PRAGMA DEF-OUTSIDE (DEF-VARS .vvs .do)
) .ue/.ve/.te .tr0/.trx .type
	:- !
	, fc-define-var-types () .vvs .vvts .ue/.ue1
	, fc-infer-var-types .vvts .ue1/.ve/.te .tr0/.tr1
	, fc-define-var-types SP .vvs .vvts .ve/.ve1
	, fc-infer-type-rule .do .ue/.ve1/.te .tr1/.trx .type
#
fc-infer-type-rule (
	PRAGMA (DEF-TYPE .definedType .class) .do
) .ue/.ve/.te .tr .type
	:- !
	, specialize .definedType/.class .pair
	, fc-infer-type-rule .do .ue/.ve/(.pair, .te) .tr .type
#
fc-infer-type-rule (PRAGMA (TYPE-CAST .superType) .do) .ue/.ve/.te .tr0/.trx .type
	:- !
	, .type = .superType
	, fc-infer-type-rule .do .ue/.ve/.te .tr0/.tr1 .subType
	, .tr1 = (SUB-SUPER-TYPES .te .subType .superType, .trx)
#
fc-infer-type-rule (PRAGMA TYPE-CAST-TO-CLASS .pair) .env .tr .classType
	:- !
	, .classType = CLASS _
	, fc-infer-type-rule (PRAGMA (TYPE-CAST .classType) .pair) .env .tr .classType
#
fc-infer-type-rule (PRAGMA TYPE-RESOLVE .do) .env .tr/.tr .type
	:- !
	, fc-infer-type-rule .do .env .tr1/() .type
	, fc-resolve-type-rules .tr1
#
fc-infer-type-rule (PRAGMA TYPE-SKIP-CHECK _) _ .tr/.tr _
	:- !
#
fc-infer-type-rule (PRAGMA (TYPE-VERIFY .var .varType) .do) .env .tr0/.trx .type
	:- !
	, fc-infer-type-rule .var .env .tr0/.tr1 .varType
	, fc-infer-type-rule .do .env .tr1/.trx .type
#
fc-infer-type-rule (PRAGMA _ .do) .env .tr .type
	:- fc-infer-type-rule .do .env .tr .type
#
fc-infer-type-rule (TREE .oper .left .right) .env .tr0/.trx .type
	:- once (
		member (' + ', ' - ', ' * ', ' / ', ' %% ',) .oper, !
		, .inputType = NUMBER
		, .type = NUMBER
		; member (' = ', ' != ', ' > ', ' < ', ' >= ', ' <= ',) .oper, !
		, .type = BOOLEAN
	)
	, fc-infer-type-rule .left .env .tr0/.tr1 .inputType
	, fc-infer-type-rule .right .env .tr1/.trx .inputType
#
fc-infer-type-rule (USING _ _ .lib .do) .env .tr/.tr .type
	:- fc-load-precompiled-library .lib (.pred # _ # _ #)
	, clone .pred (
		fc-infer-type-rule-using-lib .lib .do .env .tr1/() .type :- .tail
	)
	, once .tail
	, fc-resolve-type-rules .tr1
#
fc-infer-type-rule .do .env .tr .type
	:- (.do = UNWRAP .do1; .do = WRAP .do1)
	, fc-infer-type-rule .do1 .env .tr .type
#
fc-infer-type-rule .do _ _ _
	:- throw "Unmatched types"
#

fc-define-var-types .sp (.var .value, .vvs) (.var .value .varType0, .vvts) .ue0/.uex
	:- once (.sp = SP, graph.specialize .varType0 .varType1; .varType0 = .varType1)
	, fc-dict-add .var/.varType1 .ue0/.ue1
	, fc-define-var-types .sp .vvs .vvts .ue1/.uex
#
fc-define-var-types _ () () .ue/.ue
#

fc-infer-var-types (.var .value .varType, .vvts) .env .tr0/.trx
	:- try (fc-infer-type-rule .value .env .tr0/.tr1 .varType)
	.ex (throw .ex "%0Aat variable" .var)
	, fc-infer-var-types .vvts .env .tr1/.trx
#
fc-infer-var-types () _ .tr/.tr
#

fc-find-simple-type (ATOM ()) _ (LIST-OF _) :- ! #
fc-find-simple-type (ATOM .a) _ (ATOM-OF .a) #
fc-find-simple-type (BOOLEAN _) _ BOOLEAN #
fc-find-simple-type (DO _) _ (FUNCTOR-OF Do _) #
fc-find-simple-type (NUMBER _) _ NUMBER #
fc-find-simple-type (VAR .var) .ue/.ve/_ .type
	:- once (
		fc-dict-get .ue .var/.type
		; fc-dict-get .ve .var/.varType, !, graph.generalize .varType .type
		; fc-default-fun-type .var .type -- ; fc-define-default-fun _ .var _
		; throw "Undefined variable" .var
	)
#

fc-resolve-type-rules .tr
	:- once (not is.cyclic .tr; throw "Cyclic types")
	, once (fc-resolve-type-rules0 .tr; throw "Unmatched types")
#

fc-resolve-type-rules0 ()
	:- !
#
fc-resolve-type-rules0 (SUB-SUPER-TYPES .te .t0 .tx, .tr1)
	:- !, fc-resolve-sub-super-types .te .t0 .tx, fc-resolve-type-rules0 .tr1
#
fc-resolve-type-rules0 _
	:- !, throw "Not enough type information"
#

fc-resolve-sub-super-types _ .t .t
#
fc-resolve-sub-super-types .te .t0 .tx
	:- bound .t0
	, fc-sub-super-type-pair .te .t0 .t1
	, fc-resolve-sub-super-types .te .t1 .tx
	; bound .tx
	, fc-sub-super-type-pair .te .t1 .tx
	, fc-resolve-sub-super-types .te .t0 .t1
#

fc-sub-super-type-pair .te .subType .superType
	:- once (bound .subType; bound .superType)
	, member .te .tc
	, generalize .tc .subType/.superType
#

fc-default-fun-type +call%i-t1 (FUN-OF (FUNCTOR-OF data (ATOM-OF %i)) (FUN-OF _ _)) #
fc-default-fun-type +call%i-t2 (FUN-OF (FUNCTOR-OF data (ATOM-OF %i)) (FUN-OF _ (FUN-OF _ _))) #
fc-default-fun-type +call%i-t3 (FUN-OF (FUNCTOR-OF data (ATOM-OF %i)) (FUN-OF _  (FUN-OF _ (FUN-OF _ _)))) #
fc-default-fun-type +call%i-v1 (FUN-OF (FUNCTOR-OF data (ATOM-OF %i)) (FUN-OF _ _)) #
fc-default-fun-type +call%i-v2 (FUN-OF (FUNCTOR-OF data (ATOM-OF %i)) (FUN-OF _ (FUN-OF _ _))) #
fc-default-fun-type +call%i-v3 (FUN-OF (FUNCTOR-OF data (ATOM-OF %i)) (FUN-OF _  (FUN-OF _ (FUN-OF _ _)))) #
fc-default-fun-type +compare (FUN-OF .t (FUN-OF .t NUMBER)) #
fc-default-fun-type +get%i (FUN-OF (ATOM-OF _) (FUNCTOR-OF data (ATOM-OF %i))) #
fc-default-fun-type +is-list (FUN-OF (LIST-OF _) BOOLEAN) #
fc-default-fun-type +is-pair (FUN-OF (PAIR-OF _ _) BOOLEAN) #
fc-default-fun-type +lhead (FUN-OF (LIST-OF .t) .t) #
fc-default-fun-type +ltail (FUN-OF (LIST-OF .t) (LIST-OF .t)) #
fc-default-fun-type +pleft (FUN-OF (PAIR-OF .t _) .t) #
fc-default-fun-type +pright (FUN-OF (PAIR-OF _ .t) .t) #
