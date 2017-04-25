#~/bin/sh
#stop on first error (jdoc ignored)

if [ -z $ANDROID_HOME ]; then 
	echo "ANDROID_HOME env variable not defined!"
	exit
fi

if [ -z $1 ]; then 
	echo "Missing version argument"
	exit
fi

VERSION=$1

echo Version:$VERSION

sed -e 's/{VERSION}/'$VERSION'/g' template.pom > nativeimage-$VERSION.pom


#build NativeImage
cd ../../NativeImage/android
build.sh
cd -
rm -R ../nativeimage/src/main/jniLibs
mkdir ../nativeimage/src/main/jniLibs
cp -a ../../NativeImage/android/libs/* ../nativeimage/src/main/jniLibs

#generate nativeimage-sources.jar
cd ../nativeimage/src/main/java
jar cf nativeimage-$VERSION-sources.jar .
cd -
mv ../nativeimage/src/main/java/nativeimage-$VERSION-sources.jar .

#generate nativeimage-javadoc.jar
javadoc -d jdoc ../nativeimage/src/main/java/com/scurab/android/nativeimage/*
cd jdoc
jar cvf ../nativeimage-$VERSION-javadoc.jar *
cd ..
rm -R jdoc

#generate nativeimage.aar
cd ..
chmod +x gradlew
gradlew assembleRelease
cd mavenrepo
mv ../nativeimage/build/outputs/aar/nativeimage-release.aar nativeimage-$VERSION.aar

FILES=(nativeimage-$VERSION.pom nativeimage-$VERSION.aar nativeimage-$VERSION-sources.jar nativeimage-$VERSION-javadoc.jar)
for file in "${FILES[@]}"
do
	if [ ! -f $file ]; then
		echo "$file not generated"
		exit 1
	fi
done

set -e
#generate final bundle
gpg -ab nativeimage-$VERSION.pom
gpg -ab nativeimage-$VERSION.aar
gpg -ab nativeimage-$VERSION-sources.jar
gpg -ab nativeimage-$VERSION-javadoc.jar
jar -cvf nativeimage-$VERSION-bundle.jar nativeimage-$VERSION.pom nativeimage-$VERSION.pom.asc nativeimage-$VERSION.aar nativeimage-$VERSION.aar.asc nativeimage-$VERSION-javadoc.jar nativeimage-$VERSION-javadoc.jar.asc nativeimage-$VERSION-sources.jar nativeimage-$VERSION-sources.jar.asc