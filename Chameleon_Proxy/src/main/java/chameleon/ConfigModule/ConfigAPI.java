package chameleon.ConfigModule;

import chameleon.Models.StructureSchema;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@RequestMapping("/config")
public interface ConfigAPI {

    @PostMapping("/schema")
    ResponseEntity<?> configSchema(@RequestHeader("authorization") String auth, @RequestBody StructureSchema mappings);

}
