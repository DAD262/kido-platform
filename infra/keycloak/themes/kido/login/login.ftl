<#--
  Pantalla de ingreso de KIDO. Solo pide credenciales: el registro (estudiante o docente)
  vive en la app Angular (/registro), porque el docente entrega datos adicionales para verificar.
-->
<#import "template.ftl" as layout>
<#assign kidoApp = (properties.kidoAppUrl)!"http://localhost:4300">
<#assign hayError = messagesPerField.existsError('username','password')>
<@layout.registrationLayout displayMessage=!hayError displayInfo=false; section>
    <#if section = "header">
        ${msg("loginAccountTitle")}
    <#elseif section = "form">
        <p class="kc-lead">${msg("kidoLoginLead")}</p>
        <#if realm.password>
            <form id="kc-form-login" class="kc-form" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post" novalidate>
                <#if !usernameHidden??>
                    <div class="kc-field<#if hayError> has-error</#if>">
                        <label for="username"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>
                        <input tabindex="1" id="username" name="username" value="${(login.username!'')}" type="text"
                               autofocus autocomplete="username" autocapitalize="none" spellcheck="false"
                               placeholder="${msg("kidoUsuarioPlaceholder")}"
                               aria-invalid="<#if hayError>true<#else>false</#if>"/>
                    </div>
                </#if>

                <div class="kc-field<#if hayError> has-error</#if>">
                    <div class="kc-label-row">
                        <label for="password">${msg("password")}</label>
                        <#if realm.resetPasswordAllowed>
                            <a tabindex="6" class="kc-link" href="${kidoApp}/recuperar">${msg("doForgotPassword")}</a>
                        </#if>
                    </div>
                    <div class="kc-password" dir="ltr">
                        <input tabindex="2" id="password" name="password" type="password" autocomplete="current-password" autocapitalize="none" spellcheck="false"
                               aria-describedby="kc-caps-warning kc-spaces-warning"
                               aria-invalid="<#if hayError>true<#else>false</#if>"/>
                        <button type="button" class="kc-eye" tabindex="3" data-password-toggle aria-controls="password"
                                aria-label="${msg("kidoMostrarAria")}" data-label-show="${msg("kidoMostrarAria")}" data-label-hide="${msg("kidoOcultarAria")}"
                                data-text-show="${msg("kidoMostrar")}" data-text-hide="${msg("kidoOcultar")}">${msg("kidoMostrar")}</button>
                    </div>
                    <p id="kc-caps-warning" class="kc-input-hint" role="status" hidden>${msg("kidoCapsWarning")}</p>
                    <p id="kc-spaces-warning" class="kc-input-hint" role="status" hidden>${msg("kidoSpacesWarning")}</p>
                    <#if hayError>
                        <span id="input-error" class="kc-error" aria-live="polite">${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}</span>
                    </#if>
                </div>

                <#if realm.rememberMe && !usernameHidden??>
                    <label class="kc-check">
                        <input tabindex="4" id="rememberMe" name="rememberMe" type="checkbox" <#if login.rememberMe??>checked</#if>>
                        <span>${msg("rememberMe")}</span>
                    </label>
                </#if>

                <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                <button tabindex="5" class="kc-submit" name="login" id="kc-login" type="submit">${msg("doLogIn")}</button>
            </form>
        </#if>

        <div class="kc-signup">
            <span class="kc-divider">${msg("kidoNuevo")}</span>
            <a class="kc-choice" href="${kidoApp}/registro?tipo=estudiante">
                <strong>${msg("kidoCrearEstudiante")}</strong>
                <small>${msg("kidoCrearEstudianteDetalle")}</small>
            </a>
            <a class="kc-choice kc-choice-teach" href="${kidoApp}/registro?tipo=docente">
                <strong>${msg("kidoPostularDocente")}</strong>
                <small>${msg("kidoPostularDocenteDetalle")}</small>
            </a>
        </div>
        <script type="module" src="${url.resourcesPath}/js/passwordVisibility.js"></script>
    <#elseif section = "socialProviders">
        <#if realm.password && social?? && social.providers?has_content>
            <div id="kc-social-providers" class="kc-social">
                <span class="kc-divider">${msg("identity-provider-login-label")}</span>
                <#list social.providers as p>
                    <a id="social-${p.alias}" class="kc-button kc-button-default" href="${p.loginUrl}">${p.displayName!}</a>
                </#list>
            </div>
        </#if>
    </#if>
</@layout.registrationLayout>
