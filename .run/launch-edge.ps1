$edge = 'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'
if (-not (Test-Path $edge)) { $edge = 'C:\Program Files\Microsoft\Edge\Application\msedge.exe' }
$profile = 'D:\DevelopmentLOOK\Idea\idea_project_workspace\huang-parent\.run\edge-browser-control'
if (Test-Path $profile) { Remove-Item -Recurse -Force $profile }
New-Item -ItemType Directory -Force -Path $profile | Out-Null
Start-Process -FilePath $edge -ArgumentList @('--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check','--remote-debugging-port=9222',"--user-data-dir=$profile",'about:blank') | Out-Null
