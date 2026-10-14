#!/bin/bash
echo "Building PixelXpert Minimal APK..."
chmod +x gradlew
./gradlew :app:assembleRelease

echo "Packaging KernelSU/Magisk Module..."

cd MagiskModBase
zip -r -9 -q ../PixelXpert-Minimal.zip *
cd ..

echo "Copying APK for manual installation..."
cp app/build/outputs/apk/release/PixelXpert.apk ./PixelXpert-Minimal.apk

echo "Done! Files generated: PixelXpert-Minimal.zip and PixelXpert-Minimal.apk"
