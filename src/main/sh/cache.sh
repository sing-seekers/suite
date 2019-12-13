# cchs "ls -al" "sort" "tail -3"

CCACHE=~/.cmd-cache
DCACHE=~/.dl-cache
mkdir -p ${CCACHE}/ ${DCACHE}/

find ${CCACHE}/ -type f -mtime 28 -print0 | xargs -0 echo rm -f 1>&2
find ${DCACHE}/ -maxdepth 1 -type f -mtime 360 -type d -print0 | xargs -0 echo rm -rf 1>&2

cchs() {
	F=/dev/null
	while [ "${1}" ]; do
		CMD="${1}"
		shift
		if [ "${CMD:0:2}" == "{}" ]; then
			D=$(cat ${F})
			F=$(cchf "${D}${CMD:2}")
		elif [ "${CMD:0:3}" == "#cd" ]; then
			D=$(cat ${F})
			F=$(cchf "cd ${D}/; ${CMD:4}")
		elif [ "${CMD:0:6}" == "#chmod" ]; then
			FILE=$(cat ${F})
			chmod ${CMD:6} ${FILE}
			F=$(cchf "printf ${FILE}")
		elif [ "${CMD}" == "#curl" ]; then
			URL=$(cat ${F})
			DF=${DCACHE}/$(urlDir "${URL}")
			[ -f ${DF} ] || do-cmd curl -sL "${URL}" > ${DF}
			F=$(cchf "printf ${DF}")
		elif [ "${CMD}" == "#dir" ]; then
			DIR=$(cat ${F})
			LINK=$(sh -c "readlink -f ${DIR}/*")
			F=$(cchf "printf ${LINK}")
		elif [ "${CMD:0:6}" == "#do-cd" ]; then
			D=$(cat ${F})
			F=$(cchf "cd ${D}/; ${CMD:7} 1>&2; echo ${D}")
		elif [ "${CMD:0:10}" == "#do-git-cd" ]; then
			D=$(cat ${F})
			F=$(cchf "V=${D:0:8}; cd ${D:9}/; ${CMD:11} 1>&2; echo ${D}")
		elif [ "${CMD:0:7}" == "#git-cd" ]; then
			D=$(cat ${F})
			F=$(cchf "V=${D:0:8}; cd ${D:9}/; ${CMD:8}")
		elif [ "${CMD}" == "#git-clone" ]; then
			URL=$(cat ${F})
			DF=${DCACHE}/$(urlDir "${URL}")
			if ! [ -d ${DF} ]; then
				do-cmd "git clone --depth 1 --single-branch ${URL} ${DF} --quiet"
				touch ${DF}.pulltime
			fi
			D0=$(date +%s)
			D1=$(stat -c %Y ${DF}.pulltime)
			if (( 3600 < ${D0} - ${D1} )); then
				do-cmd "cd ${DF}/ && git pull --quiet"
				touch ${DF}.pulltime
			fi
			COMMIT=$(cd ${DF}/ && git rev-parse HEAD | cut -c1-8)
			F=$(cchf "printf ${COMMIT}:${DF}")
		elif [ "${CMD}" == "#git-clone-branch" ]; then
			URL=$(cat ${F} | cut -d' ' -f1)
			B=$(cat ${F} | cut -d' ' -f2)
			DF=${DCACHE}/$(urlDir "${URL}_${B}")
			if ! [ -d ${DF} ]; then
				do-cmd "git clone --depth 1 -b ${B} --single-branch ${URL} ${DF} --quiet"
				touch ${DF}.pulltime
			fi
			D0=$(date +%s)
			D1=$(stat -c %Y ${DF}.pulltime)
			if (( 3600 < ${D0} - ${D1} )); then
				do-cmd "cd ${DF}/ && git pull --quiet"
				touch ${DF}.pulltime
			fi
			COMMIT=$(cd ${DF}/ && git rev-parse HEAD | cut -c1-8)
			F=$(cchf "printf ${COMMIT}:${DF}")
		elif [ "${CMD:0:10}" == "#maven-get" ]; then
			#REPO=https://repo.maven.apache.org/maven2
			RGAV=$(cat ${F})
			REPO=$(echo ${RGAV} | cut -d# -f1)
			GROUPID=$(echo ${RGAV} | cut -d# -f2)
			ARTIFACTID=$(echo ${RGAV} | cut -d# -f3)
			VERSION=$(echo ${RGAV} | cut -d# -f4)
			P=$(echo ${GROUPID} | sed s#\\.#/#g)
			URL="${REPO}/${P}/${ARTIFACTID}/${VERSION}/${ARTIFACTID}-${VERSION}.pom"
			DF=${DCACHE}/$(urlDir "${URL}")
			[ -f ${DF} ] || do-cmd curl -sL "${URL}" > ${DF}
			F=$(cchf "printf ${DF}")
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

urlDir() {
	MD5=$(printf "${1}" | md5sum - | cut -d" " -f1)
	SHORT=$(printf "${1}" | cch-fn)
	echo ${MD5:0:8}.${SHORT}
}

do-cmd() {
	CMD="${@}"
	echo "START ${CMD}" >&2
	sh -c "${CMD}"
	echo "END~${?} ${CMD}" >&2
}

cch-fn() {
	tr /:@ _ | tr -dc '[\-.0-9A-Z_a-z]'
}
