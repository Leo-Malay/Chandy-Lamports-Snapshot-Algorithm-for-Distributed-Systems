@echo off
setlocal enabledelayedexpansion

set "n=4"

for /L %%d in (0,1,%n%) do (
    start cmd /k "java Node %%d"
)

endlocal