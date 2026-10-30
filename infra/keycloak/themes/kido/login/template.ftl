<#--
  Marco de todas las pantallas de acceso de KIDO.
  Mantiene la firma de la macro de base (registrationLayout) para que las plantillas
  heredadas (recuperar contraseña, actualizar contraseña, errores, OTP...) sigan funcionando.
  "others..." absorbe parámetros que futuras versiones de Keycloak puedan añadir.
-->
<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false showAnotherWayIfPresent=true others...>
<#assign kidoApp = (properties.kidoAppUrl)!"http://localhost:4300">
<!DOCTYPE html>
<html lang="${(locale.currentLanguageTag)!'es'}">
<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <meta name="color-scheme" content="light dark">
    <title>${msg("loginTitle",(realm.displayName!'KIDO'))}</title>
    <link rel="icon" type="image/svg+xml" href="${url.resourcesPath}/img/favicon.svg">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wght@12..96,600;12..96,700&family=Instrument+Sans:wght@400;500;600&display=swap">
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <#if style?has_content><link href="${url.resourcesPath}/${style}" rel="stylesheet"></#if>
        </#list>
    </#if>
    <#if scripts??>
        <#list scripts as script>
            <script src="${script}" type="text/javascript"></script>
        </#list>
    </#if>
</head>
<body class="kido ${bodyClass}">
<div class="kc-shell">
    <aside class="kc-brand" aria-hidden="true">
        <a class="kc-brand-logo" href="${kidoApp}" tabindex="-1">
            <svg viewBox="0 0 40 40" width="34" height="34"><rect width="40" height="40" rx="12" fill="#5b6dff"/><g fill="none" stroke="#fff" stroke-width="4.6" stroke-linecap="round" stroke-linejoin="round"><path d="M14 10.5v19"/><path d="M14 21.6 23.4 13.4"/><path d="M18.6 18.4 25.6 29.5"/></g><circle cx="28" cy="10.6" r="3.3" fill="#ffc463"/></svg>
            <span>kido</span>
        </a>
        <div class="kc-pitch">
            <h2>${msg("kidoBrandTitle")}</h2>
            <p>${msg("kidoBrandText")}</p>
            <ul>
                <li><span>1</span>${msg("kidoPunto1")}</li>
                <li><span>2</span>${msg("kidoPunto2")}</li>
                <li><span>3</span>${msg("kidoPunto3")}</li>
            </ul>
        </div>
        <div class="kc-glass">
            <strong>${msg("kidoSeguro")}</strong>
            <small>realm ${realm.name}</small>
        </div>
    </aside>

    <main class="kc-main">
        <div class="kc-top">
            <a class="kc-logo-mobile" href="${kidoApp}">
                <svg viewBox="0 0 40 40" width="28" height="28" aria-hidden="true"><rect width="40" height="40" rx="12" fill="#2f49e5"/><g fill="none" stroke="#fff" stroke-width="4.6" stroke-linecap="round" stroke-linejoin="round"><path d="M14 10.5v19"/><path d="M14 21.6 23.4 13.4"/><path d="M18.6 18.4 25.6 29.5"/></g><circle cx="28" cy="10.6" r="3.3" fill="#ffc463"/></svg>
                <span>kido</span>
            </a>
            <a class="kc-back" href="${(client.baseUrl)!kidoApp}">&larr; ${msg("kidoVolver")}</a>
            <#if realm.internationalizationEnabled && locale.supported?size gt 1>
                <nav class="kc-locales" aria-label="${msg("kidoIdiomas")}">
                    <#list locale.supported as l>
                        <a href="${l.url}" lang="${l.languageTag}"<#if l.languageTag == locale.currentLanguageTag> aria-current="true" class="on"</#if>>${l.languageTag?upper_case}</a>
                    </#list>
                </nav>
            </#if>
        </div>

        <div class="kc-card">
            <header class="kc-header">
                <#if !(auth?has_content && auth.showUsername() && !auth.showResetCredentials())>
                    <h1 id="kc-page-title"><#nested "header"></h1>
                    <#if displayRequiredFields>
                        <p class="kc-required"><span class="required">*</span> ${msg("requiredFields")}</p>
                    </#if>
                <#else>
                    <#nested "show-username">
                    <div id="kc-username" class="kc-attempted">
                        <#assign intento = auth.attemptedUsername!''>
                        <span class="kc-avatar" aria-hidden="true"><#if intento?has_content>${intento?substring(0,1)?upper_case}<#else>?</#if></span>
                        <span id="kc-attempted-username">${auth.attemptedUsername}</span>
                        <a id="reset-login" href="${url.loginRestartFlowUrl}" aria-label="${msg("restartLoginTooltip")}">${msg("kidoCambiar")}</a>
                    </div>
                    <h1 id="kc-page-title"><#nested "header"></h1>
                </#if>
            </header>

            <div id="kc-content">
                <div id="kc-content-wrapper">
                    <#-- Las acciones iniciadas por la app no muestran avisos de "completa esta acción". -->
                    <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                        <div class="kc-alert kc-alert-${message.type}" role="alert">
                            <span class="kc-alert-title">${kcSanitize(message.summary)?no_esc}</span>
                        </div>
                    </#if>

                    <#nested "form">

                    <#if auth?has_content && auth.showTryAnotherWayLink() && showAnotherWayIfPresent>
                        <form id="kc-select-try-another-way-form" action="${url.loginAction}" method="post">
                            <input type="hidden" name="tryAnotherWay" value="on"/>
                            <a href="#" id="try-another-way" class="kc-link"
                               onclick="document.forms['kc-select-try-another-way-form'].submit();return false;">${msg("doTryAnotherWay")}</a>
                        </form>
                    </#if>

                    <#nested "socialProviders">

                    <#if displayInfo>
                        <div id="kc-info" class="kc-info">
                            <div id="kc-info-wrapper"><#nested "info"></div>
                        </div>
                    </#if>
                </div>
            </div>
        </div>
        <p class="kc-foot">${msg("kidoPie")}</p>
    </main>
</div>
</body>
</html>
</#macro>
