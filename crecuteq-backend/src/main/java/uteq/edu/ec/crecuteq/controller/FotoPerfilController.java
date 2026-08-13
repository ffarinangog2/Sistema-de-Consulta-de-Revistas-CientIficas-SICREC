package uteq.edu.ec.crecuteq.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.crecuteq.entity.FotoPerfil;
import uteq.edu.ec.crecuteq.service.FotoPerfilService;

@RestController
@RequestMapping("/api/foto-perfil")
public class FotoPerfilController {
    private final FotoPerfilService service;

    public FotoPerfilController(FotoPerfilService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<byte[]> obtener(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        FotoPerfil foto = service.obtener(authorization);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.getTipoContenido()))
                .cacheControl(CacheControl.noStore())
                .body(foto.getContenido());
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> guardar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestPart("archivo") MultipartFile archivo) {
        service.guardar(authorization, archivo);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> eliminar(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        service.eliminar(authorization);
        return ResponseEntity.noContent().build();
    }
}
