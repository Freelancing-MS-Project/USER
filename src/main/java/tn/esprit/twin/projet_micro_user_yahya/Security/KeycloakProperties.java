package tn.esprit.twin.projet_micro_user_yahya.Security;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "keycloak.admin")
public class KeycloakProperties {

    private String clientId;
    private String clientSecret;
    private String realm;
    private String serverUrl;

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getRealm() { return realm; }
    public void setRealm(String realm) { this.realm = realm; }

    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }
}
