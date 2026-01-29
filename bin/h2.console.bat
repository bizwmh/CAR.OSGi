@echo off
java -cp ..\bundles\04_DB\h2-2.2.224.jar org.h2.tools.Server ^
  -web ^
  -webPort 8082 ^
  -webAllowOthers ^
  -ifNotExists 