# cchs "ls -al" "sort" "tail -3"

CCACHE=~/.cmd-cache
DCACHE=~/.dl-cache
mkdir -p ${CCACHE}/ ${DCACHE}/

find ${CCACHE}/ -mtime 28 -print0 | xargs -0 echo rm -f 1>&2
find ${DCACHE}/ -maxdepth 1 -mtime 360 -type d -print0 | xargs -0 echo rm -rf 1>&2

cchs() {
	F=/dev/null
	while [ "${1}" ]; do
		CMD="${1}"
		shift
		if [ "${CMD:0:2}" == "{}" ]; then
			D=$(cat ${F})
			F=$(cchf "${D}${CMD:2}")
		elif [ "${CMD:0:3}" == "{V}" ]; then
			D=$(cat ${F})
			F=$(cchf "echo version ${D:0:8}; cd ${D:9}/; ${CMD:4}")
		elif [ "${CMD:0:6}" == "#chmod" ]; then
			FILE=$(cat ${F})
			chmod ${CMD:6} ${FILE}
			F=$(cchf "printf ${FILE}")
		elif [ "${CMD}" == "#curl" ]; then
			URL=$(cat ${F})
			MD5=$(printf "${URL}" | md5sum - | cut -d" " -f1)
			SHORT=$(printf "${URL}" | tr /:@ _ | tr -dc '[\-.0-9A-Z_a-z]')
			DF="${DCACHE}/${MD5:0:8}.${SHORT}"
			[ -f ${DF} ] || do-cmd curl -sL "${URL}" > ${DF}
			F=$(cchf "printf ${DF}")
		elif [ "${CMD}" == "#dir" ]; then
			DIR=$(cat ${F})
			LINK=$(sh -c "readlink -f ${DIR}/*")
			F=$(cchf "printf ${LINK}")
		elif [ "${CMD}" == "#git-clone" ]; then
			URL=$(cat ${F})
			MD5=$(printf "${URL}" | md5sum - | cut -d" " -f1)
			SHORT=$(printf "${URL}" | tr /: _ | tr -dc '[\-.0-9A-Z_a-z]')
			DF="${DCACHE}/${MD5:0:8}.${SHORT}"
			if [ -d ${DF} ]; then
				D0=$(date +%s)
				D1=$(stat -c %Y ${DF}.pulltime)
				if (( 3600 < ${D0} - ${D1} )); then
					do-cmd "cd ${DF}/ && git pull --quiet"
				fi
			else
				do-cmd "git clone --depth 1 ${URL} ${DF} --quiet"
			fi &&
			touch ${DF}.pulltime &&
			COMMIT=$(cd ${DF}/ && git rev-parse HEAD | cut -c1-8)
			F=$(cchf "printf ${COMMIT}:${DF}")
		elif [ "${CMD:0:5}" == "#tar-" ]; then
			OPT=${CMD:5}
			TARF=$(cat ${F})
			TARDIR=${TARF}.d
			[ -d ${TARDIR} ] || do-cmd "mkdir -p ${TARDIR} && tar ${OPT} ${TARF} -C ${TARDIR}"
			F=$(cchf "printf ${TARDIR}")
		elif [ "${CMD}" == "#unzip" ]; then
			ZIPF=$(cat ${F})
			ZIPDIR=${ZIPF}.d
			[ -d ${ZIPDIR} ] || do-cmd "mkdir -p ${ZIPDIR} && unzip -d ${ZIPDIR} -q ${ZIPF}"
			F=$(cchf "printf ${ZIPDIR}")
		else
			F=$(cchf "cat ${F} | ${CMD}")
		fi
	done
	cat ${F}
}

cchf() {
	CMD="${@}"
	MD5=$(printf "${CMD}" | md5sum - | cut -d" " -f1)
	P=${MD5:0:2}
	DIR=${CCACHE}/${P}
	FP=${DIR}/${MD5}
	KF=${FP}.k
	VF=${FP}.v

	mkdir -p ${DIR}

	if [ -f "${KF}" ] && diff <(printf "${CMD}") <(cat "${KF}"); then
		true
	else
		do-cmd "${CMD}" | tee "${VF}" 1>&2 && printf "${CMD}" > "${KF}"
	fi

	printf "${VF}"
}

do-cmd() {
	CMD="${@}"
	echo "START ${CMD}" >&2
	sh -c "${CMD}"
	echo "END~${?} ${CMD}" >&2
}
