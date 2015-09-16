() :- import.path "ic/ic-erase-type.sl"
	, import.path "ic/ic-infer-type.sl"
	, import.path "ic/ic-parse.sl"
	, import.path "ic/ic-rewrite.sl"
#

compile-imperative .do0 .e0/.ex
	:- ic-parse .do0 .do1
	, !, ic-infer-type .do1 I32
	, !, ic-erase-type .do1 .do2
	, !, ic-compile 0 .do2 .e0/.ex
	, !
#

-- Register usage
-- EAX - expression stack
-- EBX - expression stack
-- ECX - scratch; shift counter; divisor
-- EDX - scratch; dividend
-- ESI - expression stack
-- EDI - unused
-- EBP - stack frame
-- ESP - stack pointer
ic-compile .fs .do .e0/.ex
	:- ic-compile-better-option .fs .do .e0/.ex, !
#
ic-compile _ $$EBP (_ R+, _ MOV ($0, EBP), .e)/.e
#
ic-compile .fs (ALLOC .size .var .do) .e0/.ex
	:- let .fs1 (.fs + .size)
	, let .offset (0 - .fs1)
	, replace (VAR .var) (MEMORY .size (TREE ' + ' $$EBP (NUMBER .offset))) .do .do1
	, .e0 = (_ SUB (ESP, .size), .e1)
	, ic-compile .fs1 .do1 .e1/.e2
	, .e2 = (_ ADD (ESP, .size), .ex)
#
ic-compile _ (ASM .i) (.i, _ R+, .e)/.e
	:- ! -- Assembler might have variables, skip processing
#
ic-compile .fs (INVOKE .mr .params) .e0/.ex
	:- once (
		.mr = METHOD .this .sub
		; .this = MEMORY 4 (REF .mr), .sub = MEMORY 4 (TREE ' + ' (REF .mr) (NUMBER 4))
	)
	, .e0 = (_ RSAVE, .e1)
	, ic-push EBP .fs/.fs1 .e1/.e2
	, ic-push-pop-parameters .fs1/.fs2 .params .e2/.e3 .e6/.e7
	, ic-compile .fs2 .sub .e3/.e4
	, ic-compile .fs2 .this .e4/.e5
	, .e5 = (_ MOV (EBP, $0)
		, _ R-
		, _ CALL ($0)
		, _ R-
		, _ MOV (ECX, EAX)
		, .e6)
	, .e7 = (_ POP (EBP)
		, _ RRESTORE
		, _ R+
		, _ MOV ($0, ECX)
		, .ex)
#
ic-compile .fs (IF .if .then .else) .e0/.ex
	:- ic-compile .fs .if .e0/.e1
	, .e1 = (_ OR ($0, $0)
		, _ R-
		, _ JZ (DWORD .elseLabel)
		, .e2)
	, ic-compile .fs .then .e2/.e3
	, .e3 = (_ JMP (DWORD .endLabel)
		, .elseLabel R-
		, .e4)
	, ic-compile .fs .else .e4/.e5
	, .e5 = (.endLabel (), .ex)
#
ic-compile .fs (LET .var .value) .e0/.ex
	:- ic-let .fs .value .var .e0/.ex
#
ic-compile .fs (MEMORY 4 .value) .e0/.ex
	:- ic-compile .fs .value .e0/.e1
	, .e1 = (_ MOV ($0, `$0`), .ex)
#
ic-compile _ (METHOD0 .pss .do) .e0/.ex
	:- .e0 = (_ JMP (DWORD .label)
		, .funLabel RBEGIN
		, _ PUSH (EBP)
		, _ MOV (EBP, ESP)
		, .e1)
	, replace $$EBP (MEMORY 4 $$EBP) .do .do1
	, ic-replace-parameters .pss 4 .do1 .do2
	, ic-compile 0 .do2 .e1/.e2
	, .e2 = (_ MOV (ESP, EBP)
		, _ POP (EBP)
		, _ RET ()
		, _ R-
		, _ REND
		, .label R+
		, _ MOV ($0, .funLabel)
		, .ex)
#
ic-compile _ (NUMBER .i) (_ R+, _ MOV ($0, .i), .e)/.e
	:- is.int .i
#
ic-compile _ NOP .e0/.ex
	:- .e0 = (_ R+, .ex)
#
ic-compile _ NULL .e0/.ex
	:- .e0 = (_ R+, _ XOR ($0, $0), .ex)
#
ic-compile .fs (POST-ADD-NUMBER (MEMORY 4 .pointer) .i) .e0/.ex
	:- ic-compile .fs .pointer .e0/.e1
	, .e1 = (_ ADD (DWORD `$0`, .i)
		, _ MOV ($0, `$0`)
		, .ex)
#
ic-compile .fs (PRE-ADD-NUMBER (MEMORY 4 .pointer) .i) .e0/.ex
	:- ic-compile .fs .pointer .e0/.e1
	, .e1 = (_ R+
		, _ MOV ($0, $1)
		, _ MOV ($1, `$0`)
		, _ ADD (DWORD `$0`, .i)
		, _ R-
		, .ex)
