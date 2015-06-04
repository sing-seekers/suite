fc-bind .v0 .v1 .then .else .parsed
	:- .then1 = PRAGMA (TYPE-VERIFY (TREE ' = ' .v0 .v1) BOOLEAN) .then
	, fc-bind0 .v0 .v1 .then1 .else .parsed
#

fc-bind0 .v0 (PRAGMA NEW (VAR .nv)) .then _ (DEF-VARS (.nv (PRAGMA TYPE-SKIP-CHECK .v0),) .then)
	:- !
#
fc-bind0 (PRAGMA _ .v0) .v1 .then .else .parsed
	:- !
	, fc-bind0 .v0 .v1 .then .else .parsed
#
fc-bind0 .v0 (PRAGMA _ .v1) .then .else .parsed
	:- !
	, fc-bind0 .v0 .v1 .then .else .parsed
#
fc-bind0 (CONS .type .h0 .t0) (CONS .type .h1 .t1) .then .else .parsed
	:- !, fc-bind-pair .h0 .t0 .h1 .t1 .then .else .parsed
#
fc-bind0 .v0 (CONS L .h1 .t1) .then .else (
	DEF-VARS (.elseVar (WRAP .else), .v0var .v0,) (
		IF (INVOKE (VAR .v0var) (VAR +is-list)) (
			DEF-VARS (
				.headVar (INVOKE (VAR .v0var) (VAR +lhead)),
				.tailVar (INVOKE (VAR .v0var) (VAR +ltail)),
			)
			.then1
		) .else1
	)
) :- !
	, temp .elseVar, temp .v0var, temp .headVar, temp .tailVar
	, .else1 = UNWRAP (VAR .elseVar)
	, fc-bind-pair (VAR .headVar) (VAR .tailVar) .h1 .t1 .then .else1 .then1
#
fc-bind0 .v0 (CONS P .p1 .q1) .then .else (
	DEF-VARS (.elseVar (WRAP .else), .v0var .v0,) (
		IF (INVOKE (VAR .v0var) (VAR +is-pair)) (
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
fc-bind0 .v0 .v1 .then .else (
	IF (PRAGMA TYPE-SKIP-CHECK (TREE ' = ' .v0 .v1)) .then .else
) #

fc-bind-pair .h0 .t0 .h1 .t1 .then .else (DEF-VARS (.elseVar (WRAP .else),) .parsed)
	:- temp .elseVar
	, .else1 = UNWRAP (VAR .elseVar)
	, fc-bind0 .h0 .h1 .then1 .else1 .parsed
	, fc-bind0 .t0 .t1 .then .else1 .then1
#
