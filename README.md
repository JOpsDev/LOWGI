# LibreOffice Writer Graphics importer

If you need to import a many pictures into a Writer document you might want to preserve the JPG format for the images.
But if you import each image and these are rotate they get converted to the PNG format which uses a lot more space and enlarges the document.

LOWGI uses the approach to rotate images losslessly upfront if needed and import the resulting image.

# Requirements

On Ubuntu:

apt install ure-java

for

/usr/lib/libreoffice/program/libjpipe.so

# Thanks

mediautil packages are a copy of https://github.com/drogatkin/mediautil

