package chameleon.Models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import org.elasticsearch.common.Nullable;

import java.util.Map;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ProxyUser {

    public static final String METADATA_CONFIG = "userMetadata";

    @JsonView(Views.ElasticUserGET.class)
    private String username;

    @JsonView(Views.ElasticUserPOST.class)
    private String password;
    @JsonView({Views.ElasticUserPOST.class, Views.ElasticUserGET.class})
    private Set<String> roles;
    @JsonView({Views.ElasticUserPOST.class, Views.ElasticUserGET.class})
    private Map<String, Object> metadata;
    @Nullable
    @JsonView({Views.ElasticUserPOST.class, Views.ElasticUserGET.class})
    private String full_name;
    @Nullable
    @JsonView({Views.ElasticUserPOST.class, Views.ElasticUserGET.class})
    private String email;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFull_name() {
        return full_name;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }
}


