DESCRIPTION = "Open source SIP stack and media stack for presence, im/instant \
               messaging, and multimedia communication"
SECTION = "libs"
HOMEPAGE = "http://www.pjsip.org/"
# there are various 3rd party sources which may or may not be part of the
# build, there license term vary or are not explicitely specified.
LICENSE = "GPLv2+ & Proprietary"

DEPENDS = "alsa-lib libv4l openssl util-linux libopus virtual/libsdl2 libav"

PARALLEL_MAKE = ""

SRC_URI = "http://www.pjsip.org/release/${PV}/pjproject-${PV}.tar.bz2" 
SRC_URI += " file://aconfigure"
SRC_URI += " file://aconfigure.ac"
	

SRC_URI[md5sum] = "c347a672679e7875ce572e18517884b2"
SRC_URI[sha256sum] = "2f5a1da1c174d845871c758bd80fbb580fca7799d3cfaa0d3c4e082b5161c7b4"
LIC_FILES_CHKSUM = "file://README.txt;endline=16;md5=e7413c72dd9334edfa7dc723eed8644b"

S = "${WORKDIR}/pjproject-${PV}"

inherit autotools-brokensep pkgconfig pythonnative

#EXTRA_OECONF += "STAGING_DIR=${STAGING_DIR_NATIVE}"
# webrtc fails compiling, emmintrin.h missing (the file is x86 specific, sse2)
EXTRA_OECONF_arm += " --disable-libwebrtc"
EXTRA_OECONF_arm += " --with-sdl=${STAGING_DIR_HOST}${prefix}"
EXTRA_OECONF_arm += " --with-ffmpeg=${STAGING_DIR_HOST}${prefix}"

FILES_${PN} = "${bindir}/*"
FILES_${PN}-dev = "${includedir}/*"
FILES_${PN}-staticdev = "${libdir}/*"

HAS_DIRECTFB = "${@base_contains('DISTRO_FEATURES', 'directfb', 1, 0, d)}"


do_configure_prepend () {
    export LD="${CC}"

    bbnote "STAGING_BINDIR_CROSS is ${STAGING_BINDIR_CROSS}"
    cp ${WORKDIR}/aconfigure ${S}/aconfigure
    cp -f ${WORKDIR}/aconfigure.ac ${S}/aconfigure.ac


    echo "export CFLAGS += -fPIC" > user.mak
    echo "export LDFLAGS += -fuse-ld=bfd" >> user.mak
    if test ${HAS_DIRECTFB} -eq 1; then
	    echo export CFLAGS += -I${STAGING_DIR}/scheatquad/usr/include/directfb >> user.mak
	    echo export LDFLAGS += -L${STAGING_DIR}/scheatquad/usr/lib/directfb >> user.mak
    fi
}

do_compile_prepend() {
    oe_runmake dep
}

do_compile_append() {
    oe_runmake

    cd ${S}/pjsip-apps/build/
    make -f Samples.mak
}

do_install_prepend() {
    bbwarn "about to install pjproject in ${D}"
    bbnote "creating install directories"
    install -d ${D}/usr ${D}${bindir} ${D}${libdir} ${D}${includedir}
}

do_install_append() {   
    bbnote "installing pjproject test binaries"
    install -m 0755 ${S}/pjsip-apps/bin/pj* ${D}${bindir}
    install -m 0755 ${S}/pjsip-apps/bin/samples/arm-poky-linux-gnueabi/* ${D}/${bindir}

    bbnote "installing pjproject headers"
    cp -RLf ${S}/pjlib/include/* ${D}${includedir}

    cp -RLf ${S}/pjlib-util/include/* ${D}${includedir}
    cp -RLf ${S}/pjnath/include/* ${D}${includedir}
    cp -RLf ${S}/pjmedia/include/* ${D}${includedir}
    cp -RLf ${S}/pjsip/include/* ${D}${includedir}

    bbnote "installing pjproject libs"
    cp -Lf ${S}/pjsip/lib/*.a ${D}${libdir}
    cp -Lf ${S}/pjmedia/lib/*.a ${D}${libdir}
    cp -Lf ${S}/pjnath/lib/*.a ${D}${libdir}
    cp -Lf ${S}/pjlib-util/lib/*.a ${D}${libdir}
    cp -Lf ${S}/pjlib/lib/*.a ${D}${libdir}
    cp -Lf ${S}/third_party/lib/*.a ${D}${libdir}
 
    # remove the absolute path to the host's include dir
    sed -i 's:\-I/usr/include::' ${D}/usr/lib/pkgconfig/libpjproject.pc
    # remove the fdebug-prefix-map options
    sed -i 's:\-fdebug-prefix-map[a-zA-Z0-9\._\/=\-]*::g' ${D}/usr/lib/pkgconfig/libpjproject.pc
}
