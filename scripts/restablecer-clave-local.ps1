param([string]$Email='adanyana@gmail.com')
$ErrorActionPreference='Stop'
$resultPath=Join-Path $PSScriptRoot '../.design-review/resultado-restablecimiento.json'
$changed=$false
try {
    Add-Type -AssemblyName System.Windows.Forms
    Add-Type -AssemblyName System.Drawing
    [System.Windows.Forms.Application]::EnableVisualStyles()
    $form=New-Object System.Windows.Forms.Form
    $form.Text='KIDO - Restablecer acceso'
    $form.Size=New-Object System.Drawing.Size(560,365)
    $form.StartPosition='CenterScreen'
    $form.FormBorderStyle='FixedDialog'
    $form.MaximizeBox=$false
    $form.MinimizeBox=$false
    $form.TopMost=$true
    $title=New-Object System.Windows.Forms.Label
    $title.Text="Nueva contrasena para $Email"
    $title.SetBounds(22,20,505,25)
    $form.Controls.Add($title)
    $hint=New-Object System.Windows.Forms.Label
    $hint.Text='De 8 a 72 caracteres; incluye mayuscula, minuscula y numero.'
    $hint.SetBounds(22,50,505,25)
    $form.Controls.Add($hint)
    $label1=New-Object System.Windows.Forms.Label
    $label1.Text='Nueva contrasena'
    $label1.SetBounds(22,82,505,22)
    $form.Controls.Add($label1)
    $one=New-Object System.Windows.Forms.TextBox
    $one.SetBounds(22,105,505,28)
    $one.UseSystemPasswordChar=$true
    $form.Controls.Add($one)
    $label2=New-Object System.Windows.Forms.Label
    $label2.Text='Confirmar contrasena'
    $label2.SetBounds(22,143,505,22)
    $form.Controls.Add($label2)
    $two=New-Object System.Windows.Forms.TextBox
    $two.SetBounds(22,166,505,28)
    $two.UseSystemPasswordChar=$true
    $form.Controls.Add($two)
    $show=New-Object System.Windows.Forms.CheckBox
    $show.Text='Mostrar ambas contrasenas para revisarlas'
    $show.SetBounds(22,205,505,24)
    $show.Add_CheckedChanged({$one.UseSystemPasswordChar=-not $show.Checked;$two.UseSystemPasswordChar=-not $show.Checked})
    $form.Controls.Add($show)
    $status=New-Object System.Windows.Forms.Label
    $status.SetBounds(22,239,505,32)
    $form.Controls.Add($status)
    $save=New-Object System.Windows.Forms.Button
    $save.Text='Guardar y comprobar acceso'
    $save.SetBounds(252,280,275,32)
    $form.Controls.Add($save)
    $save.Add_Click({
        if(-not [string]::Equals($one.Text,$two.Text,[StringComparison]::Ordinal)){$status.Text='Las entradas son distintas. Marca Mostrar para revisarlas.';return}
        if($one.Text.Length -lt 8 -or $one.Text.Length -gt 72 -or $one.Text -cnotmatch '[A-Z]' -or $one.Text -cnotmatch '[a-z]' -or $one.Text -notmatch '\d'){$status.Text='Falta longitud, mayuscula, minuscula o numero.';return}
        if($one.Text -cne $one.Text.Trim()){$status.Text='Evita espacios al inicio o al final.';return}
        $form.Tag=$one.Text
        $form.DialogResult=[System.Windows.Forms.DialogResult]::OK
        $form.Close()
    })
    $update={if($two.Text.Length){if([string]::Equals($one.Text,$two.Text,[StringComparison]::Ordinal)){$status.Text='Las contrasenas coinciden.'}else{$status.Text='Todavia no coinciden. Puedes marcar Mostrar para revisarlas.'}}else{$status.Text=''}}
    $one.Add_TextChanged($update)
    $two.Add_TextChanged($update)
    $form.AcceptButton=$save
    if($form.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK){return}
    $plain=[string]$form.Tag
    $one.Clear();$two.Clear();$form.Tag=$null;$form.Dispose()
    $container=docker compose ps -q kido-usuario-ms
    $info=(docker inspect $container | ConvertFrom-Json)[0]
    $values=@{}
    foreach ($entry in $info.Config.Env) { $pair=$entry -split '=',2; $values[$pair[0]]=$pair[1] }
    $base='http://localhost:19090'
    $service=Invoke-RestMethod -Method Post "$base/realms/kido/protocol/openid-connect/token" -TimeoutSec 15 -ContentType 'application/x-www-form-urlencoded' -Body @{client_id='kido-usuario-admin';client_secret=$values.KIDO_USUARIO_ADMIN_SECRET;grant_type='client_credentials'}
    $headers=@{Authorization="Bearer $($service.access_token)"}
    $encoded=[uri]::EscapeDataString($Email.Trim().ToLowerInvariant())
    $users=@(Invoke-RestMethod "$base/admin/realms/kido/users?email=$encoded&exact=true" -Headers $headers -TimeoutSec 15)
    if ($users.Count -ne 1 -or $users[0].email -ine $Email.Trim()) { throw 'No se encontro exactamente una cuenta con ese correo.' }
    $user=$users[0]
    $body=[Text.Encoding]::UTF8.GetBytes((@{type='password';value=$plain;temporary=$false} | ConvertTo-Json -Compress))
    Invoke-RestMethod -Method Put "$base/admin/realms/kido/users/$($user.id)/reset-password" -Headers $headers -ContentType 'application/json; charset=utf-8' -Body $body -TimeoutSec 15
    $changed=$true
    $login=Invoke-RestMethod -Method Post "$base/realms/kido/protocol/openid-connect/token" -TimeoutSec 15 -ContentType 'application/x-www-form-urlencoded; charset=utf-8' -Body @{client_id='kido-cli';grant_type='password';username=$user.username;password=$plain}
    if (-not $login.access_token) { throw 'La clave cambio, pero falta comprobar el acceso.' }
    $profile=Invoke-RestMethod 'http://localhost:4300/api/v1/usuarios/me' -Headers @{Authorization="Bearer $($login.access_token)"} -TimeoutSec 15
    @{ok=$true;changed=$true;profileId=$profile.id;timestamp=(Get-Date -Format o)} | ConvertTo-Json | Set-Content $resultPath -Encoding utf8
    [System.Windows.Forms.MessageBox]::Show('Contrasena actualizada. Ingreso y perfil comprobados correctamente. Vuelve a http://localhost:4300/login', 'KIDO - Acceso recuperado') | Out-Null
    Write-Host 'Abre http://localhost:4300/login e ingresa con tu correo y la nueva contrasena.'
} catch {
    $reason='No se pudo completar la operacion.'
    if ($_.Exception.Response) { $reason='El servidor respondio HTTP '+[int]$_.Exception.Response.StatusCode }
    else { $reason=$_.Exception.Message }
    @{ok=$false;changed=$changed;reason=$reason;timestamp=(Get-Date -Format o)} | ConvertTo-Json | Set-Content $resultPath -Encoding utf8
    [System.Windows.Forms.MessageBox]::Show($reason,'KIDO - Resultado') | Out-Null
} finally {
    $plain=$null; $confirmation=$null; $body=$null; $login=$null; $service=$null; $first=$null; $second=$null

}


