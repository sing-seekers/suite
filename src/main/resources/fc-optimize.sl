fc-optimize (DEF-VAR .var .value .do0) .dox
	:- !
	, complexity .do0 .complexity
	, .complexity < 4
	, fc-optimize-substitution .var .value .do0 .dox
#
fc-optimize (INVOKE .value (FUN .var .do0)) .dox
	:- !, fc-optimize-substitution .var .value .do0 .dox
#
fc-optimize .p0 .p1 :- fc-transform .p0 .p1 .p0p1s, fc-optimize-list .p0p1s #

fc-optimize-list () #
fc-optimize-list (.p0p1, .p0p1s) :- fc-optimize .p0p1, fc-optimize-list .p0p1s #

fc-optimize-substitution .var .value .do0 .do1
	:- replace (VAR .var) .value .do0 .do1
	, fc-optimize .do1 .dox
#
