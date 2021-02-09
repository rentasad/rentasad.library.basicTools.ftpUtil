cd lib\rentasad_library_basicTools	
call mvn clean install
cd ..\..
cd lib\gustini_library_configFileTool\lib\rentasad.library.logging
call mvn clean install
cd ..\..\..\..
	
cd lib\gustini_library_configFileTool
call mvn clean install
cd ..\..
cd lib\rentasad.library_db 
call mvn clean install -Dmaven.test.skip=true
cd ..\..
call mvn clean install

pause