#
ic-compile .fs (REF MEMORY _ .pointer) .e0/.ex
	:- ic-compile .fs .pointer .e0/.ex
#
ic-compile _ (REG .reg) (_ R+, _ MOV ($0, .reg), .e)/.e
#
ic-compile .fs (SEQ .do0 .do1) .e0/.ex
	:- ic-compile .fs .do0 .e0/.e1
	, .e1 = (_ R-, .e2)
	, ic-compile .fs .do1 .e2/.ex
#
ic-compile _ (SNIPPET .snippet) .e0/.ex
	:- .e0 = (_ JMP (DWORD .label)
		, .snippetLabel ()
		, .e1)
	, ic-compile 0 .snippet .e1/.e2
	, .e2 = (_ R-
		, .label R+
		, _ MOV ($0, .snippetLabel)
		, .ex)
#
ic-compile _ (STRING .s) .e0/.ex
	:- .e0 = (_ JMP (DWORD .label)
		, .strLabel DS (.s)
		, _ D8 (0)
		, .label R+
		, _ MOV ($0, .strLabel)
		, .ex)
#
ic-compile .fs (TREE .op .value0 .value1) .e0/.ex
	:- ic-operator .op .e2/.ex
	, once (
		ic-right-associative .op
		, ic-compile .fs .value1 .e0/.e1
		, ic-compile .fs .value0 .e1/.e2
	;
		, ic-compile .fs .value0 .e0/.e1
		, ic-compile .fs .value1 .e1/.e2
	)
#
ic-compile .fs (WHILE .while .do) .e0/.ex
	:- .e0 = (.nextLabel (), .e1)
	, ic-compile .fs .while .e1/.e2
	, .e2 = (_ OR ($0, $0)
		, _ R-
		, _ JZ (DWORD .endLabel)
		, .e3)
	, ic-compile .fs .do .e3/.e4
	, .e4 = (_ JMP (DWORD .nextLabel)
		, .endLabel ()
		, .ex)
#

-- Generates faster code
ic-compile-better-option .fs (TREE ' + ' .do0 (NUMBER .i)) .e0/.ex
	:- ic-compile .fs .do0 .e0/.e1
	, .e1 = (_ ADD ($0, .i), .ex)
#
ic-compile-better-option .fs (LET (MEMORY 4 (TREE ' + ' $$EBP (NUMBER .i))) .value) .e0/.ex
	:- ic-compile .fs .value .e0/.e1
	, .e1 = (_ MOV (`EBP + .i`, $0), .ex)
#
ic-compile-better-option .fs (LET (MEMORY 4 $$EBP) .value) .e0/.ex
	:- ic-compile .fs .value .e0/.e1
	, .e1 = (_ MOV (`EBP`, $0), .ex)
#
ic-compile-better-option .fs (LET (MEMORY 4 (TREE ' + ' .pointer (NUMBER .i))) .value) .e0/.ex
	:- ic-compile .fs .value .e0/.e1
	, ic-compile .fs .pointer .e1/.e2
	, .e2 = (_ MOV (`$0 + .i`, $1), _ R-, .ex)
#
ic-compile-better-option .fs (LET (MEMORY 4 .pointer) .value) .e0/.ex
	:- ic-compile .fs .value .e0/.e1
	, ic-compile .fs .pointer .e1/.e2
	, .e2 = (_ MOV (`$0`, $1), _ R-, .ex)
#
ic-compile-better-option _ (MEMORY 4 (TREE ' + ' $$EBP (NUMBER .i))) .e0/.ex
	:- .e0 = (_ R+, _ MOV ($0, `EBP + .i`), .ex)
#
ic-compile-better-option _ (MEMORY 4 $$EBP) .e0/.ex
	:- .e0 = (_ R+, _ MOV ($0, `EBP`), .ex)
#
ic-compile-better-option .fs (MEMORY 4 (TREE ' + ' .pointer (NUMBER .i))) .e0/.ex
	:- ic-compile .fs .pointer .e0/.e1
	, .e1 = (_ MOV ($0, `$0 + .i`), .ex)
#
ic-compile-better-option _ 0 (_ R+, _ XOR ($0, $0), .e)/.e
#

ic-let .fs .source (MEMORY 4 .pointer) .e0/.ex
	:- ic-compile .fs .source .e0/.e1
	, ic-compile .fs .pointer .e1/.e2
	, .e2 = (_ MOV (`$0`, $1)
		, _ R-
		, .ex)
#
ic-let .fs (METHOD .this .sub) (MEMORY 8 .pointer) .e0/.ex
	:- ic-compile .fs .pointer .e0/.e1
	, ic-compile .fs .this .e1/.e2
	, .e2 = (_ MOV (`$1`, $0)
		, _ R-
		, .e3)
	, ic-compile .fs .sub .e3/.e4
	, .e4 = (_ MOV (`$1 + 4`, $0)
		, _ R-
		, .ex)
#
ic-let .fs (MEMORY .size .pointer0) (MEMORY .size .pointer1) .e0/.ex
	:- ic-compile .fs .pointer0 .e0/.e1
	, ic-compile .fs .pointer1 .e1/.e2
	, ic-copy-memory 0 .size .e2/.e3
	, .e3 = (_ R-, .ex)
