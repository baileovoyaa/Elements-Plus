@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem
@rem SPDX-License-Identifier: Apache-2.0
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables, and ensure extensions are enabled
setlocal EnableExtensions

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

"%COMSPEC%" /c exit 1

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

"%COMSPEC%" /c exit 1

:execute
@rem Setup the command line

@rem Optional local override of the Gradle distribution URL (e.g. a mirror).
@rem Precedence: GRADLE_DISTRIBUTION_URL environment variable, then the
@rem "gradle.distributionUrl" entry in local.properties (not committed).
@rem The generated wrapper scripts have no built-in support for this, so a copy
@rem of the wrapper jar plus a matching properties file is used instead.
set "GRADLE_WRAPPER_JAR=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar"
if not defined GRADLE_DISTRIBUTION_URL if exist "%APP_HOME%\local.properties" (
    for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%APP_HOME%\local.properties") do (
        if /i "%%A"=="gradle.distributionUrl" set "GRADLE_DISTRIBUTION_URL=%%B"
    )
)
if defined GRADLE_DISTRIBUTION_URL (
    for /f "tokens=*" %%C in ("%GRADLE_DISTRIBUTION_URL%") do set "GRADLE_DISTRIBUTION_URL=%%C"
)
if defined GRADLE_DISTRIBUTION_URL (
    copy /y "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" "%APP_HOME%\gradle\wrapper\gradle-wrapper-local.jar" >NUL
    findstr /v /b /i /c:"distributionUrl=" "%APP_HOME%\gradle\wrapper\gradle-wrapper.properties" > "%APP_HOME%\gradle\wrapper\gradle-wrapper-local.properties"
    >> "%APP_HOME%\gradle\wrapper\gradle-wrapper-local.properties" echo distributionUrl=%GRADLE_DISTRIBUTION_URL%
    set "GRADLE_WRAPPER_JAR=%APP_HOME%\gradle\wrapper\gradle-wrapper-local.jar"
)



@rem Execute Gradle
@rem endlocal doesn't take effect until after the line is parsed and variables are expanded
@rem which allows us to clear the local environment before executing the java command
endlocal & "%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -jar "%GRADLE_WRAPPER_JAR%" %* & call :exitWithErrorLevel

:exitWithErrorLevel
@rem Use "%COMSPEC%" /c exit to allow operators to work properly in scripts
"%COMSPEC%" /c exit %ERRORLEVEL%
