@echo off
set DIR=%~dp0
set APP_BASE_NAME=%~n0
set APP_HOME=%DIR%
"%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" %*