#

ic-push-pop-parameters .fs/.fs () .e/.e .f/.f
#
ic-push-pop-parameters .fs0/.fsx (.p, .ps) .e0/.ex .f0/.fx
	:- ic-push-pop-parameters .fs0/.fs1 .ps .e0/.e1 .f1/.fx
	, once (
		ic-compile .fs1 .p .e1/.e2
		, ic-push-top .fs1/.fsx .e2/.ex
		, .f0 = (_ POP (EDX), .f1)
		; .e1 = (_ SUB (ESP, .size), .e2)
		, .f0 = (_ ADD (ESP, .size), .f1)
		, let .fsx (.fs1 + .size)
		, ic-let .fsx .p (MEMORY .size (REG ESP)) .e2/.e3
		, .e3 = (_ R-, .ex)
	)
#

ic-replace-parameters () _ .do .do
#
ic-replace-parameters (PS .size .var, .vars) .s0 .do0 .dox
	:- let .s (.s0 + .size)
	, replace (VAR .var) (MEMORY .size (TREE ' + ' $$EBP (NUMBER .s))) .do0 .do1
	, ic-replace-parameters .vars .s .do1 .dox
#

ic-push-top .fs0/.fsx (_ PUSH ($0), _ R-, .e)/.e
	:- let .fsx (.fs0 + 4)
#

ic-push .op .fs0/.fsx (_ PUSH .op, .e)/.e
	:- let .fsx (.fs0 + 4)
#

ic-right-associative ' + ' #
ic-right-associative ' * ' #
ic-right-associative and #
ic-right-associative or #
ic-right-associative xor #

ic-operator .op
	(_ .insn ($1, $0)
	, _ R-
	, .e
)/.e
	:- ic-operator-insn .op .insn
#
ic-operator .op
	(_ CMP ($1, $0)
	, _ R-
	, _ .setcc (DL)
	, _ MOVSX ($0, DL)
	, .e
)/.e
	:- ic-operator-setcc .op .setcc
#
ic-operator ' / ' .e :- ic-divide EAX .e
#
ic-operator ' %% ' .e :- ic-divide EDX .e
#
ic-operator .shift
	(_ MOV (ECX, $0)
	, _ R-
	, _ .insn ($0, CL)
	, .e
)/.e
	:- ic-operator-shift .shift .insn
#

ic-divide .reg
	(_ MOV (ECX, $0)
	, _ R-
	, _ XOR (EDX, EDX)
	, _ PUSH (EAX)
	, _ MOV (EAX, $0)
	, _ R-
	, _ IDIV (ECX)
	, _ MOV (ECX, .reg)
	, _ POP (EAX)
	, _ R+
	, _ MOV ($0, ECX)
	, .e
)/.e #

ic-copy-memory _ 0 .e/.e
#
ic-copy-memory .o 1 (_ MOV (CL, `$1 + .o`), _ MOV (`$0 + .o`, CL), .e)/.e
#
ic-copy-memory .o 2 (_ MOV (CX, `$1 + .o`), _ MOV (`$0 + .o`, CX), .e)/.e
#
ic-copy-memory .o 4 (_ MOV (ECX, `$1 + .o`), _ MOV (`$0 + .o`, ECX), .e)/.e
#
ic-copy-memory 0 .size .e0/.ex
	:- .size > 16
	, let .div4 (.size / 4)
	, let .mod4 (.size % 4)
	, .e0 = (_ CLD ()
		, _ MOV (EDX, ESI)
		, _ MOV (EDI, $0)
		, _ MOV (ESI, $1)
		, _ MOV (ECX, .div4)
		, _ REP MOVSD ()
		, .e1)
	, once (.mod4 >= 1, .e1 = (_ MOVSB (), .e2); .e1 = .e2)
	, once (.mod4 >= 2, .e2 = (_ MOVSB (), .e3); .e2 = .e3)
	, once (.mod4 >= 3, .e3 = (_ MOVSB (), .e4); .e3 = .e4)
	, .e4 = (_ MOV (ESI, EDX), .ex)
#
ic-copy-memory .o .size .e0/.ex
  :- .size > 4
  , ic-copy-memory .o 4 .e0/.e1
  , let .size1 (.size - 4)
  , let .o1 (.o + 4)
  , ic-copy-memory .o1 .size1 .e1/.ex
#

ic-operator-insn ' + ' ADD #
ic-operator-insn ' - ' SUB #
ic-operator-insn ' * ' IMUL #
ic-operator-insn and AND #
ic-operator-insn or OR #
ic-operator-insn xor XOR #

ic-operator-setcc ' = ' SETE #
ic-operator-setcc ' != ' SETNE #
ic-operator-setcc ' <= ' SETLE #
ic-operator-setcc ' < ' SETL #
ic-operator-setcc ' >= ' SETGE #
ic-operator-setcc ' > ' SETG #

ic-operator-shift shl SAL #
ic-operator-shift shr SAR #
ic-operator-shift ushl SHL #
ic-operator-shift ushr SHR #

ic-error .m :- !, throw .m #
