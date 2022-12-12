call sass --update . --no-source-map
rmdir "META-INF" /s /q
mkdir "META-INF\resources"
Xcopy /E /I "primefaces-mirage-index-purple-dark\*.css" "META-INF\resources\primefaces-mirage-index-purple-dark"
Xcopy /E /I "primefaces-mirage-index-purple-light\*.css" "META-INF\resources\primefaces-mirage-index-purple-light"
jar cf index-themes.jar META-INF
rmdir "META-INF" /s /q