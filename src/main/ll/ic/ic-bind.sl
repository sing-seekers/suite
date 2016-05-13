ic-bind .v0 .v1 .then .else .parsed
	:- .then1 = PRAGMA (TYPE-VERIFY (TREE ' = ' .v0 .v1) BOOLEAN) .then
	, ic-bind0 .v0 .v1 .then1 .else .parsed
#

ic-bind0 .v0 (PRAGMA NEW (VAR .nv)) .then _ (DECLARE MONO .nv _ (SEQ (LET (VAR .nv) (PRAGMA TYPE-SKIP-CHECK .v0)) .then))
	:- !
#
ic-bind0 (PRAGMA _ .v0) .v1 .then .else .parsed
	:- !
	, ic-bind0 .v0 .v1 .then .else .parsed
#
ic-bind0 .v0 (PRAGMA _ .v1) .then .else .parsed
	:- !
	, ic-bind0 .v0 .v1 .then .else .parsed
#
ic-bind0 (NEW _ ()) (NEW _ ()) .then _ .then
#
ic-bind0 (NEW .structType (.name = .value0, .nvs0)) (NEW .structType (.name = .value1, .nvs1)) .then .else .parsed
	:- .structType = STRUCT-OF (.nameTypes | .name _)
	, !
	, .struct0 = NEW (STRUCT-OF .nameTypes) .nvs0
	, .struct1 = NEW (STRUCT-OF .nameTypes) .nvs1
	, ic-bind-pair .value0 .value1 .struct0 .struct1 .then .else .parsed
#
ic-bind0 .v0 .v1 .then .else (
	IF (PRAGMA TYPE-SKIP-CHECK (TREE ' = ' .v0 .v1)) .then .else
) #

ic-bind-pair .h0 .t0 .h1 .t1 .then .else .parsed
	:- temp .elseVar
	, .else1 = INVOKE (VAR .elseVar) ()
	, ic-bind0 .h0 .h1 .then1 .else1 .parsed0
	, ic-bind0 .t0 .t1 .then .else1 .then1
	, .parsed = DECLARE MONO .elseVar _ (SEQ (LET (VAR .elseVar) (METHOD THIS .else)) .parsed0)
#
