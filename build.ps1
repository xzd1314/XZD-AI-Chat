# XZD AI Chat - WinUI3 一键构建脚本
# 在 Windows 上右键 -> 使用 PowerShell 运行，或：./build.ps1
$ErrorActionPreference = "Stop"

Write-Host "==> 检查 dotnet SDK ..." -ForegroundColor Cyan
$dotnet = Get-Command dotnet -ErrorAction SilentlyContinue
if (-not $dotnet) {
    Write-Host "未检测到 dotnet。请先安装 .NET 8 SDK：https://dotnet.microsoft.com/download/dotnet/8.0" -ForegroundColor Red
    exit 1
}
dotnet --version

Write-Host "==> 安装 Windows App SDK 工作负载（首次需要）..." -ForegroundColor Cyan
dotnet workload install windowsappsdkruntime --skip-manifest-update 2>$null

Write-Host "==> 还原 NuGet ..." -ForegroundColor Cyan
dotnet restore XzdAiChatWinUI.sln

Write-Host "==> 发布 Release (win-x64, 自包含) ..." -ForegroundColor Cyan
dotnet publish XzdAiChatWinUI/XzdAiChatWinUI.csproj `
    -c Release `
    -r win-x64 `
    --self-contained true `
    -p:WindowsPackageType=None `
    -p:EnableMsixTooling=false `
    -o publish\win-x64

Write-Host ""
Write-Host "==> 构建完成！" -ForegroundColor Green
Write-Host "输出目录: $(Resolve-Path publish\win-x64)"
Write-Host "运行: .\publish\win-x64\XzdAiChat.exe"
